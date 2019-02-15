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

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 *
 */
public class IgniteDynamicCacheStartStopConcurrentTest extends GridCommonAbstractTest {
    /** */
    private static final int NODES = 4;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGridsMultiThreaded(NODES);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testConcurrentStartStop() throws Exception {
        awaitPartitionMapExchange();

        int minorVer = ignite(0).configuration().isLateAffinityAssignment() ? 1 : 0;

        checkTopologyVersion(new AffinityTopologyVersion(NODES, minorVer));

        for (int i = 0; i < 5; i++) {
            log.info("Iteration: " + i);

            GridTestUtils.runMultiThreaded(new IgniteInClosure<Integer>() {
                @Override public void apply(Integer idx) {
                    Ignite ignite = ignite(idx);

                    ignite.getOrCreateCache(new CacheConfiguration<>(DEFAULT_CACHE_NAME));
                }
            }, NODES, "cache-thread");

            minorVer++;

            checkTopologyVersion(new AffinityTopologyVersion(NODES, minorVer));

            ignite(0).compute().affinityRun(DEFAULT_CACHE_NAME, 1, new IgniteRunnable() {
                @Override public void run() {
                    // No-op.
                }
            });

            GridTestUtils.runMultiThreaded(new IgniteInClosure<Integer>() {
                @Override public void apply(Integer idx) {
                    Ignite ignite = ignite(idx);

                    ignite.destroyCache(DEFAULT_CACHE_NAME);
                }
            }, NODES, "cache-thread");

            minorVer++;

            checkTopologyVersion(new AffinityTopologyVersion(NODES, minorVer));
        }
    }

    /**
     * @param topVer Expected version.
     */
    private void checkTopologyVersion(AffinityTopologyVersion topVer) {
        for (int i = 0; i < NODES; i++) {
            IgniteKernal ignite = (IgniteKernal)ignite(i);

            assertEquals(ignite.name(), topVer, ignite.context().discovery().topologyVersionEx());
        }
    }
}
