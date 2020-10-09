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
package org.apache.ignite.spi.compression;

import java.util.Date;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.processors.metric.MetricRegistry;
import org.apache.ignite.spi.compression.gzip.GzipCompressionSpi;
import org.apache.ignite.spi.metric.DoubleMetric;
import org.apache.ignite.spi.metric.LongMetric;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/** */
public class GzipCompressionMetricsTest extends GridCommonAbstractTest {
    /** */
    private static final int CYCLE = 75_000;

    /** */
    private static final int MAX_ITEMS = 3 * CYCLE;

    /** */
    private CacheConfiguration cacheConfiguration() {
        return new CacheConfiguration(DEFAULT_CACHE_NAME).setCompressionSpi(new GzipCompressionSpi());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        super.afterTest();
    }

    /**
     * Checks Gzip compression behavior with primitive keys.
     *
     * Starts 2 nodes.
     * Creates cache on node 1.
     * Puts entries in the loop on node 2.
     * Removes entries in the loop after cache is sufficiently full.
     * Checks metrics values.
     */
    @Test
    public void testPrimitiveKey() throws Exception {
        Ignite ignite1 = startGrid(0);

        Ignite ignite2 = startGrid(1);

        ignite1.createCache(cacheConfiguration());

        IgniteCache c = ignite2.cache(DEFAULT_CACHE_NAME);

        for (int i = 0; i < MAX_ITEMS; i++) {
            long seed = (i * i) % Integer.MAX_VALUE;
            c.put(seed, new TestObject(seed));

            if (i > CYCLE) {
                int idToRmv = i - CYCLE;

                c.remove((idToRmv * idToRmv) % Integer.MAX_VALUE);
            }
        }

        checkMetrics(false);
    }

    /**
     * Checks Gzip compression behavior with composite but not compressable keys.
     *
     * Starts a node.
     * Creates cache.
     * Puts entries in the loop.
     * Removes entries in the loop after cache is sufficiently full.
     * Checks metrics values.
     */
    @Test
    public void testCompositeKey() throws Exception {
        Ignite ignite = startGrid(0);

        IgniteCache c = ignite.createCache(cacheConfiguration().setCompressKeys(true));

        for (int i = 0; i < MAX_ITEMS; i++) {
            long seed = (i * i) % Integer.MAX_VALUE;
            c.put(new TestKey(seed), new TestObject(seed));

            if (i > CYCLE) {
                int idToRmv = i - CYCLE;

                c.remove(new TestKey((idToRmv * idToRmv) % Integer.MAX_VALUE));
            }
        }

        checkMetrics(true);
    }

    /**
     * Checks Gzip compression behavior with composite, compressable keys and primitive values.
     *
     * Starts a node.
     * Creates cache.
     * Puts entries in the loop.
     * Removes entries in the loop after cache is sufficiently full.
     * Checks metrics values.
     */
    @Test
    public void testObjectiveKey() throws Exception {
        Ignite ignite = startGrid(0);

        IgniteCache c = ignite.createCache(cacheConfiguration().setCompressKeys(true));

        for (int i = 0; i < MAX_ITEMS; i++) {
            c.put(new TestObject(i), (long)i);

            if (i > CYCLE) {
                int idToRmv = i - CYCLE;

                assertTrue(c.remove(new TestObject(idToRmv)));
            }
        }

        checkMetrics(true);
    }

    /** */
    private void checkMetrics(boolean decompress) {
        MetricRegistry mreg = grid(0).context().metric().registry(
            "cacheGroups." + DEFAULT_CACHE_NAME + ".compression");

        double acceptance = mreg.<DoubleMetric>findMetric("Acceptance").value();
        assertTrue("Acceptance: " + acceptance, acceptance > 0.2 && acceptance <= 0.8);

        double ratio = mreg.<DoubleMetric>findMetric("Ratio").value();
        assertTrue("Ratio: " + ratio, ratio > 0.7 && ratio < 0.95);

        long totalCompNum = mreg.<LongMetric>findMetric("CompressionThroughputNumber").value();
        assertTrue("CompressionThroughputNumber: " + totalCompNum,
            totalCompNum > (MAX_ITEMS / 3) && totalCompNum < MAX_ITEMS * 3);

        long totalCompBytes = mreg.<LongMetric>findMetric("CompressionThroughputBytes").value();
        assertTrue("CompressionThroughputBytes: " + totalCompBytes,
            totalCompBytes > MAX_ITEMS * 50 && totalCompBytes < MAX_ITEMS * 300);

        if (decompress) {
            long totalDecompNum = mreg.<LongMetric>findMetric("DecompressionThroughputNumber").value();
            assertTrue("DecompressionThroughputNumber: " + totalDecompNum,
                totalDecompNum > (MAX_ITEMS / 6) && totalDecompNum < MAX_ITEMS * 2);

            long totalDecompBytes = mreg.<LongMetric>findMetric("DecompressionThroughputBytes").value();
            assertTrue("DecompressionThroughputBytes: " + totalDecompBytes,
                totalDecompBytes > MAX_ITEMS * 50 && totalDecompBytes < MAX_ITEMS * 300);
        }
    }

    /** */
    private static class TestObject {
        /** */
        private byte bVal;

        /** */
        private char cVal;

        /** */
        private short sVal;

        /** */
        private int iVal;

        /** */
        private long lVal;

        /** */
        private float fVal;

        /** */
        private double dVal;

        /** */
        private String strVal;

        /** */
        private String dateVal;

        /** */
        private byte[] byteArray;

        /**
         * @param seed Seed.
         */
        private TestObject(long seed) {
            bVal = (byte)seed;
            cVal = (char)seed;
            sVal = (short)seed;
            strVal = Long.toString(seed);
            iVal = (int)seed;
            lVal = seed;
            fVal = seed;
            dVal = seed;
            dateVal = new Date(seed).toString();
            byteArray = new byte[(int)Math.abs(seed % 64)];
        }
    }

    /**
     *
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class TestKey {
        /** */
        private String key;

        /** */
        @AffinityKeyMapped
        private int affKey;

        /**
         * @param seed Seed.
         */
        private TestKey(long seed) {
            key = Long.toHexString(seed);
            affKey = 0xfff & (int)seed;
        }
    }
}
