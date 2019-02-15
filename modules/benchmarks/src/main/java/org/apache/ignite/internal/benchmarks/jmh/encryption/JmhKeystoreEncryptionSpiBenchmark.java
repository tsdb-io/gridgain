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

package org.apache.ignite.internal.benchmarks.jmh.encryption;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.internal.benchmarks.jmh.JmhAbstractBenchmark;
import org.apache.ignite.spi.encryption.keystore.KeystoreEncryptionKey;
import org.apache.ignite.spi.encryption.keystore.KeystoreEncryptionSpi;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import static org.apache.ignite.internal.util.IgniteUtils.resolveIgnitePath;

/**
 */
public class JmhKeystoreEncryptionSpiBenchmark extends JmhAbstractBenchmark {
    /** Data amount. */
    private static final int DATA_AMOUNT = 100;

    public static final int PAGE_SIZE = 1024 * 4;

    /** */
    @Benchmark
    public void encryptBenchmark(EncryptionData d, Blackhole receiver) {
        for (int i = 0; i < DATA_AMOUNT; i++) {
            ByteBuffer[] dt = d.randomData[i];

            KeystoreEncryptionKey key = d.keys[ThreadLocalRandom.current().nextInt(4)];

            d.encSpi.encryptNoPadding(dt[0], key, dt[1]);

            receiver.consume(d.res);

            dt[0].rewind();
            dt[1].rewind();

            d.encSpi.decryptNoPadding(dt[1], key, dt[0]);
        }
    }

    @State(Scope.Thread)
    public static class EncryptionData {
        KeystoreEncryptionSpi encSpi;

        KeystoreEncryptionKey[] keys = new KeystoreEncryptionKey[4];

        ByteBuffer[][] randomData = new ByteBuffer[DATA_AMOUNT][2];

        ByteBuffer res = ByteBuffer.allocate(PAGE_SIZE);

        public EncryptionData() {
            encSpi = new KeystoreEncryptionSpi();

            encSpi.setKeyStorePath(resolveIgnitePath("modules/core/src/test/resources/tde.jks").getAbsolutePath());
            encSpi.setKeyStorePassword("love_sex_god".toCharArray());

            encSpi.onBeforeStart();
            encSpi.spiStart("test-instance");
        }

        @Setup(Level.Invocation)
        public void prepareCollection() {
            for (int i = 0; i < keys.length; i++)
                keys[i] = encSpi.create();

            for (int i = 0; i < DATA_AMOUNT; i++) {
                byte[] dt = new byte[PAGE_SIZE - 16];

                ThreadLocalRandom.current().nextBytes(dt);

                randomData[i][0] = ByteBuffer.wrap(dt);
                randomData[i][1] = ByteBuffer.allocate(PAGE_SIZE);
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            //No - op
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(JmhKeystoreEncryptionSpiBenchmark.class.getSimpleName())
            .threads(1)
            .forks(1)
            .warmupIterations(10)
            .measurementIterations(20)
            .build();

        new Runner(opt).run();
    }
}
