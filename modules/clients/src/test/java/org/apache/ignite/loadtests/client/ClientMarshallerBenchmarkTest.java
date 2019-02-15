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

package org.apache.ignite.loadtests.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.client.marshaller.GridClientMarshaller;
import org.apache.ignite.internal.client.marshaller.jdk.GridClientJdkMarshaller;
import org.apache.ignite.internal.client.marshaller.optimized.GridClientOptimizedMarshaller;
import org.apache.ignite.internal.processors.rest.client.message.GridClientCacheRequest;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.marshaller.MarshallerUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.internal.processors.rest.client.message.GridClientCacheRequest.GridCacheOperation.CAS;

/**
 * Tests basic performance of marshallers.
 */
public class ClientMarshallerBenchmarkTest extends GridCommonAbstractTest {
    /** Marshallers to test. */
    private GridClientMarshaller[] marshallers;

    /**
     */
    public ClientMarshallerBenchmarkTest() {
        try {
            marshallers = new GridClientMarshaller[] {
                new GridClientJdkMarshaller(MarshallerUtils.classNameFilter(this.getClass().getClassLoader())),
                new GridClientOptimizedMarshaller()
            };
        }
        catch (IgniteCheckedException e) {
            throw new IgniteException(e);
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testCacheRequestTime() throws Exception {
        GridClientCacheRequest req = new GridClientCacheRequest(CAS);

        req.clientId(UUID.randomUUID());
        req.cacheName("CacheName");
        req.requestId(1024);
        req.key("key");
        req.value(1L);
        req.value2(2L);

        Map<Object, Object> additional = new HashMap<>();

        for (int i = 0; i < 1000; i++)
            additional.put("key" + i, (long)i);

        req.values(additional);

        // Warm up.
        for (GridClientMarshaller marshaller : marshallers) {
            GridClientCacheRequest res = runMarshallUnmarshalLoop(req, 1, marshaller);

            assertEquals(req.operation(), res.operation());
            assertEquals(0, res.requestId()); // requestId is not marshalled.
            assertEquals(null, res.clientId()); // clientId is not marshalled.
            assertEquals(null, res.destinationId()); // destinationId is not marshalled.
            assertEquals(req.cacheName(), res.cacheName());
            assertEquals(req.key(), res.key());
            assertEquals(req.value(), res.value());
            assertEquals(req.value2(), res.value2());

            for (Map.Entry<Object, Object> e : req.values().entrySet())
                assertEquals(e.getValue(), res.values().get(e.getKey()));
        }

        // Now real test.
        for (GridClientMarshaller marshaller : marshallers)
            runMarshallUnmarshalLoop(req, 1000, marshaller);
    }

    /**
     * Runs marshal/unmarshal loop and prints statistics.
     *
     * @param obj Object to marshal.
     * @param iterCnt Iteration count.
     * @param marshaller Marshaller to use.
     * @throws IOException If marshalling failed.
     * @return Unmarshalled object in last iteration
     */
    @SuppressWarnings("unchecked")
    private <T> T runMarshallUnmarshalLoop(T obj, int iterCnt, GridClientMarshaller marshaller)
        throws IOException {
        if (iterCnt == 1) {
            // Warm-up, will not print statistics.
            ByteBuffer buf = marshaller.marshal(obj, 0);

            byte[] arr = new byte[buf.remaining()];

            buf.get(arr);

            Object res = marshaller.unmarshal(arr);

            assertNotNull("Failed for marshaller: " + marshaller.getClass().getSimpleName(), res);

            return (T)res;
        }

        long marshallingTime = 0, unmarshallingTime = 0;

        long start = System.currentTimeMillis();

        Object res = null;

        for (int i = 0; i < iterCnt; i++) {
            ByteBuffer buf = marshaller.marshal(obj, 0);

            byte[] raw = new byte[buf.remaining()];

            buf.get(raw);

            long end = System.currentTimeMillis();

            marshallingTime += (end - start);

            start = end;

            res = marshaller.unmarshal(raw);

            assertNotNull(res);

            end = System.currentTimeMillis();

            unmarshallingTime += (end - start);

            start = end;
        }

        X.println("Marshalling statistics gathered [marshallerClass=" + marshaller.getClass().getSimpleName() +
            ", objClass=" + obj.getClass().getSimpleName() + ", marshallingTime=" + marshallingTime +
            ", unmarshallingTime=" + unmarshallingTime + "]");

        assert res != null;

        return (T)res;
    }
}
