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

#pragma warning disable 649 // Readonly field is never assigned
namespace Apache.Ignite.Examples.Shared.Messaging
{
    using System;
    using Apache.Ignite.Core;
    using Apache.Ignite.Core.Messaging;
    using Apache.Ignite.Core.Resource;

    /// <summary>
    /// Listener for Ordered topic.
    /// </summary>
    public class RemoteOrderedMessageListener : IMessageListener<int>
    {
        /** Injected Ignite instance. */
        [InstanceResource]
        private readonly IIgnite _ignite;

        /// <summary>
        /// Receives a message and returns a value
        /// indicating whether provided message and node id satisfy this predicate.
        /// Returning false will unsubscribe this listener from future notifications.
        /// </summary>
        /// <param name="nodeId">Node identifier.</param>
        /// <param name="message">Message.</param>
        /// <returns>Value indicating whether provided message and node id satisfy this predicate.</returns>
        public bool Invoke(Guid nodeId, int message)
        {
            Console.WriteLine("Received ordered message [msg={0}, fromNodeId={1}]", message, nodeId);

            _ignite.GetCluster().ForNodeIds(nodeId).GetMessaging().Send(message, Topic.Ordered);

            return true;
        }
    }
}
