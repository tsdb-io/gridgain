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

import java.util.Arrays;
import org.apache.ignite.spi.compression.gzip.GzipCompressionSpi;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/** */
public class GzipCompressionSelfTest extends GridCommonAbstractTest {
    /**
     * Creates GzipCompressionSpi.
     * Tests against null input.
     * Tests against zero byte input.
     * Tests against tiny non-compressable zero array.
     * Tests against smallest compressable zero array.
     * Tests against small non-trivial array.
     * Tests against large compressable zero array.
     */
    @Test
    public void test() {
        CompressionSpi compressionSpi = new GzipCompressionSpi();

        assertEquals(null, compressionSpi.tryCompress(null));

        assertEquals(null, compressionSpi.tryCompress(new byte[0]));

        assertEquals(null, compressionSpi.tryCompress(new byte[11]));

        assertEquals(23, compressionSpi.tryCompress(new byte[32]).length);

        byte[] b = (GzipCompressionSelfTest.class.getName() + GzipCompressionSelfTest.class.getName()).getBytes();

        assertTrue(Arrays.equals(b, compressionSpi.decompress(compressionSpi.tryCompress(b))));

        assertEquals(1024 * 1024 * 10, compressionSpi.decompress(
            compressionSpi.tryCompress(new byte[1024 * 1024 * 10])).length);
    }
}
