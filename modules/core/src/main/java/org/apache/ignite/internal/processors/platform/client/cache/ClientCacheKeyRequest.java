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

import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.internal.binary.BinaryRawReaderEx;
import org.apache.ignite.internal.processors.platform.client.ClientConnectionContext;
import org.apache.ignite.internal.processors.platform.client.tx.ClientTxAwareRequest;

/**
 * Cache request involving key.
 */
public abstract class ClientCacheKeyRequest extends ClientCacheDataRequest implements ClientTxAwareRequest {
    /** Key. */
    private final Object key;

    /**
     * Ctor.
     *
     * @param reader Reader.
     */
    ClientCacheKeyRequest(BinaryRawReaderEx reader) {
        super(reader);

        key = reader.readObjectDetached();
    }

    /** {@inheritDoc} */
    @Override public boolean isAsync(ClientConnectionContext ctx) {
        // Every cache data request on the transactional cache can lock the thread, even with implicit transaction.
        return cacheDescriptor(ctx).cacheConfiguration().getAtomicityMode() == CacheAtomicityMode.TRANSACTIONAL;
    }

    /**
     * Gets the key.
     *
     * @return Key.
     */
    public Object key() {
        return key;
    }
}
