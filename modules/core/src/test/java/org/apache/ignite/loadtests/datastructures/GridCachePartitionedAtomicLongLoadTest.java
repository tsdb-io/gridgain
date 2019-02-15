/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

package org.apache.ignite.loadtests.datastructures;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.AtomicConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.transactions.Transaction;
import org.junit.Test;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC;
import static org.apache.ignite.transactions.TransactionIsolation.REPEATABLE_READ;

/**
 * Load test for atomic long.
 */
public class GridCachePartitionedAtomicLongLoadTest extends GridCommonAbstractTest {
    /** Test duration. */
    private static final long DURATION = 8 * 60 * 60 * 1000;

    /** */
    private static final AtomicInteger idx = new AtomicInteger();

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration c = super.getConfiguration(igniteInstanceName);

        AtomicConfiguration atomicCfg = new AtomicConfiguration();

        atomicCfg.setCacheMode(PARTITIONED);
        atomicCfg.setBackups(1);
        atomicCfg.setAtomicSequenceReserveSize(10);

        c.setAtomicConfiguration(atomicCfg);

        c.getTransactionConfiguration().setDefaultTxConcurrency(PESSIMISTIC);
        c.getTransactionConfiguration().setDefaultTxIsolation(REPEATABLE_READ);

        CacheConfiguration cc = defaultCacheConfiguration();

        cc.setCacheMode(CacheMode.PARTITIONED);
        cc.setRebalanceMode(CacheRebalanceMode.SYNC);
        cc.setWriteSynchronizationMode(FULL_SYNC);

        LruEvictionPolicy plc = new LruEvictionPolicy();
        plc.setMaxSize(1000);

        cc.setEvictionPolicy(plc);
        cc.setOnheapCacheEnabled(true);
        cc.setBackups(1);
        cc.setAffinity(new RendezvousAffinityFunction(true));

        c.setCacheConfiguration(cc);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testLoad() throws Exception {
        startGrid();

        try {
            multithreaded(new AtomicCallable(), 50);
        }
        finally {
            stopGrid();
        }
    }

    /**
     *
     */
    private class AtomicCallable implements Callable<Boolean> {
        /** {@inheritDoc} */
        @Override public Boolean call() throws Exception {
            Ignite ignite = grid();

            IgniteCache cache = ignite.cache(DEFAULT_CACHE_NAME);

            assert cache != null;

            IgniteAtomicSequence seq = ignite.atomicSequence("SEQUENCE", 0, true);

            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < DURATION && !Thread.currentThread().isInterrupted()) {
                Transaction tx = ignite.transactions().txStart();

                long seqVal = seq.incrementAndGet();

                int curIdx = idx.incrementAndGet();

                if (curIdx % 1000 == 0)
                    info("Sequence value [seq=" + seqVal + ", idx=" + curIdx + ']');

                tx.commit();
            }

            return true;
        }
    }
}
