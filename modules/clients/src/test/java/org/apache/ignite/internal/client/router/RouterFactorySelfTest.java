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

package org.apache.ignite.internal.client.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_JETTY_PORT;

/**
 * Test routers factory.
 */
public class RouterFactorySelfTest extends GridCommonAbstractTest {
    /** */
    private static final int GRID_HTTP_PORT = 11087;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();

        discoSpi.setIpFinder(sharedStaticIpFinder);

        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setDiscoverySpi(discoSpi);
        cfg.setIgniteInstanceName(igniteInstanceName);

        return cfg;
    }

    @Before
    public void setup() throws Exception {
        System.setProperty(IGNITE_JETTY_PORT, String.valueOf(GRID_HTTP_PORT));

        try {
            startGrid();
        }
        finally {
            System.clearProperty(IGNITE_JETTY_PORT);
        }
    }

    @After
    public void tearDown() {
        GridRouterFactory.stopAllRouters();

        stopAllGrids();
    }

    /**
     * Test router's start/stop.
     *
     * @throws Exception In case of any exception.
     */
    @Test
    public void testRouterFactory() throws Exception {
        final int size = 20;
        final Collection<GridTcpRouter> tcpRouters = new ArrayList<>(size);
        final GridTcpRouterConfiguration tcpCfg = new GridTcpRouterConfiguration();

        tcpCfg.setPortRange(size);

        for (int i = 0; i < size; i++)
            tcpRouters.add(GridRouterFactory.startTcpRouter(tcpCfg));

        for (GridTcpRouter tcpRouter : tcpRouters) {
            assertEquals(tcpCfg, tcpRouter.configuration());
            assertEquals(tcpRouter, GridRouterFactory.tcpRouter(tcpRouter.id()));
        }

        assertEquals("Validate all started tcp routers.", new HashSet<>(tcpRouters),
            new HashSet<>(GridRouterFactory.allTcpRouters()));

        for (Iterator<GridTcpRouter> it = tcpRouters.iterator(); it.hasNext(); ) {
            GridTcpRouter tcpRouter = it.next();

            assertEquals("Validate all started tcp routers.", new HashSet<>(tcpRouters),
                new HashSet<>(GridRouterFactory.allTcpRouters()));

            it.remove();

            GridRouterFactory.stopTcpRouter(tcpRouter.id());

            assertEquals("Validate all started tcp routers.", new HashSet<>(tcpRouters),
                new HashSet<>(GridRouterFactory.allTcpRouters()));
        }

        assertEquals(Collections.<GridTcpRouter>emptyList(), GridRouterFactory.allTcpRouters());
    }
}
