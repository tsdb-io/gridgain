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

package org.apache.ignite.internal.processors.cache.datastructures;

import java.util.Map;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.AtomicConfiguration;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.internal.processors.cache.ClusterReadOnlyModeTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.internal.processors.cache.datastructures.IgniteDataStructuresTestUtils.getAtomicConfigurations;
import static org.apache.ignite.internal.processors.cache.datastructures.IgniteDataStructuresTestUtils.getCollectionConfigurations;
import static org.apache.ignite.testframework.GridTestUtils.assertThrows;

/**
 * Tests that Ignite data structures can't be created if cluster in a {@link ClusterState#ACTIVE_READ_ONLY} mode.
 */
public class IgniteDataStructuresCreateDeniedInClusterReadOnlyMode extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        stopAllGrids();

        startGrids(2);

        grid(0).cluster().state(ClusterState.ACTIVE_READ_ONLY);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /** */
    @Test
    public void testAtomicLong() {
        for (Map.Entry<String, AtomicConfiguration> t : getAtomicConfigurations().entrySet()) {
            Throwable ex = assertThrows(
                log,
                () -> grid(0).atomicLong(t.getKey(), t.getValue(), 0, true),
                Exception.class,
                null
            );

            ClusterReadOnlyModeTestUtils.checkRootCause(ex, t.getKey());
        }
    }

    /** */
    @Test
    public void testAtomicReference() {
        for (Map.Entry<String, AtomicConfiguration> t : getAtomicConfigurations().entrySet()) {
            Throwable ex = assertThrows(
                log,
                () -> grid(0).atomicReference(t.getKey(), t.getValue(), null, true),
                Exception.class,
                null
            );

            ClusterReadOnlyModeTestUtils.checkRootCause(ex, t.getKey());
        }
    }

    /** */
    @Test
    public void testIgniteSet() {
        for (Map.Entry<String, CollectionConfiguration> t : getCollectionConfigurations().entrySet()) {
            Throwable ex = assertThrows(
                log,
                () -> grid(0).set(t.getKey(), t.getValue()),
                Exception.class,
                null
            );

            ClusterReadOnlyModeTestUtils.checkRootCause(ex, t.getKey());
        }
    }

    /** */
    @Test
    public void testIgniteQueue() {
        for (Map.Entry<String, CollectionConfiguration> t : getCollectionConfigurations().entrySet()) {
            Throwable ex = assertThrows(
                log,
                () -> grid(0).queue(t.getKey(), 0, t.getValue()),
                Exception.class,
                null
            );

            ClusterReadOnlyModeTestUtils.checkRootCause(ex, t.getKey());
        }
    }

    /** */
    @Test
    public void testIgniteAtomicSequence() {
        for (Map.Entry<String, AtomicConfiguration> t : getAtomicConfigurations().entrySet()) {
            Throwable ex = assertThrows(
                log,
                () -> grid(0).atomicSequence(t.getKey(), t.getValue(), 0, true),
                Exception.class,
                null
            );

            ClusterReadOnlyModeTestUtils.checkRootCause(ex, t.getKey());
        }
    }

    /** */
    @Test
    public void testIgniteAtomicStamped() {
        for (Map.Entry<String, AtomicConfiguration> t : getAtomicConfigurations().entrySet()) {
            Throwable ex = assertThrows(
                log,
                () -> grid(0).atomicStamped(t.getKey(), t.getValue(), 0, 0, true),
                Exception.class,
                null
            );

            ClusterReadOnlyModeTestUtils.checkRootCause(ex, t.getKey());
        }
    }

    /** */
    @Test
    public void testIgniteCountDownLatch() {
        Throwable ex = assertThrows(
            log,
            () -> grid(0).countDownLatch("test-latch", 10, false, true),
            Exception.class,
            null
        );

        ClusterReadOnlyModeTestUtils.checkRootCause(ex, "test-latch");
    }
}
