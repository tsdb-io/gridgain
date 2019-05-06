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

namespace Apache.Ignite.Core.Tests.Cache
{
    using System;
    using Apache.Ignite.Core.Binary;

    /// <summary>
    /// Non-serializable processor.
    /// </summary>
    public class NonSerializableCacheEntryProcessor : AddArgCacheEntryProcessor, IBinarizable
    {
        /** <inheritdoc /> */
        public void WriteBinary(IBinaryWriter writer)
        {
            throw new Exception("ExpectedException");
        }

        /** <inheritdoc /> */
        public void ReadBinary(IBinaryReader reader)
        {
            throw new Exception("ExpectedException");
        }
    }
}