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

namespace Apache.Ignite.Core.Client
{
    using System;
    using System.Diagnostics.CodeAnalysis;
    using System.Net;

    /// <summary>
    /// Represents Ignite client connection.
    /// </summary>
    public interface IClientConnection
    {
        /// <summary>
        /// Gets the remote EndPoint.
        /// </summary>
        [SuppressMessage("Microsoft.Naming", "CA1702:CompoundWordsShouldBeCasedCorrectly",
            Justification = "Consistency with EndPoint class name.")]
        EndPoint RemoteEndPoint { get; }

        /// <summary>
        /// Gets the local EndPoint.
        /// </summary>
        [SuppressMessage("Microsoft.Naming", "CA1702:CompoundWordsShouldBeCasedCorrectly",
            Justification = "Consistency with EndPoint class name.")]
        EndPoint LocalEndPoint { get; }

        /// <summary>
        /// Gets the server node id.
        /// </summary>
        Guid NodeId { get; }
    }
}