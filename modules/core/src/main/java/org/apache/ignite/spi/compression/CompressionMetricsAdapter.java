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

import java.io.Closeable;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.metric.MetricRegistry;
import org.apache.ignite.internal.processors.metric.impl.DoubleMetricImpl;
import org.apache.ignite.internal.processors.metric.impl.IntMetricImpl;
import org.apache.ignite.internal.processors.metric.impl.LongAdderMetric;
import org.apache.ignite.lang.IgniteExperimental;

import static org.apache.ignite.internal.processors.metric.impl.MetricUtils.cacheGroupMetricsRegistryName;
import static org.apache.ignite.internal.processors.metric.impl.MetricUtils.metricName;

/**
 * Implements compression metrics.
 */
@IgniteExperimental
public class CompressionMetricsAdapter implements CompressionMetrics, Closeable {
    /** */
    private static final int BATCH_SIZE = 1000;

    /**
     * Compression metrics registry name last part.
     * Full name will contain cache registry:
     * {@code "cache.sys-cache.compression."}, for example.
     */
    public static final String COMPRESSION_METRICS = "compression";

    /** Metric registry. */
    protected final MetricRegistry mreg;

    /** Number of bytes passed to {@link CompressionSpi#tryCompress(byte[])}. */
    private final LongAdderMetric compressionThroughputBytes;

    /** Number of invocations of {@link CompressionSpi#tryCompress(byte[])}. */
    private final LongAdderMetric compressionThroughputNumber;

    /** Number of bytes passed to {@link CompressionSpi#decompress(byte[])}. */
    private final LongAdderMetric decompressionThroughputBytes;

    /** Number of invocations of {@link CompressionSpi#decompress(byte[])}. */
    private final LongAdderMetric decompressionThroughputNumber;

    /** Current number of invocations of {@link CompressionSpi#tryCompress(byte[])}. */
    private final IntMetricImpl acceptanceDivisor;

    /** Current number of invocations of {@link CompressionSpi#tryCompress(byte[])} yielding compressed data. */
    private final IntMetricImpl acceptanceDividend;

    /** Fraction of data which was actually compressed. */
    private final DoubleMetricImpl acceptance;

    /** Current number of uncompressed bytes. */
    private final LongAdderMetric ratioDivisor;

    /** Current number of compressed bytes. */
    private final LongAdderMetric ratioDividend;

    /** Ratio of compressed data size to uncompressed size. */
    private final DoubleMetricImpl ratio;

    /**
     * @param ctx Kernal context.
     */
    public CompressionMetricsAdapter(GridKernalContext ctx, String implName, String cacheName) {
        mreg = metricRegistry(ctx, cacheName);

        mreg.objectMetric("Implementation", String.class,
            "Name of compression SPI implementation.").value(implName);

        compressionThroughputBytes = mreg.longAdderMetric("CompressionThroughputBytes",
            "Total compression throughput, as input bytes.");
        compressionThroughputNumber = mreg.longAdderMetric("CompressionThroughputNumber",
            "Total compression throughput, as number of records.");
        decompressionThroughputBytes = mreg.longAdderMetric("DecompressionThroughputBytes",
            "Total compression throughput, as input bytes.");
        decompressionThroughputNumber = mreg.longAdderMetric("DecompressionThroughputNumber",
            "Total compression throughput, as number of records.");

        acceptanceDivisor = new IntMetricImpl("acceptanceDivisor", "");
        acceptanceDividend = new IntMetricImpl("acceptanceDividend", "");
        acceptance = mreg.doubleMetric("Acceptance", "Fraction of records which were actually compressed.");

        ratioDivisor = new LongAdderMetric("ratioDivisor", "");
        ratioDividend = new LongAdderMetric("ratioDividend", "");
        ratio = mreg.doubleMetric("Ratio", "Ratio of compressed data size to uncompressed.");
    }

    /**
     * @return {@link MetricRegistry} for this metrics adapter.
     */
    public MetricRegistry metricRegistry(GridKernalContext ctx, String cacheName) {
        return ctx.metric().registry(metricName(
            cacheGroupMetricsRegistryName(cacheName), COMPRESSION_METRICS));
    }

    /** {@inheritDoc} */
    @Override public void close() {
        for (String m : new String[] {"Implementation", "CompressionThroughputBytes", "CompressionThroughputNumber",
            "DecompressionThroughputBytes", "DecompressionThroughputNumber", "Acceptance", "Ratio"})
            mreg.remove(m);
    }

    /** {@inheritDoc} */
    @Override public double acceptance() {
        return acceptance.value();
    }

    /** {@inheritDoc} */
    @Override public double ratio() {
        return ratio.value();
    }

    /** {@inheritDoc} */
    @Override public long compressionThroughputBytes() {
        return compressionThroughputBytes.value();
    }

    /** {@inheritDoc} */
    @Override public long compressionThroughputNumber() {
        return compressionThroughputNumber.value();
    }

    /** {@inheritDoc} */
    @Override public long decompressionThroughputBytes() {
        return decompressionThroughputBytes.value();
    }

    /** {@inheritDoc} */
    @Override public long decompressionThroughputNumber() {
        return decompressionThroughputNumber.value();
    }

    /** */
    public void onTryCompress(boolean accepted, int uncompressed, int compressed) {
        compressionThroughputNumber.increment();
        compressionThroughputBytes.add(uncompressed);

        acceptanceDivisor.increment();
        if (accepted) {
            acceptanceDividend.increment();
            ratioDivisor.add(uncompressed);
            ratioDividend.add(compressed);
        }

        if (acceptanceDivisor.value() >= BATCH_SIZE) {
            synchronized (this) {
                int acc = acceptanceDivisor.value();

                if (acc >= BATCH_SIZE) {
                    double curAcceptance = acceptanceDividend.value() / (double)acc;
                    double curRatio = ratioDividend.value() / (double)ratioDivisor.value();

                    acceptanceDividend.reset();
                    acceptanceDivisor.reset();
                    ratioDividend.reset();
                    ratioDivisor.reset();

                    acceptance.value(curAcceptance);
                    ratio.value(curRatio);
                }
            }
        }
    }

    /** */
    public void onDecompress(int compressed) {
        decompressionThroughputNumber.increment();
        decompressionThroughputBytes.add(compressed);
    }
}
