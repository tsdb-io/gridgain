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

package org.apache.ignite.internal.util.nio;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgniteRunnable;

/**
 * Message tracker.
 */
public class GridNioMessageTracker implements IgniteRunnable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final GridNioSession ses;

    /** */
    private final int msgQueueLimit;

    /** */
    private final Lock lock = new ReentrantLock();

    /** */
    private final AtomicInteger msgCnt = new AtomicInteger();

    /** */
    private volatile boolean paused;

    /**
     * @param ses Session.
     * @param msgQueueLimit Message queue limit.
     */
    public GridNioMessageTracker(GridNioSession ses, int msgQueueLimit) {
        this.ses = ses;
        this.msgQueueLimit = msgQueueLimit;
    }

    /** {@inheritDoc} */
    @Override public void run() {
        // In case of ordered messages this may be called twice for 1 message.
        // Example: message arrives, but listener has not been installed yet.
        // Message set is created, but message does not get actually processed.
        // If this is not called, connection may be paused which causes hang.
        // It seems acceptable to have the following logic accounting the aforementioned.
        int cnt = 0;

        for (; ; ) {
            int cur = msgCnt.get();

            if (cur == 0)
                break;

            cnt = cur - 1;

            if (msgCnt.compareAndSet(cur, cnt))
                break;
        }

        assert cnt >= 0 : "Invalid count [cnt=" + cnt + ", this=" + this + ']';

        if (cnt < msgQueueLimit && paused && lock.tryLock()) {
            try {
                // Double check.
                if (paused && msgCnt.get() < msgQueueLimit) {
                    ses.resumeReads();

                    paused = false;
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    /**
     *
     */
    public void onMessageProcessed() {
        run();
    }

    /**
     */
    public void onMessageReceived() {
        int cnt = msgCnt.incrementAndGet();

        if (cnt >= msgQueueLimit && !paused) {
            lock.lock();

            try {
                // Double check.
                if (!paused && msgCnt.get() >= msgQueueLimit) {
                    ses.pauseReads();

                    paused = true;
                }
            }
            finally {
                lock.unlock();
            }

            // Need to recheck since message processing threads
            // may have failed to acquire lock.
            if (paused && msgCnt.get() < msgQueueLimit && lock.tryLock()) {
                try {
                    // Double check only for pause, since count is incremented only
                    // in this method and only from one (current) thread.
                    if (paused) {
                        ses.resumeReads();

                        paused = false;
                    }
                }
                finally {
                    lock.unlock();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNioMessageTracker.class, this, "hash", System.identityHashCode(this));
    }
}