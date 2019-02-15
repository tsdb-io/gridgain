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

package org.apache.ignite.failure;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.worker.GridWorker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.thread.IgniteThread;
import org.junit.Test;

/**
 * Tests the handling of long blocking operations in system-critical workers.
 */
public class SystemWorkersBlockingTest extends GridCommonAbstractTest {
    /** Handler latch. */
    private static volatile CountDownLatch hndLatch;

    /** */
    private static final long FAILURE_DETECTION_TIMEOUT = 5_000;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        AbstractFailureHandler failureHnd = new AbstractFailureHandler() {
            @Override protected boolean handle(Ignite ignite, FailureContext failureCtx) {
                if (failureCtx.type() == FailureType.SYSTEM_WORKER_BLOCKED)
                    hndLatch.countDown();

                return false;
            }
        };

        Set<FailureType> ignoredFailureTypes = new HashSet<>(failureHnd.getIgnoredFailureTypes());

        ignoredFailureTypes.remove(FailureType.SYSTEM_WORKER_BLOCKED);

        failureHnd.setIgnoredFailureTypes(ignoredFailureTypes);

        cfg.setFailureHandler(failureHnd);

        cfg.setFailureDetectionTimeout(FAILURE_DETECTION_TIMEOUT);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        hndLatch = new CountDownLatch(1);

        startGrid(0);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testBlockingWorker() throws Exception {
        IgniteEx ignite = grid(0);

        GridWorker worker = new GridWorker(ignite.name(), "test-worker", log) {
            @Override protected void body() throws InterruptedException {
                Thread.sleep(Long.MAX_VALUE);
            }
        };

        new IgniteThread(worker).start();

        while (worker.runner() == null)
            Thread.sleep(10);

        ignite.context().workersRegistry().register(worker);

        assertTrue(hndLatch.await(ignite.configuration().getFailureDetectionTimeout() * 2, TimeUnit.MILLISECONDS));

        Thread runner = worker.runner();

        runner.interrupt();
        runner.join(1000);

        assertFalse(runner.isAlive());
    }
}
