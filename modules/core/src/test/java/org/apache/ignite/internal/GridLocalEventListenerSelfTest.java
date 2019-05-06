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

package org.apache.ignite.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * Test ensuring that event listeners are picked by started node.
 */
public class GridLocalEventListenerSelfTest extends GridCommonAbstractTest {
    /** Whether event fired. */
    private final CountDownLatch fired = new CountDownLatch(1);

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        int idx = getTestIgniteInstanceIndex(igniteInstanceName);

        if (idx == 0) {
            Map<IgnitePredicate<? extends Event>, int[]> lsnrs = new HashMap<>();

            lsnrs.put(new IgnitePredicate<Event>() {
                @Override public boolean apply(Event evt) {
                    fired.countDown();

                    return true;
                }
            }, new int[] { EventType.EVT_NODE_JOINED } );

            cfg.setLocalEventListeners(lsnrs);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids(true);
    }

    /**
     * Test listeners notification.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testListener() throws Exception {
        startGrids(2);

        assert fired.await(5000, TimeUnit.MILLISECONDS);
    }
}
