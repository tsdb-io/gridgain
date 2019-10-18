/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.processors.cache.index.AbstractIndexingCommonTest;

/** */
public abstract class SubqueryCacheAbstractTest extends AbstractIndexingCommonTest {
    /** Test schema. */
    private static final String TEST_SCHEMA = "TEST";

    /** Outer table name. */
    protected static final String OUTER_TBL = "OUTER_TBL";

    /** Inner table name. */
    protected static final String INNER_TBL = "INNER_TBL";

    /** Outer table size. */
    protected static final int OUTER_SIZE = 10;

    /** Inner table size. */
    protected static final int INNER_SIZE = 50;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrid(0);

        createSchema();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        for (String name : Arrays.asList(OUTER_TBL, INNER_TBL))
            grid(0).cache(name).clear();

        populateData();
    }

    /** */
    private void createSchema() {
        createCache(OUTER_TBL, Collections.singleton(new QueryEntity(Long.class.getTypeName(), "A_VAL")
            .setTableName(OUTER_TBL)
            .addQueryField("ID", Long.class.getName(), null)
            .addQueryField("JID", Long.class.getName(), null)
            .addQueryField("VAL", Long.class.getName(), null)
            .setKeyFieldName("ID")
        ));

        createCache(INNER_TBL, Collections.singleton(new QueryEntity(Long.class.getName(), "B_VAL")
            .setTableName(INNER_TBL)
            .addQueryField("ID", Long.class.getName(), null)
            .addQueryField("JID", Long.class.getName(), null)
            .addQueryField("VAL", String.class.getName(), null)
            .setKeyFieldName("ID")
        ));
    }

    /**
     * @param name Name.
     * @param qryEntities Query entities.
     */
    private void createCache(String name, Collection<QueryEntity> qryEntities) {
        grid(0).createCache(
            new CacheConfiguration<Long, Long>()
                .setName(name)
                .setSqlSchema(TEST_SCHEMA)
                .setSqlFunctionClasses(TestSQLFunctions.class)
                .setQueryEntities(qryEntities)
        );
    }

    /**
     * Fills caches with test data.
     */
    @SuppressWarnings("unchecked")
    private void populateData() {
        IgniteCache cacheA = grid(0).cache(OUTER_TBL);

        for (long i = 1; i <= OUTER_SIZE; ++i) {
            cacheA.put(i, grid(0).binary().builder("A_VAL")
                .setField("JID", i)
                .setField("VAL", i)
                .build());
        }

        IgniteCache cacheB = grid(0).cache(INNER_TBL);

        for (long i = 1; i <= INNER_SIZE; ++i)
            cacheB.put(i, grid(0).binary().builder("B_VAL")
                .setField("JID", i)
                .setField("VAL", String.format("val%03d", i))
                .build());
    }

    /**
     * @param sql SQL query.
     * @param args Query parameters.
     * @return Results cursor.
     */
    protected FieldsQueryCursor<List<?>> sql(String sql, Object... args) {
        return grid(0).context().query().querySqlFields(new SqlFieldsQuery(sql)
            .setSchema(TEST_SCHEMA)
            .setEnforceJoinOrder(true)
            .setLazy(true)
            .setArgs(args), false);
    }

    /**
     * Utility class with custom SQL functions.
     */
    public static class TestSQLFunctions {
        /** Count of functions call. */
        static final AtomicLong CALL_CNT = new AtomicLong();

        /** */
        @QuerySqlFunction(deterministic = true)
        public static long det_foo(long val) {
            CALL_CNT.getAndIncrement();

            return val;
        }

        /** */
        @QuerySqlFunction(deterministic = false)
        public static long nondet_foo(long val) {
            CALL_CNT.getAndIncrement();

            return val;
        }
    }
}
