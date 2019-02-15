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

package org.apache.ignite.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.ignite.testframework.junits.common.GridCommonTest;
import org.junit.Test;

/**
 * Grid basic communication test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridCommunicationSelfTest extends GridCommonAbstractTest {
    /** */
    private static Ignite ignite;

    /** */
    public GridCommunicationSelfTest() {
        super(/*start grid*/true);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        ignite = G.ignite(getTestIgniteInstanceName());
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        super.afterTestsStopped();

        ignite = null;
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testSendMessageToEmptyNodes() throws Exception {
        Collection<ClusterNode> empty = Collections.emptyList();

        try {
            sendMessage(empty, 1);
        }
        catch (IllegalArgumentException ignored) {
            // No-op.
        }
    }

    /**
     * @param nodes Nodes to send message to.
     * @param cntr Counter.
     */
    private void sendMessage(Collection<ClusterNode> nodes, int cntr) {
        try {
            message(ignite.cluster().forNodes(nodes)).send(null,
                new GridTestCommunicationMessage(cntr, ignite.cluster().localNode().id()));
        }
        catch (IgniteException e) {
            error("Failed to send message.", e);
        }
    }

    /**
     * Test message.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class GridTestCommunicationMessage implements Serializable {
        /** */
        private final int msgId;

        /** */
        private final UUID sndId;

        /**
         * @param msgId Message id.
         * @param sndId Sender id.
         */
        public GridTestCommunicationMessage(int msgId, UUID sndId) {
            assert sndId != null;

            this.msgId = msgId;
            this.sndId = sndId;
        }

        /**
         * @return Message id.
         */
        public int getMessageId() {
            return msgId;
        }

        /**
         * @return Sender id.
         */
        public UUID getSenderId() {
            return sndId;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            StringBuilder buf = new StringBuilder();

            buf.append(getClass().getSimpleName());
            buf.append(" [msgId=").append(msgId);
            buf.append(']');

            return buf.toString();
        }
    }
}
