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

package org.apache.ignite.internal.binary;

import java.io.Serializable;
import java.util.Collections;
import java.util.UUID;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinarySerializer;
import org.apache.ignite.binary.BinaryTypeConfiguration;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.client.GridClient;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.client.GridClientFactory;
import org.apache.ignite.internal.client.GridClientProtocol;
import org.apache.ignite.internal.client.balancer.GridClientRoundRobinBalancer;
import org.apache.ignite.internal.visor.VisorTaskArgument;
import org.apache.ignite.internal.visor.node.VisorNodePingTask;
import org.apache.ignite.internal.visor.node.VisorNodePingTaskArg;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * Tests that node will start with custom binary serializer and thin client will connect to such node.
 */
public class BinaryConfigurationCustomSerializerSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setConnectorConfiguration(new ConnectorConfiguration());

        cfg.setMarshaller(new BinaryMarshaller());

        BinaryConfiguration binaryCfg = new BinaryConfiguration();

        BinaryTypeConfiguration btc = new BinaryTypeConfiguration("org.MyClass");

        btc.setIdMapper(BinaryContext.defaultIdMapper());
        btc.setEnum(false);

        // Set custom serializer that is unknown for Optimized marshaller.
        btc.setSerializer(new MyBinarySerializer());

        binaryCfg.setTypeConfigurations(Collections.singletonList(btc));

        cfg.setBinaryConfiguration(binaryCfg);

        // Set custom consistent ID that unknown for Optimized marshaller.
        cfg.setConsistentId(new MyConsistentId("test"));

        cfg.setCacheConfiguration(new CacheConfiguration("TEST"));

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        startGrids(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * Test that thin client will be able to connect to node with custom binary serializer and custom consistent ID.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testThinClientConnected() throws Exception {
        UUID nid = ignite(0).cluster().localNode().id();

        GridClientConfiguration clnCfg = new GridClientConfiguration();

        clnCfg.setProtocol(GridClientProtocol.TCP);
        clnCfg.setServers(Collections.singleton("127.0.0.1:11211"));
        clnCfg.setBalancer(new GridClientRoundRobinBalancer());

        // Start client.
        GridClient client = GridClientFactory.start(clnCfg);

        // Execute some task.
        client.compute().execute(VisorNodePingTask.class.getName(),
            new VisorTaskArgument<>(nid, new VisorNodePingTaskArg(nid), false));

        GridClientFactory.stop(client.id(), false);
    }

    /**
     * Custom consistent ID.
     */
    private static class MyConsistentId implements Serializable {
        /** */
        private static final long serialVersionUID = 0L;

        /** Actual ID. */
        private String id;

        /**
         * @param id Actual ID.
         */
        MyConsistentId(String id) {
            this.id = id;
        }

        /**
         * @return Consistent ID.
         */
        public String getId() {
            return id;
        }
    }

    /**
     * Custom BinarySerializer.
     */
    private static class MyBinarySerializer implements BinarySerializer {
        /** {@inheritDoc} */
        @Override public void writeBinary(Object obj, BinaryWriter writer) throws BinaryObjectException {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void readBinary(Object obj, BinaryReader reader) throws BinaryObjectException {
            // No-op.
        }
    }
}
