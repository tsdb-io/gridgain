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

package org.apache.ignite.internal.jdbc2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.IgniteJdbcDriver.CFG_URL_PREFIX;
import static org.apache.ignite.IgniteJdbcDriver.PROP_NODE_ID;
import static org.apache.ignite.cache.CacheMode.LOCAL;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 * Test JDBC with several local caches.
 */
public class JdbcLocalCachesSelfTest extends GridCommonAbstractTest {
    /** Cache name. */
    private static final String CACHE_NAME = "cache";

    /** JDBC URL. */
    private static final String BASE_URL =
        CFG_URL_PREFIX + "cache=" + CACHE_NAME + "@modules/clients/src/test/config/jdbc-config.xml";

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration cache = defaultCacheConfiguration();

        cache.setName(CACHE_NAME);
        cache.setCacheMode(LOCAL);
        cache.setWriteSynchronizationMode(FULL_SYNC);
        cache.setIndexedTypes(
            String.class, Integer.class
        );

        cfg.setCacheConfiguration(cache);

        cfg.setConnectorConfiguration(new ConnectorConfiguration());

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGridsMultiThreaded(2);

        IgniteCache<Object, Object> cache1 = grid(0).cache(CACHE_NAME);

        assert cache1 != null;

        cache1.put("key1", 1);
        cache1.put("key2", 2);

        IgniteCache<Object, Object> cache2 = grid(1).cache(CACHE_NAME);

        assert cache2 != null;

        cache2.put("key1", 3);
        cache2.put("key2", 4);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCache1() throws Exception {
        Properties cfg = new Properties();

        cfg.setProperty(PROP_NODE_ID, grid(0).localNode().id().toString());

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(BASE_URL, cfg);

            ResultSet rs = conn.createStatement().executeQuery("select _val from Integer order by _val");

            int cnt = 0;

            while (rs.next())
                assertEquals(++cnt, rs.getInt(1));

            assertEquals(2, cnt);
        }
        finally {
            if (conn != null)
                conn.close();
        }
    }

    /**
     * Verifies that <code>select count(*)</code> behaves correctly in
     * {@link org.apache.ignite.cache.CacheMode#LOCAL} mode.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testCountAll() throws Exception {
        Properties cfg = new Properties();

        cfg.setProperty(PROP_NODE_ID, grid(0).localNode().id().toString());

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(BASE_URL, cfg);

            ResultSet rs = conn.createStatement().executeQuery("select count(*) from Integer");

            assertTrue(rs.next());

            assertEquals(2L, rs.getLong(1));
        }
        finally {
            if (conn != null)
                conn.close();
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCache2() throws Exception {
        Properties cfg = new Properties();

        cfg.setProperty(PROP_NODE_ID, grid(1).localNode().id().toString());

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(BASE_URL, cfg);

            ResultSet rs = conn.createStatement().executeQuery("select _val from Integer order by _val");

            int cnt = 0;

            while (rs.next())
                assertEquals(++cnt + 2, rs.getInt(1));

            assertEquals(2, cnt);
        }
        finally {
            if (conn != null)
                conn.close();
        }
    }
}
