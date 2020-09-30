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

/**
 * Compression implementation metrics.
 */
public interface CompressionMetrics {
    /**
     * Gets current acceptance, a fraction of records which were actually compressed.
     *
     * Data is not picked for compression if it is not eligible for some reason, or if compression does not yield
     * sufficient space benefit.
     *
     * @return Acceptance, in [0, 1] range.
     */
    public double acceptance();

    /**
     * Gets current ratio of compressed data size to uncompressed size, for all data which was accepted.
     *
     * @see #acceptance()
     *
     * @return Ratio, in (0, 1) range.
     */
    public double ratio();

    /**
     * Gets total compression throughput, in bytes of all uncompressed data taken as input, regardless of whether
     * it was accepted for compression and of compression ratio.
     *
     * @return Throughput, in bytes.
     */
    public long compressionThroughputBytes();

    /**
     * Gets total compression throughput, as number of records taken as input, regardless of whether
     * it was accepted for compression and of compression ratio.
     *
     * @return Throughput, in number of records.
     */
    public long compressionThroughputNumber();

    /**
     * Gets total decompression throughput, in bytes of all compressed data taken as input.
     *
     * @return Throughput, in bytes.
     */
    public long decompressionThroughputBytes();

    /**
     * Gets total decompression throughput, as number of records taken as input.
     *
     * @return Throughput, in number of records.
     */
    public long decompressionThroughputNumber();
}
