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
package org.apache.ignite.spi.compression.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.cache.configuration.Factory;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.lang.IgniteExperimental;
import org.apache.ignite.spi.IgniteSpiAdapter;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.compression.CompressionMetricsAdapter;
import org.apache.ignite.spi.compression.CompressionSpi;

/**
 * Gzip-based implementation of {@link CompressionSpi}.
 */
@IgniteExperimental
public class GzipCompressionSpi extends IgniteSpiAdapter implements CompressionSpi, Factory<CompressionSpi> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    public static final int MIN_DELTA_BYTES = 8;

    /** */
    private transient CompressionMetricsAdapter metrics;

    /** {@inheritDoc} */
    @Override public byte[] tryCompress(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try (GZIPOutputStream gz = new GZIPOutputStream(out, bytes.length)) {
                gz.write(bytes);
            }
            catch (IOException ex) {
                log.warning("Failed to compress input", ex);
            }

            if (bytes.length - out.size() >= MIN_DELTA_BYTES) {
                if (metrics != null)
                    metrics.onTryCompress(true, bytes.length, out.size());

                return out.toByteArray();
            }
        }

        if (metrics != null)
            metrics.onTryCompress(false, bytes == null ? 0 : bytes.length, 0);

        return null;
    }

    /** {@inheritDoc} */
    @Override public byte[] decompress(byte[] bytes) {
        if (metrics != null)
            metrics.onDecompress(bytes.length);

        try {
            byte[] uncompressed = new byte[bytes.length * 2];

            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length)) {
                int len = gz.read(uncompressed);

                if (len < uncompressed.length)
                    return Arrays.copyOf(uncompressed, len);

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                do {
                    out.write(uncompressed, 0, len);

                    len = gz.read(uncompressed);
                } while (len > 0);

                return out.toByteArray();
            }
        }
        catch (IOException ex) {
            throw new IgniteSpiException("Failed to decompress input", ex);
        }
    }

    /** {@inheritDoc} */
    @Override public void spiStart(String igniteInstanceName, CacheConfiguration ccfg) throws IgniteSpiException {
        metrics = new CompressionMetricsAdapter(((IgniteEx)ignite).context(),
            GzipCompressionSpi.class.getName(), ccfg.getName());
    }

    /** {@inheritDoc} */
    @Override public CompressionSpi create() {
        return new GzipCompressionSpi();
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws IgniteSpiException {
        if (metrics != null)
            metrics.close();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        return o != null && (this == o || getClass() == o.getClass());
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return GzipCompressionSpi.class.hashCode();
    }

    /** */
    @Override public String toString() {
        return GzipCompressionSpi.class.getSimpleName();
    }
}
