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
package org.apache.ignite.internal.processors.cache.eviction.paged;

import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class PageEvictionDataStreamerTest extends PageEvictionMultinodeAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        return setEvictionMode(DataPageEvictionMode.RANDOM_LRU, super.getConfiguration(gridName));
    }

    /** {@inheritDoc} */
    @Override protected void createCacheAndTestEviction(CacheConfiguration<Object, Object> cfg) throws Exception {
        IgniteCache<Object, Object> cache = clientGrid().getOrCreateCache(cfg);

        try (IgniteDataStreamer<Object, Object> ldr = clientGrid().dataStreamer(cfg.getName())) {
            ldr.allowOverwrite(true);

            for (int i = 1; i <= ENTRIES; i++) {
                ThreadLocalRandom r = ThreadLocalRandom.current();

                if (r.nextInt() % 5 == 0)
                    ldr.addData(i, new TestObject(PAGE_SIZE / 4 - 50 + r.nextInt(5000))); // Fragmented object.
                else
                    ldr.addData(i, new TestObject(r.nextInt(PAGE_SIZE / 4 - 50))); // Fits in one page.

                if (i % (ENTRIES / 10) == 0)
                    System.out.println(">>> Entries put: " + i);
            }
        }

        int resultingSize = cache.size(CachePeekMode.PRIMARY);

        System.out.println(">>> Resulting size: " + resultingSize);

        // Eviction started, no OutOfMemory occurred, success.
        assertTrue(resultingSize < ENTRIES);

        clientGrid().destroyCache(cfg.getName());
    }

    /** {@inheritDoc} */
    @Test
    @Ignore("https://ggsystems.atlassian.net/browse/GG-36146")
    @Override public void testPageEviction() throws Exception {
        super.testPageEviction();
    }
}
