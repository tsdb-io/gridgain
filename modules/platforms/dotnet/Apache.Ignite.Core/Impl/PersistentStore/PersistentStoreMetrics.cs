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

namespace Apache.Ignite.Core.Impl.PersistentStore
{
    using System;
    using System.Diagnostics;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.PersistentStore;
    
    /// <summary>
    /// Persistent store metrics.
    /// </summary>
#pragma warning disable 618
    internal class PersistentStoreMetrics : IPersistentStoreMetrics
#pragma warning restore 618
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="PersistentStoreMetrics"/> class.
        /// </summary>
        public PersistentStoreMetrics(IBinaryRawReader reader)
        {
            Debug.Assert(reader != null);

            WalLoggingRate = reader.ReadFloat();
            WalWritingRate = reader.ReadFloat();
            WalArchiveSegments = reader.ReadInt();
            WalFsyncTimeAverage = reader.ReadFloat();
            LastCheckpointingDuration = reader.ConfigReadLongAsTimespan();
            LastCheckpointLockWaitDuration = reader.ConfigReadLongAsTimespan();
            LastCheckpointMarkDuration = reader.ConfigReadLongAsTimespan();
            LastCheckpointPagesWriteDuration = reader.ConfigReadLongAsTimespan();
            LastCheckpointFsyncDuration = reader.ConfigReadLongAsTimespan();
            LastCheckpointTotalPagesNumber = reader.ReadLong();
            LastCheckpointDataPagesNumber = reader.ReadLong();
            LastCheckpointCopiedOnWritePagesNumber = reader.ReadLong();
        }

        /** <inheritdoc /> */
        public float WalLoggingRate { get; private set; }

        /** <inheritdoc /> */
        public float WalWritingRate { get; private set; }

        /** <inheritdoc /> */
        public int WalArchiveSegments { get; private set; }

        /** <inheritdoc /> */
        public float WalFsyncTimeAverage { get; private set; }

        /** <inheritdoc /> */
        public TimeSpan LastCheckpointingDuration { get; private set; }

        /** <inheritdoc /> */
        public TimeSpan LastCheckpointLockWaitDuration { get; private set; }

        /** <inheritdoc /> */
        public TimeSpan LastCheckpointMarkDuration { get; private set; }

        /** <inheritdoc /> */
        public TimeSpan LastCheckpointPagesWriteDuration { get; private set; }

        /** <inheritdoc /> */
        public TimeSpan LastCheckpointFsyncDuration { get; private set; }

        /** <inheritdoc /> */
        public long LastCheckpointTotalPagesNumber { get; private set; }

        /** <inheritdoc /> */
        public long LastCheckpointDataPagesNumber { get; private set; }

        /** <inheritdoc /> */
        public long LastCheckpointCopiedOnWritePagesNumber { get; private set; }
    }
}
