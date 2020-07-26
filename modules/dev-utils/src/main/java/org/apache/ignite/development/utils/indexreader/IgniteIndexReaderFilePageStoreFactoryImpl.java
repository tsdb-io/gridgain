/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.development.utils.indexreader;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.internal.processors.cache.persistence.file.AsyncFileIOFactory;
import org.apache.ignite.internal.processors.cache.persistence.file.FilePageStore;
import org.apache.ignite.internal.processors.cache.persistence.file.FileVersionCheckingFactory;
import org.apache.ignite.internal.processors.metric.impl.LongAdderMetric;
import org.jetbrains.annotations.Nullable;

/**
 * Factory {@link FilePageStore} for case of regular pds.
 */
public class IgniteIndexReaderFilePageStoreFactoryImpl implements IgniteIndexReaderFilePageStoreFactory {
    /** Directory with data(partitions and index). */
    private final File dir;

    /** {@link FilePageStore} factory by page store version. */
    private final FileVersionCheckingFactory storeFactory;

    /** Metrics updater. */
    private final LongAdderMetric allocationTracker = new LongAdderMetric("n", "d");

    /**
     * Constructor.
     *
     * @param dir Directory with data(partitions and index).
     * @param pageSize Page size.
     * @param filePageStoreVer Page store version.
     */
    public IgniteIndexReaderFilePageStoreFactoryImpl(File dir, int pageSize, int filePageStoreVer) {
        this.dir = dir;

        storeFactory = new FileVersionCheckingFactory(
            new AsyncFileIOFactory(),
            new AsyncFileIOFactory(),
            new DataStorageConfiguration().setPageSize(pageSize)
        ) {
            /** {@inheritDoc} */
            @Override public int latestVersion() {
                return filePageStoreVer;
            }
        };
    }

    /** {@inheritDoc} */
    @Override @Nullable public FilePageStore createFilePageStore(int partId, byte type, List<Throwable> errors) throws IgniteCheckedException {
        File file = getFile(dir, partId, null);

        return !file.exists() ? null : (FilePageStore)storeFactory.createPageStore(type, file, allocationTracker);
    }

    /** {@inheritDoc} */
    @Override public ByteBuffer headerBuffer(byte type) {
        int ver = storeFactory.latestVersion();

        FilePageStore store = storeFactory.createPageStore(type, null, ver, allocationTracker);

        return store.header(type, storeFactory.headerSize(ver));
    }
}
