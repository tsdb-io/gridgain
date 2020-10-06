package org.apache.ignite.internal.processors.query.stat;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
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

        grid(0).context().query().getIndexing().statsManager().collectObjectStatistics("PUBLIC", "SMALL");
    }

    /**
     * Use select with two conditions which shows statistics presence.
     * 1) Check that select use correct index with statistics
     * 2) Restart grid
     * 3) Check that select use correct index with statistics
     * 4) Clear statistics
     * 5) Check that select use correct index WITHOUT statistics (i.e. other one)
     * @throws Exception
     */
    @Test
    public void testRestart() throws Exception {
        String isNullSql = "select * from SMALL i1 where b < 2 and c = 1";
        String[][] noHints = new String[1][];
        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"SMALL_B"}, isNullSql, noHints);

        stopGrid(0);

        startGrid(0);

        grid(0).cluster().state(ClusterState.ACTIVE);

        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"SMALL_B"}, isNullSql, noHints);

        grid(0).context().query().getIndexing().statsManager().clearObjectStatistics("PUBLIC", "SMALL");

        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"SMALL_C"}, isNullSql, noHints);
    }
}
