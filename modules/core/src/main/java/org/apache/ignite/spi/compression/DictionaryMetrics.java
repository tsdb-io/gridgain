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
 * Compression with dictionary cycling implementation metrics.
 */
public interface DictionaryMetrics {
    /**
     * Gets number of dictionaries trained by running instance.
     *
     * Trained dictionaries may be discarded if they did not yield sufficient win over the existing one, or they may be
     * loaded from persistence, so this number may differ from {@link #activeDictionaries()}.
     *
     * @return Number of trained dictionaries.
     */
    public int trainedDictionaries();

    /**
     * Gets number of active dictionaries.
     *
     * Active means that it may be still needed for decompression of some of records. Usually, only one is used for
     * compression at the same time, but historic dictionaries are needed to decompress previous generations of data.
     *
     * @return Number of active dictionaries.
     */
    public int activeDictionaries();

    /**
     * Gets number of samples collected currently.
     *
     * Samples are needed to train dictionaries. Please note that samples are rotated, so this value is not expected
     * to grow unbound.
     *
     * @return Number of collected samples.
     */
    public int collectedSamples();

    /**
     * Gets total number of bytes of samples collected currently.
     *
     * Samples are needed to train dictionaries. Please note that samples are rotated, so this value is not expected
     * to grow unbound.
     *
     * @return Total size of collected samples, in bytes.
     */
    public int collectedBytes();
}
