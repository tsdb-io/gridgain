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

import org.apache.ignite.internal.processors.cache.query.QueryTable;

import java.util.Collection;

/**
 * Statistics persistence store interface.
 */
public interface IgniteStatisticsStore {

    /**
     * Clear statistics of any type for any objects;
     */
    void clearAllStatistics();

    /**
     * Replace all table statistics with specified ones.
     *
     * @param tbl table.
     * @param statistics collection of tables partition statistics
     */
    void saveLocalPartitionsStatistics(QueryTable tbl, Collection<ObjectPartitionStatistics> statistics);

    /**
     * Get local partition statistics by specified table.
     *
     * @param tbl table to get statistics by.
     * @return collection of partitions statistics.
     */
    Collection<ObjectPartitionStatistics> getLocalPartitionsStatistics(QueryTable tbl);

    /**
     * Clear partition statistics for specified table.
     *
     * @param tbl table to clear statistics by.
     */
    void clearLocalPartitionsStatistics(QueryTable tbl);

    /**
     * Get partition statistics.
     *
     * @param tbl table.
     * @param partId partition id.
     * @return object partition statistics or {@code null} if there are no statistics collected for such partition.
     */
    ObjectPartitionStatistics getLocalPartitionStatistics(QueryTable tbl, int partId);

    /**
     * Clear partition statistics.
     *
     * @param tbl table.
     * @param partId partiton id.
     */
    void clearLocalPartitionStatistics(QueryTable tbl, int partId);

    /**
     * Save partition statistics.
     *
     * @param tbl table which statistics belongs to.
     * @param statistics statistics to save.
     */
    void saveLocalPartitionStatistics(QueryTable tbl, ObjectPartitionStatistics statistics);
}

