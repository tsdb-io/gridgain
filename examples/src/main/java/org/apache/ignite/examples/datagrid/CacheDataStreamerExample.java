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

package org.apache.ignite.examples.datagrid;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.examples.ExampleNodeStartup;
import org.apache.ignite.examples.ExamplesUtils;

/**
 * Demonstrates how cache can be populated with data utilizing {@link IgniteDataStreamer} API.
 * {@link IgniteDataStreamer} is a lot more efficient to use than standard
 * {@code put(...)} operation as it properly buffers cache requests
 * together and properly manages load on remote nodes.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ignite.{sh|bat} examples/config/example-ignite.xml'}.
 * <p>
 * Alternatively you can run {@link ExampleNodeStartup} in another JVM which will
 * start node with {@code examples/config/example-ignite.xml} configuration.
 */
public class CacheDataStreamerExample {
    /** Cache name. */
    private static final String CACHE_NAME = CacheDataStreamerExample.class.getSimpleName();

    /** Number of entries to load. */
    private static final int ENTRY_COUNT = 500000;

    /** Heap size required to run this example. */
    public static final int MIN_MEMORY = 512 * 1024 * 1024;

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws IgniteException If example execution failed.
     */
    public static void main(String[] args) throws IgniteException {
        ExamplesUtils.checkMinMemory(MIN_MEMORY);

        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
            System.out.println();
            System.out.println(">>> Cache data streamer example started.");

            // Auto-close cache at the end of the example.
            try (IgniteCache<Integer, String> cache = ignite.getOrCreateCache(CACHE_NAME)) {
                long start = System.currentTimeMillis();

                try (IgniteDataStreamer<Integer, String> stmr = ignite.dataStreamer(CACHE_NAME)) {
                    // Configure loader.
                    stmr.perNodeBufferSize(1024);
                    stmr.perNodeParallelOperations(8);

                    for (int i = 0; i < ENTRY_COUNT; i++) {
                        stmr.addData(i, Integer.toString(i));

                        // Print out progress while loading cache.
                        if (i > 0 && i % 10000 == 0)
                            System.out.println("Loaded " + i + " keys.");
                    }
                }

                long end = System.currentTimeMillis();

                System.out.println(">>> Loaded " + ENTRY_COUNT + " keys in " + (end - start) + "ms.");
            }
            finally {
                // Distributed cache could be removed from cluster only by #destroyCache() call.
                ignite.destroyCache(CACHE_NAME);
            }
        }
    }
}