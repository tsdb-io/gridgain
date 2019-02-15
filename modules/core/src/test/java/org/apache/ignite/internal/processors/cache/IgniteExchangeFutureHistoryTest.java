/*
 *                    GridGain Community Edition Licensing
 *                    Copyright 2019 GridGain Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 *  Restriction; you may not use this file except in compliance with the License. You may obtain a
 *  copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *
 *  Commons Clause Restriction
 *
 *  The Software is provided to you by the Licensor under the License, as defined below, subject to
 *  the following condition.
 *
 *  Without limiting other conditions in the License, the grant of rights under the License will not
 *  include, and the License does not grant to you, the right to Sell the Software.
 *  For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 *  under the License to provide to third parties, for a fee or other consideration (including without
 *  limitation fees for hosting or consulting/ support services related to the Software), a product or
 *  service whose value derives, entirely or substantially, from the functionality of the Software.
 *  Any license notice or attribution required by the License must also include this Commons Clause
 *  License Condition notice.
 *
 *  For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 *  the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 *  Edition software provided with this notice.
 */

package org.apache.ignite.internal.processors.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsExchangeFuture;
import org.junit.Test;

/**
 * Checks that top value at {@link GridCachePartitionExchangeManager#exchangeFutures()} is the newest one.
 */
public class IgniteExchangeFutureHistoryTest extends IgniteCacheAbstractTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected CacheMode cacheMode() {
        return CacheMode.PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected CacheAtomicityMode atomicityMode() {
        return CacheAtomicityMode.ATOMIC;
    }

    /** {@inheritDoc} */
    @Override protected NearCacheConfiguration nearConfiguration() {
        return null;
    }

    /**
     * Checks reverse order of exchangeFutures.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testExchangeFutures() throws Exception {
        GridCachePartitionExchangeManager mgr = ((IgniteKernal)grid(0)).internalCache(DEFAULT_CACHE_NAME).context().shared().exchange();

        for (int i = 1; i <= 10; i++) {
            startGrid(i);

            List<GridDhtPartitionsExchangeFuture> futs = mgr.exchangeFutures();

            List<GridDhtPartitionsExchangeFuture> sortedFuts = new ArrayList<>(futs);

            Collections.sort(sortedFuts, Collections.reverseOrder());

            for (int j = 0; j < futs.size(); j++)
                assertEquals(futs.get(j), sortedFuts.get(j));
        }
    }
}
