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

package org.apache.ignite.spi.discovery.zk;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.apache.ignite.internal.IgniteClientReconnectCacheTest;
import org.apache.ignite.internal.processors.cache.distributed.near.GridCachePartitionedMultiNodeFullApiSelfTest;
import org.apache.ignite.internal.processors.cache.distributed.near.GridCachePartitionedNodeRestartTest;
import org.apache.ignite.internal.processors.cache.distributed.replicated.GridCacheReplicatedMultiNodeFullApiSelfTest;
import org.apache.ignite.spi.discovery.tcp.ipfinder.zk.curator.TestingCluster;
import org.apache.ignite.util.GridCommandHandlerTest;

/**
 * Regular Ignite tests executed with {@link org.apache.ignite.spi.discovery.zk.ZookeeperDiscoverySpi}.
 */
public class ZookeeperDiscoverySpiTestSuite2 extends ZookeeperDiscoverySpiAbstractTestSuite {
    /** */
    private static TestingCluster testingCluster;

    /**
     * @return Test suite.
     * @throws Exception Thrown in case of the failure.
     */
    public static TestSuite suite() throws Exception {
        System.setProperty("H2_JDBC_CONNECTIONS", "500"); // For multi-jvm tests.

        initSuite();

        TestSuite suite = new TestSuite("ZookeeperDiscoverySpi Test Suite");

        suite.addTest(new JUnit4TestAdapter(GridCachePartitionedNodeRestartTest.class));
        suite.addTest(new JUnit4TestAdapter(IgniteCacheEntryListenerWithZkDiscoAtomicTest.class));
        suite.addTest(new JUnit4TestAdapter(IgniteClientReconnectCacheTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCachePartitionedMultiNodeFullApiSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCacheReplicatedMultiNodeFullApiSelfTest.class));
        suite.addTest(new JUnit4TestAdapter(GridCommandHandlerTest.class));

        return suite;
    }
}
