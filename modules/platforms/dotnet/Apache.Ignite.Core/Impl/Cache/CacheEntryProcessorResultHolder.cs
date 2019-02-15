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

namespace Apache.Ignite.Core.Impl.Cache
{
    using System;
    using System.Diagnostics.CodeAnalysis;
    using System.Globalization;
    using System.IO;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.Impl.Binary.IO;
    using BinaryWriter = Apache.Ignite.Core.Impl.Binary.BinaryWriter;

    /// <summary>
    /// Manages cache entry processing result in non-generic form.
    /// </summary>
    internal class CacheEntryProcessorResultHolder
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="CacheEntryProcessorResultHolder"/> class.
        /// </summary>
        /// <param name="entry">Entry.</param>
        /// <param name="processResult">Process result.</param>
        /// <param name="error">Error.</param>
        public CacheEntryProcessorResultHolder(IMutableCacheEntryInternal entry, object processResult, Exception error)
        {
            Entry = entry;
            ProcessResult = processResult;
            Error = error;
        }

        /// <summary>
        /// Gets the entry.
        /// </summary>
        public IMutableCacheEntryInternal Entry { get; private set; }

        /// <summary>
        /// Gets the process result.
        /// </summary>
        public object ProcessResult { get; private set; }

        /// <summary>
        /// Gets the error.
        /// </summary>
        public Exception Error { get; private set; }

        /// <summary>
        /// Writes this instance to the stream.
        /// </summary>
        /// <param name="stream">Stream.</param>
        /// <param name="marsh">Marshaller.</param>
        public void Write(IBinaryStream stream, Marshaller marsh)
        {
            var writer = marsh.StartMarshal(stream);

            try
            {
                Marshal(writer);
            }
            finally
            {
                marsh.FinishMarshal(writer);
            }
        }

        /// <summary>
        /// Marshal this instance.
        /// </summary>
        /// <param name="writer">Writer.</param>
        [SuppressMessage("Microsoft.Design", "CA1031:DoNotCatchGeneralExceptionTypes",
            Justification = "Any kind of exception can be thrown during user type marshalling.")]
        private void Marshal(BinaryWriter writer)
        {
            var pos = writer.Stream.Position;

            try
            {
                if (Error == null)
                {
                    writer.WriteByte((byte) Entry.State);

                    if (Entry.State == MutableCacheEntryState.ValueSet)
                        writer.Write(Entry.Value);

                    writer.Write(ProcessResult);
                }
                else
                {
                    writer.WriteByte((byte) MutableCacheEntryState.ErrBinary);
                    writer.Write(Error);
                }
            }
            catch (Exception marshErr)
            {
                writer.Stream.Seek(pos, SeekOrigin.Begin);

                writer.WriteByte((byte) MutableCacheEntryState.ErrString);

                if (Error == null)
                {
                    writer.WriteString(string.Format(CultureInfo.InvariantCulture,
                    "CacheEntryProcessor completed with error, but result serialization failed [errType={0}, " +
                    "err={1}, serializationErrMsg={2}]", marshErr.GetType().Name, marshErr, marshErr.Message));
                }
                else
                {
                    writer.WriteString(string.Format(CultureInfo.InvariantCulture,
                    "CacheEntryProcessor completed with error, and error serialization failed [errType={0}, " +
                    "err={1}, serializationErrMsg={2}]", marshErr.GetType().Name, marshErr, marshErr.Message));
                }
            }
        }
    }
}