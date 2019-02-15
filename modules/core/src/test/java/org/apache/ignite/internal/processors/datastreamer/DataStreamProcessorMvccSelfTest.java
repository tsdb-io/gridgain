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

package org.apache.ignite.internal.processors.datastreamer;

import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT;

/**
 * Check DataStreamer with Mvcc enabled.
 */
public class DataStreamProcessorMvccSelfTest extends DataStreamProcessorSelfTest {
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration igniteConfiguration = super.getConfiguration(igniteInstanceName);

        CacheConfiguration[] ccfgs = igniteConfiguration.getCacheConfiguration();

        if (ccfgs != null) {
            for (CacheConfiguration ccfg : ccfgs)
                ccfg.setNearConfiguration(null);
        }

        assert ccfgs == null || ccfgs.length == 0 ||
            (ccfgs.length == 1 && ccfgs[0].getAtomicityMode() == TRANSACTIONAL_SNAPSHOT);

        return igniteConfiguration;
    }

    /** {@inheritDoc} */
    @Override protected CacheAtomicityMode getCacheAtomicityMode() {
        return TRANSACTIONAL_SNAPSHOT;
    }

    /** {@inheritDoc} */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-8582")
    @Test
    @Override public void testUpdateStore() throws Exception {
        super.testUpdateStore();
    }

    /** {@inheritDoc} */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-9321")
    @Test
    @Override public void testFlushTimeout() throws Exception {
        super.testFlushTimeout();
    }

    /** {@inheritDoc} */
    @Ignore("https://issues.apache.org/jira/browse/IGNITE-9530")
    @Test
    @Override public void testLocal() throws Exception {
        super.testLocal();
    }

    /** {@inheritDoc} */
    @Test
    @Override public void testTryFlush() throws Exception {
        super.testTryFlush();
    }
}
