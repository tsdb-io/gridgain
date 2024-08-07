﻿/*
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

namespace Apache.Ignite.Core.Impl.Binary
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Impl.Common;

    /// <summary>
    /// Reader extensions.
    /// </summary>
    internal static class BinaryReaderExtensions
    {
        /// <summary>
        /// Reads untyped collection as a generic list.
        /// </summary>
        /// <typeparam name="T">Type of list element.</typeparam>
        /// <param name="reader">The reader.</param>
        /// <returns>Resulting generic list.</returns>
        public static List<T> ReadCollectionAsList<T>(this IBinaryRawReader reader)
        {
            return ((List<T>) reader.ReadCollection(size => new List<T>(size),
                (col, elem) => ((List<T>) col).Add((T) elem)));
        }

        /// <summary>
        /// Reads untyped dictionary as generic dictionary.
        /// </summary>
        /// <typeparam name="TKey">The type of the key.</typeparam>
        /// <typeparam name="TValue">The type of the value.</typeparam>
        /// <param name="reader">The reader.</param>
        /// <returns>Resulting dictionary.</returns>
        public static Dictionary<TKey, TValue> ReadDictionaryAsGeneric<TKey, TValue>(this IBinaryRawReader reader)
        {
            return (Dictionary<TKey, TValue>) reader.ReadDictionary(size => new Dictionary<TKey, TValue>(size));
        }

        /// <summary>
        /// Reads the object either as a normal object or as a [typeName+props] wrapper.
        /// </summary>
        public static T ReadObjectEx<T>(this IBinaryRawReader reader)
        {
            var obj = reader.ReadObject<object>();

            if (obj == null)
                return default(T);

            return obj is T ? (T) obj : ((ObjectInfoHolder) obj).CreateInstance<T>();
        }

        /// <summary>
        /// Reads the collection. The collection could be produced by Java PlatformUtils.writeCollection()
        /// from org.apache.ignite.internal.processors.platform.utils package
        /// Note: return null if collection is empty
        /// </summary>
        public static IList<T> ReadCollectionRaw<T, TReader>(this TReader reader,
            Func<TReader, T> factory) where TReader : IBinaryRawReader
        {
            Debug.Assert(reader != null);
            Debug.Assert(factory != null);

            int count = reader.ReadInt();

            if (count <= 0)
            {
                return null;
            }

            var res = new List<T>(count);

            for (var i = 0; i < count; i++)
            {
                res.Add(factory(reader));
            }

            return res;
        }

        /// <summary>
        /// Reads the string collection.
        /// </summary>
        public static List<string> ReadStringCollection(this IBinaryRawReader reader)
        {
            Debug.Assert(reader != null);

            var cnt = reader.ReadInt();
            var res = new List<string>(cnt);

            for (var i = 0; i < cnt; i++)
            {
                res.Add(reader.ReadString());
            }

            return res;
        }

        /// <summary>
        /// Reads a nullable collection. The collection could be produced by Java 
        /// PlatformUtils.writeNullableCollection() from org.apache.ignite.internal.processors.platform.utils package.
        /// </summary>
        public static ICollection<T> ReadNullableCollectionRaw<T, TReader>(this TReader reader,
            Func<TReader, T> factory) where TReader : IBinaryRawReader
        {
            Debug.Assert(reader != null);
            Debug.Assert(factory != null);

            var hasVal = reader.ReadBoolean();

            if (!hasVal)
            {
                return null;
            }
            return ReadCollectionRaw(reader, factory);
        }
    }
}
