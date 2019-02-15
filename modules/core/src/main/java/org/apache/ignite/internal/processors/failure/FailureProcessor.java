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

package org.apache.ignite.internal.processors.failure;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.FailureContext;
import org.apache.ignite.failure.FailureHandler;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.failure.StopNodeOrHaltFailureHandler;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.GridProcessorAdapter;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * General failure processing API
 */
public class FailureProcessor extends GridProcessorAdapter {
    /** Value of the system property that enables threads dumping on failure. */
    private static final boolean IGNITE_DUMP_THREADS_ON_FAILURE =
        IgniteSystemProperties.getBoolean(IgniteSystemProperties.IGNITE_DUMP_THREADS_ON_FAILURE, true);

    /** Ignite. */
    private final Ignite ignite;

    /** Handler. */
    private volatile FailureHandler hnd;

    /** Failure context. */
    private volatile FailureContext failureCtx;

    /** Reserve buffer, which can be dropped to handle OOME. */
    private volatile byte[] reserveBuf;

    /**
     * @param ctx Context.
     */
    public FailureProcessor(GridKernalContext ctx) {
        super(ctx);

        this.ignite = ctx.grid();
    }

    /** {@inheritDoc} */
    @Override public void start() throws IgniteCheckedException {
        FailureHandler hnd = ctx.config().getFailureHandler();

        if (hnd == null)
            hnd = getDefaultFailureHandler();

        reserveBuf = new byte[IgniteSystemProperties.getInteger(
            IgniteSystemProperties.IGNITE_FAILURE_HANDLER_RESERVE_BUFFER_SIZE, 64 * 1024)];

        assert hnd != null;

        this.hnd = hnd;

        U.quietAndInfo(log, "Configured failure handler: [hnd=" + hnd + ']');
    }

    /**
     * @return @{code True} if a node will be stopped by current handler in near time.
     */
    public boolean nodeStopping() {
        return failureCtx != null && !(hnd instanceof NoOpFailureHandler);
    }

    /**
     * This method is used to initialize local failure handler if {@link IgniteConfiguration} don't contain configured one.
     *
     * @return Default {@link FailureHandler} implementation.
     */
    protected FailureHandler getDefaultFailureHandler() {
        return new StopNodeOrHaltFailureHandler();
    }

    /**
     * @return Failure context.
     */
    public FailureContext failureContext() {
        return failureCtx;
    }

    /**
     * Processes failure accordingly to configured {@link FailureHandler}.
     *
     * @param failureCtx Failure context.
     * @return {@code True} If this very call led to Ignite node invalidation.
     */
    public boolean process(FailureContext failureCtx) {
        return process(failureCtx, hnd);
    }

    /**
     * Processes failure accordingly to given failure handler.
     *
     * @param failureCtx Failure context.
     * @param hnd Failure handler.
     * @return {@code True} If this very call led to Ignite node invalidation.
     */
    public synchronized boolean process(FailureContext failureCtx, FailureHandler hnd) {
        assert failureCtx != null;
        assert hnd != null;

        if (this.failureCtx != null) // Node already terminating, no reason to process more errors.
            return false;

        U.error(ignite.log(), "Critical system error detected. Will be handled accordingly to configured handler " +
            "[hnd=" + hnd + ", failureCtx=" + failureCtx + ']', failureCtx.error());

        if (reserveBuf != null && X.hasCause(failureCtx.error(), OutOfMemoryError.class))
            reserveBuf = null;

        if (IGNITE_DUMP_THREADS_ON_FAILURE)
            U.dumpThreads(log);

        boolean invalidated = hnd.onFailure(ignite, failureCtx);

        if (invalidated) {
            this.failureCtx = failureCtx;

            log.error("Ignite node is in invalid state due to a critical failure.");
        }

        return invalidated;
    }
}
