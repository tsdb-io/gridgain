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

package org.apache.ignite.internal.processors.platform.client.cache;

import org.apache.ignite.internal.binary.BinaryRawWriterEx;
import org.apache.ignite.internal.processors.platform.client.ClientConnectionContext;
import org.apache.ignite.internal.processors.platform.client.ClientResponse;

/**
 * Scan query response.
 */
class ClientCacheQueryResponse extends ClientResponse {
    /** Cursor. */
    private final ClientCacheQueryCursor cursor;

    /**
     * Ctor.
     *
     * @param requestId Request id.
     * @param cursor Cursor.
     */
    ClientCacheQueryResponse(long requestId, ClientCacheQueryCursor cursor) {
        super(requestId);

        assert cursor != null;

        this.cursor = cursor;
    }

    /** {@inheritDoc} */
    @Override public void encode(ClientConnectionContext ctx, BinaryRawWriterEx writer) {
        try {
            super.encode(ctx, writer);

            writer.writeLong(cursor.id());

            cursor.writePage(writer);
        } catch (Throwable t) {
            cursor.close();

            throw t;
        }
    }
}
