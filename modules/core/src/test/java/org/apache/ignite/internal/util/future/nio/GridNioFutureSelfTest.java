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

package org.apache.ignite.internal.util.future.nio;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.IgniteFutureCancelledCheckedException;
import org.apache.ignite.internal.IgniteFutureTimeoutCheckedException;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.nio.GridNioFuture;
import org.apache.ignite.internal.util.nio.GridNioFutureImpl;
import org.apache.ignite.internal.util.typedef.CI1;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * Test for NIO future.
 */
public class GridNioFutureSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    @Test
    public void testOnDone() throws Exception {
        GridNioFutureImpl<String> fut = new GridNioFutureImpl<>(null);

        fut.onDone();

        assertNull(fut.get());

        fut = new GridNioFutureImpl<>(null);

        fut.onDone("test");

        assertEquals("test", fut.get());

        fut = new GridNioFutureImpl<>(null);

        fut.onDone(new IgniteCheckedException("TestMessage"));

        final GridNioFutureImpl<String> callFut1 = fut;

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return callFut1.get();
            }
        }, IgniteCheckedException.class, "TestMessage");

        fut = new GridNioFutureImpl<>(null);

        fut.onDone("test", new IgniteCheckedException("TestMessage"));

        final GridNioFuture<String> callFut2 = fut;

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return callFut2.get();
            }
        }, IgniteCheckedException.class, "TestMessage");

        fut = new GridNioFutureImpl<>(null);

        fut.onDone("test");

        fut.onCancelled();

        assertEquals("test", fut.get());
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testOnCancelled() throws Exception {
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridNioFutureImpl<String> fut = new GridNioFutureImpl<>(null);

                fut.onCancelled();

                return fut.get();
            }
        }, IgniteFutureCancelledCheckedException.class, null);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridNioFutureImpl<String> fut = new GridNioFutureImpl<>(null);

                fut.onCancelled();

                fut.onDone();

                return fut.get();
            }
        }, IgniteFutureCancelledCheckedException.class, null);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testListenSyncNotify() throws Exception {
        GridNioFutureImpl<String> fut = new GridNioFutureImpl<>(null);

        int lsnrCnt = 10;

        final CountDownLatch latch = new CountDownLatch(lsnrCnt);

        final Thread runThread = Thread.currentThread();

        final AtomicReference<Exception> err = new AtomicReference<>();

        for (int i = 0; i < lsnrCnt; i++) {
            fut.listen(new CI1<IgniteInternalFuture<String>>() {
                @Override public void apply(IgniteInternalFuture<String> t) {
                    if (Thread.currentThread() != runThread)
                        err.compareAndSet(null, new Exception("Wrong notification thread: " + Thread.currentThread()));

                    latch.countDown();
                }
            });
        }

        fut.onDone();

        assertEquals(0, latch.getCount());

        if (err.get() != null)
            throw err.get();

        final AtomicBoolean called = new AtomicBoolean();

        err.set(null);

        fut.listen(new CI1<IgniteInternalFuture<String>>() {
            @Override public void apply(IgniteInternalFuture<String> t) {
                if (Thread.currentThread() != runThread)
                    err.compareAndSet(null, new Exception("Wrong notification thread: " + Thread.currentThread()));

                called.set(true);
            }
        });

        assertTrue(called.get());

        if (err.get() != null)
            throw err.get();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testGet() throws Exception {
        GridNioFutureImpl<Object> unfinished = new GridNioFutureImpl<>(null);
        GridNioFutureImpl<Object> finished = new GridNioFutureImpl<>(null);
        GridNioFutureImpl<Object> cancelled = new GridNioFutureImpl<>(null);

        finished.onDone("Finished");

        cancelled.onCancelled();

        try {
            unfinished.get(50);

            assert false;
        }
        catch (IgniteFutureTimeoutCheckedException e) {
            info("Caught expected exception: " + e);
        }

        Object o = finished.get();

        assertEquals("Finished", o);

        o = finished.get(1000);

        assertEquals("Finished", o);

        try {
            cancelled.get();

            assert false;
        }
        catch (IgniteFutureCancelledCheckedException e) {
            info("Caught expected exception: " + e);
        }

        try {
            cancelled.get(1000);

            assert false;
        }
        catch (IgniteFutureCancelledCheckedException e) {
            info("Caught expected exception: " + e);
        }

    }
}
