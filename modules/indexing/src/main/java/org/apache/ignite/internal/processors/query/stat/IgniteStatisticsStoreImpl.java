/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ignite.internal.processors.query.stat;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.cache.persistence.metastorage.MetastorageLifecycleListener;
import org.apache.ignite.internal.processors.cache.persistence.metastorage.ReadOnlyMetastorage;
import org.apache.ignite.internal.processors.cache.persistence.metastorage.ReadWriteMetastorage;
import org.apache.ignite.internal.processors.cache.query.QueryTable;
import org.apache.ignite.internal.processors.query.stat.messages.StatsObjectData;
import org.apache.ignite.internal.processors.query.stat.messages.StatsPropagationMessage;
import org.apache.ignite.resources.LoggerResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Sql statistics storage in metastore.
 * Will store all statistics related objects with prefix "stats."
 * Store only partition level statistics.
 */
public class IgniteStatisticsStoreImpl implements IgniteStatisticsStore, MetastorageLifecycleListener {

    // In local meta store it store partitions statistics by path: stats.<SCHEMA>.<OBJECT>.<partId>
    private final static String META_SEPARATOR = ".";
    private final static String META_STAT_PREFIX = "stats";

    /** Logger. */
    @LoggerResource
    private IgniteLogger log;

    private final GridKernalContext ctx;
    private IgniteStatisticsRepository repository;
    private volatile ReadWriteMetastorage metastore;

    // TODO remove with new serialization
    private AtomicBoolean inited = new AtomicBoolean(false);
    private AtomicBoolean initing = new AtomicBoolean(false);

    /**
     * Constructor.
     *
     * @param ctx grid kernal context.
     * @param repository repository to fulfill on metastore available.
     */
    public IgniteStatisticsStoreImpl(GridKernalContext ctx, IgniteStatisticsRepository repository) {
        this.ctx = ctx;
        this.repository = repository;
        ctx.internalSubscriptionProcessor().registerMetastorageListener(this);
    }

    private void writePartStatistics(String schemaName, String objName, ObjectPartitionStatistics partStats) {
        if (metastore == null) {
            log.warning(String.format("Metastore not ready to save partition statistic: %s.%s part %d", schemaName,
                    objName, partStats.partId()));
            return;
        }

        String key = getPartKeyPrefix(schemaName, objName);
        try {
            StatsObjectData statsData = StatisticsUtils.toMessage(schemaName, objName, StatsType.PARTITION, partStats);
            metastore.write(key + partStats.partId(), statsData);
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error while writing statistics %s.%s:%d to local storage: %s", schemaName, objName,
                    partStats.partId(), e.getMessage()), e);
        }
    }

    /**
     * Get object name from meta key.
     *
     * @param metaKey meta key to get object name from.
     * @return object name.
     */
    private String getObjName(String metaKey) {
        int idx = metaKey.lastIndexOf(META_SEPARATOR) + 1;

        assert idx < metaKey.length();

        return metaKey.substring(idx);
    }

    /**
     * Get schema name from meta key.
     *
     * @param metaKey meta key to get schema name from.
     * @return schema name.
     */
    private String getSchemaName(String metaKey) {
        int schemaIdx = metaKey.indexOf(META_SEPARATOR) + 1;
        int tableIdx = metaKey.indexOf(META_SEPARATOR, schemaIdx + 1);

        return metaKey.substring(schemaIdx, tableIdx);
    }

    private int getPartitionId(String metaKey) {
        int partIdx = metaKey.lastIndexOf(META_SEPARATOR);
        String partIdStr = metaKey.substring(partIdx);
        return Integer.valueOf(partIdStr);
    }

    private QueryTable getQueryTable(String metaKey) {
        int schemaIdx = metaKey.indexOf(META_SEPARATOR) + 1;
        int tableIdx = metaKey.indexOf(META_SEPARATOR, schemaIdx + 1);

        return new QueryTable(metaKey.substring(schemaIdx, tableIdx), metaKey.substring(tableIdx + 1));
    }


    private String getPartKeyPrefix(String schema, String tblName) {
        return META_STAT_PREFIX + META_SEPARATOR + schema + META_SEPARATOR + tblName + META_SEPARATOR;
    }


    @Override
    public void onReadyForRead(ReadOnlyMetastorage metastorage) throws IgniteCheckedException {

    }

    @Override
    public void onReadyForReadWrite(ReadWriteMetastorage metastorage) throws IgniteCheckedException {
        this.metastore = metastorage;
        Map<QueryTable, List<ObjectPartitionStatistics>> statsMap = new HashMap<>();
        metastorage.iterate(META_STAT_PREFIX, (key, statMsg) -> {
            QueryTable tbl = getQueryTable(key);
            try {
                ObjectPartitionStatistics statistics = StatisticsUtils.toObjectPartitionStatistics((StatsObjectData)statMsg);
                statsMap.compute(tbl, (k,v) -> {
                    if (v == null)
                        v =  new ArrayList<>();
                    v.add(statistics);

                    return v;
                });

            } catch (IgniteCheckedException e) {
                log.warning("Unable to read statistics by key " + key);
            }
            if (log.isDebugEnabled()) {
                log.debug("Local statistics for table " + tbl + " loaded");
            }
        },true);
        for(Map.Entry<QueryTable, List<ObjectPartitionStatistics>> entry : statsMap.entrySet())
            repository.cacheLocalStatistics(entry.getKey(), entry.getValue());
    }

    private void writeMeta(String key, Serializable object) throws IgniteCheckedException {
        assert object != null;

        if (metastore == null) {
            log.warning("Unable to save metadata to " + key);
            return;
        }

        ctx.cache().context().database().checkpointReadLock();
        try {
            metastore.write(key, object);
        } finally {
            ctx.cache().context().database().checkpointReadUnlock();
        }
    }

    private Serializable readMeta(String key) throws IgniteCheckedException {
        ctx.cache().context().database().checkpointReadLock();
        try {
            return metastore.read(key);
        } finally {
            ctx.cache().context().database().checkpointReadUnlock();
        }
    }

    private void removeMeta(String key) throws IgniteCheckedException {
        ctx.cache().context().database().checkpointReadLock();
        try {
            metastore.remove(key);
        } finally {
            ctx.cache().context().database().checkpointReadUnlock();
        }
    }

    private void removeMeta(Collection<String> keys) throws IgniteCheckedException {
        ctx.cache().context().database().checkpointReadLock();
        try {
            for (String key : keys)
                metastore.remove(key);
        } finally {
            ctx.cache().context().database().checkpointReadUnlock();
        }
    }

    private void iterateMeta(String keyPrefix, BiConsumer<String, ? super Serializable> cb, boolean unmarshall)
            throws IgniteCheckedException {
        assert metastore != null;

        ctx.cache().context().database().checkpointReadLock();
        try {
            metastore.iterate(keyPrefix, cb, unmarshall);
        } finally {
            ctx.cache().context().database().checkpointReadUnlock();
        }
    }

    @Override
    public void clearAllStatistics() {
        if (metastore == null)
            return;

        try {
            iterateMeta(META_STAT_PREFIX, (k,v) -> {
                try {
                    metastore.remove(k);
                } catch (IgniteCheckedException e) {
                    log.warning("Error during clearing statistics by key " + k, e);
                }
            }, false);
        } catch (IgniteCheckedException e) {
            log.warning("Error during clearing statistics", e);
        }
    }

    @Override
    public void saveLocalPartitionsStatistics(QueryTable tbl, Collection<ObjectPartitionStatistics> statistics) {
        if (metastore == null)
            // TODO: log warning
            return;
        Map<Integer, ObjectPartitionStatistics> partitionStatistics = statistics.stream().collect(
                Collectors.toMap(ObjectPartitionStatistics::partId, s -> s));

        try {
            iterateMeta(getPartKeyPrefix(tbl.schema(), tbl.table()), (k,v) -> {
                ObjectPartitionStatistics newStats = partitionStatistics.get(getPartitionId(k));
                try {
                    if (newStats == null)
                        metastore.remove(k);
                    else
                        metastore.write(k, StatisticsUtils.toMessage(tbl.schema(), tbl.table(), StatsType.PARTITION,
                                newStats));

                } catch (IgniteCheckedException e) {
                    log.warning(String.format("Error during saving statistics %s.%s to %s",
                            tbl.schema(), tbl.table(), k), e);
                }
            }, false);
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error during saving statistics %s.%s", tbl.schema(), tbl.table()), e);
        }
    }

    @Override
    public Collection<ObjectPartitionStatistics> getLocalPartitionsStatistics(QueryTable tbl) {
        if (metastore == null)
            return null;
        List<ObjectPartitionStatistics> result = new ArrayList<>();
        try {
            iterateMeta(getPartKeyPrefix(tbl.schema(), tbl.table()), (k,v) -> {
                try {
                    ObjectPartitionStatistics partStats = StatisticsUtils.toObjectPartitionStatistics((StatsObjectData)v);
                    result.add(partStats);
                } catch (IgniteCheckedException e) {
                    log.warning(String.format("Error during reading statistics %s.%s by key %s",
                            tbl.schema(), tbl.table(), k));
                }
            }, true);
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error during reading statistics %s.%s", tbl.schema(), tbl.table()), e);
        }
        return result;
    }

    @Override
    public void clearLocalPartitionsStatistics(QueryTable tbl) {
        if (metastore == null)
            return;

        try {
            iterateMeta(getPartKeyPrefix(tbl.schema(), tbl.table()), (k,v) -> {
                try {
                    metastore.remove(k);
                } catch (IgniteCheckedException e) {
                    log.warning(String.format("Error during clearing statistics %s.%s", tbl.schema(), tbl.table()), e);
                }
            }, false);
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error during clearing statistics %s.%s", tbl.schema(), tbl.table()), e);
        }
    }

    @Override
    public void saveLocalPartitionStatistics(QueryTable tbl, ObjectPartitionStatistics statistics) {
        if (metastore == null) {
            log.warning(String.format("Unable to store local partition statistics %s.%s:%d", tbl.schema(), tbl.table(),
                    statistics.partId()));
            return;
        }
        String partPrefix = getPartKeyPrefix(tbl.schema(), tbl.table()) + statistics.partId();

        try {
            StatsObjectData statsMessage = StatisticsUtils.toMessage(tbl.schema(), tbl.table(), StatsType.PARTITION,
                    statistics);
            writeMeta(partPrefix, statsMessage);
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error while storing local partition statistics %s.%s:%d", tbl.schema(), tbl.table(),
                    statistics.partId()), e);
        }

    }

    @Override
    public ObjectPartitionStatistics getLocalPartitionStatistics(QueryTable tbl, int partId) {
        if (metastore == null)
            return null;
        String metaKey = getPartKeyPrefix(tbl.schema(), tbl.table()) + partId;
        try {
            return StatisticsUtils.toObjectPartitionStatistics((StatsObjectData) readMeta(metaKey));
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error while reading local partition statistics %s.%s:%d",
                    tbl.schema(), tbl.table(), partId), e);
        }
        return null;
    }

    @Override
    public void clearLocalPartitionStatistics(QueryTable tbl, int partId) {
        if (metastore == null)
            return;
        String metaKey = getPartKeyPrefix(tbl.schema(), tbl.table()) + partId;
        try {
            removeMeta(metaKey);
        } catch (IgniteCheckedException e) {
            log.warning(String.format("Error while clearing local partition statistics %s.%s:%d",
                    tbl.schema(), tbl.table(), partId), e);
        }
    }
}
