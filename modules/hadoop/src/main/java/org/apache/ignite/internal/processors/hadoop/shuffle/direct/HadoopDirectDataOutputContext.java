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

package org.apache.ignite.internal.processors.hadoop.shuffle.direct;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.hadoop.HadoopSerialization;
import org.apache.ignite.internal.processors.hadoop.HadoopTaskContext;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Hadoop data output context for direct communication.
 */
public class HadoopDirectDataOutputContext {
    /** Initial allocation size for GZIP output. We start with very low value, but then it will grow if needed. */
    private static final int GZIP_OUT_MIN_ALLOC_SIZE = 1024;

    /** GZIP buffer size. We should remove it when we implement efficient direct GZIP output. */
    private static final int GZIP_BUFFER_SIZE = 8096;

    /** Flush size. */
    private final int flushSize;

    /** Whether to perform GZIP. */
    private final boolean gzip;

    /** Key serialization. */
    private final HadoopSerialization keySer;

    /** Value serialization. */
    private final HadoopSerialization valSer;

    /** Data output. */
    private HadoopDirectDataOutput out;

    /** Data output for GZIP. */
    private HadoopDirectDataOutput gzipOut;

    /** Number of keys written. */
    private int cnt;

    /**
     * Constructor.
     *
     * @param flushSize Flush size.
     * @param gzip Whether to perform GZIP.
     * @param taskCtx Task context.
     * @throws IgniteCheckedException If failed.
     */
    public HadoopDirectDataOutputContext(int flushSize, boolean gzip, HadoopTaskContext taskCtx)
        throws IgniteCheckedException {
        this.flushSize = flushSize;
        this.gzip = gzip;

        keySer = taskCtx.keySerialization();
        valSer = taskCtx.valueSerialization();

        out = new HadoopDirectDataOutput(flushSize);

        if (gzip)
            gzipOut = new HadoopDirectDataOutput(Math.max(flushSize / 8, GZIP_OUT_MIN_ALLOC_SIZE));
    }

    /**
     * Write key-value pair.
     *
     * @param key Key.
     * @param val Value.
     * @return Whether flush is needed.
     * @throws IgniteCheckedException If failed.
     */
    public boolean write(Object key, Object val) throws IgniteCheckedException {
        keySer.write(out, key);
        valSer.write(out, val);

        cnt++;

        return out.readyForFlush();
    }

    /**
     * @return Key-value pairs count.
     */
    public int count() {
        return cnt;
    }

    /**
     * @return State.
     */
    public HadoopDirectDataOutputState state() {
        if (gzip) {
            try {
                try (GZIPOutputStream gzip = new GZIPOutputStream(gzipOut, GZIP_BUFFER_SIZE)) {
                    gzip.write(out.buffer(), 0, out.position());
                }

                return new HadoopDirectDataOutputState(gzipOut.buffer(), gzipOut.position(), out.position());
            }
            catch (IOException e) {
                throw new IgniteException("Failed to compress.", e);
            }
        }
        else
            return new HadoopDirectDataOutputState(out.buffer(), out.position(), out.position());
    }

    /**
     * Reset buffer.
     */
    public void reset() {
        if (gzip) {
            // In GZIP mode we do not expose normal output to the outside. Hence, no need for reallocation, just reset.
            out.reset();

            gzipOut = new HadoopDirectDataOutput(gzipOut.bufferLength());
        }
        else
            out = new HadoopDirectDataOutput(flushSize, out.bufferLength());

        cnt = 0;
    }
}
