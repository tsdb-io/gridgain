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

package org.apache.ignite.internal.processors.cache;

import java.util.concurrent.Callable;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.binary.BinaryCachingMetadataHandler;
import org.apache.ignite.internal.binary.BinaryContext;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.internal.binary.BinaryObjectImpl;
import org.apache.ignite.internal.binary.GridBinaryMarshaller;
import org.apache.ignite.internal.binary.builder.BinaryObjectBuilderImpl;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.logger.NullLogger;
import org.apache.ignite.marshaller.MarshallerContextTestImpl;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * Grid cache utils test.
 */
public class GridCacheUtilsSelfTest extends GridCommonAbstractTest {
    /**
     * Does not override equals and hashCode.
     */
    private static class NoEqualsAndHashCode {
    }

    /**
     * Does not override equals.
     */
    private static class NoEquals {
        /** {@inheritDoc} */
        @Override public int hashCode() {
            return 1;
        }
    }

    /**
     * Does not override hashCode.
     */
    private static class NoHashCode {
        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    /**
     * Defines equals with different signature.
     */
    private static class WrongEquals {
        /**
         * @param obj Object.
         * @return {@code False}.
         */
        @Override public boolean equals(Object obj) {
            return false;
        }
    }

    /**
     * Overrides equals and hashCode.
     */
    private static class EqualsAndHashCode {
        /** {@inheritDoc} */
        @Override public int hashCode() {
            return super.hashCode();
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    /**
     * Extends class which overrides equals and hashCode.
     */
    private static class ExtendsClassWithEqualsAndHashCode extends EqualsAndHashCode {
    }

    /**
     * Extends class which overrides equals and hashCode, overrides equals and hashCode.
     */
    private static class ExtendsClassWithEqualsAndHashCode2 extends EqualsAndHashCode {
        /** {@inheritDoc} */
        @Override public int hashCode() {
            return super.hashCode();
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    /**
     */
    @Test
    public void testCacheKeyValidation() throws IgniteCheckedException {
        CU.validateCacheKey("key");

        CU.validateCacheKey(1);

        CU.validateCacheKey(1L);

        CU.validateCacheKey(1.0);

        CU.validateCacheKey(new ExtendsClassWithEqualsAndHashCode());

        CU.validateCacheKey(new ExtendsClassWithEqualsAndHashCode2());

        assertThrowsForInvalidKey(new NoEqualsAndHashCode());

        assertThrowsForInvalidKey(new NoEquals());

        assertThrowsForInvalidKey(new NoHashCode());

        assertThrowsForInvalidKey(new WrongEquals());

        BinaryObjectBuilderImpl binBuilder = new BinaryObjectBuilderImpl(binaryContext(),
            EqualsAndHashCode.class.getName());

        BinaryObject binObj = binBuilder.build();

        CU.validateCacheKey(binObj);

        BinaryObjectBuilderImpl binBuilder2 = new BinaryObjectBuilderImpl((BinaryObjectImpl) binObj);

        CU.validateCacheKey(binBuilder2.build());
    }

    /**
     * @return Binary marshaller.
     * @throws IgniteCheckedException if failed.
     */
    private BinaryMarshaller binaryMarshaller() throws IgniteCheckedException {
        IgniteConfiguration iCfg = new IgniteConfiguration();

        BinaryConfiguration bCfg = new BinaryConfiguration();

        iCfg.setBinaryConfiguration(bCfg);

        BinaryContext ctx = new BinaryContext(BinaryCachingMetadataHandler.create(), iCfg, new NullLogger());

        BinaryMarshaller marsh = new BinaryMarshaller();

        marsh.setContext(new MarshallerContextTestImpl(null));

        IgniteUtils.invoke(BinaryMarshaller.class, marsh, "setBinaryContext", ctx, iCfg);

        return marsh;
    }

    /**
     * @return Binary context.
     * @throws IgniteCheckedException if failed.
     */
    private BinaryContext binaryContext() throws IgniteCheckedException {
        GridBinaryMarshaller impl = U.field(binaryMarshaller(), "impl");

        return impl.context();
    }

    /**
     * @param key Cache key.
     */
    private void assertThrowsForInvalidKey(final Object key) {
        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                CU.validateCacheKey(key);

                return null;
            }
        }, IllegalArgumentException.class, null);
    }
}
