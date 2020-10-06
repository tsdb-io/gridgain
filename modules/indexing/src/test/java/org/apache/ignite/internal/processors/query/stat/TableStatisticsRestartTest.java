package org.apache.ignite.internal.processors.query.stat;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.processors.query.h2.TableStatisticsAbstractTest;
import org.junit.Test;

public class TableStatisticsRestartTest extends TableStatisticsAbstractTest {

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setConsistentId(igniteInstanceName);

        DataStorageConfiguration memCfg = new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(
                        new DataRegionConfiguration()
                                .setPersistenceEnabled(true)
                );

        cfg.setDataStorageConfiguration(memCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        cleanPersistenceDir();

        startGridsMultiThreaded(1);

        grid(0).getOrCreateCache(DEFAULT_CACHE_NAME);

        runSql("DROP TABLE IF EXISTS small");

        runSql("CREATE TABLE small (a INT PRIMARY KEY, b INT, c INT)");

        runSql("CREATE INDEX small_b ON small(b)");

        runSql("CREATE INDEX small_c ON small(c)");

        IgniteCache<Integer, Object> cache = grid(0).cache(DEFAULT_CACHE_NAME);

        for (int i = 0; i < SMALL_SIZE; i++)
            runSql("INSERT INTO small(a, b, c) VALUES(" + i + "," + i + "," + i % 10 + ")");
    }

    @Test
    public void testRestart() {

    }
}
