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

 /**
  * @file
  * Declares ignite::cluster::ClusterNode class.
  */

#ifndef _IGNITE_CLUSTER_CLUSTER_NODE
#define _IGNITE_CLUSTER_CLUSTER_NODE

#include <ignite/impl/cluster/cluster_node_impl.h>

namespace ignite
{
    namespace cluster
    {
        /**
         * Interface representing a single cluster node.
         * Use GetAttribute(String) or GetMetrics() to get static and dynamic information about cluster nodes.
         */
        class IGNITE_IMPORT_EXPORT ClusterNode
        {
        public:
            /**
             * Constructor.
             *
             * @param impl Pointer to cluster node implementation.
             */
            ClusterNode(common::concurrent::SharedPointer<ignite::impl::cluster::ClusterNodeImpl> impl);

            /**
             * Get collection of addresses this node is known by.
             *
             * @return Collection of addresses this node is known by.
             */
            std::vector<std::string> GetAddresses();

            /**
             * Check if node attribute is set.
             *
             * @param name Node attribute name.
             * @return True if set.
             */
            bool IsAttributeSet(std::string name);

            /**
             * Get a node attribute.
             *
             * @param name Node attribute name.
             * @return Node attribute.
             *
             * @throw IgniteError in case of attribute name does not exist
             * or if template type is not compatible with attribute.
             */
            template<typename T>
            T GetAttribute(std::string name)
            {
                return impl.Get()->GetAttribute<T>(name);
            }

            /**
             * Get collection of all Cluster Node attributes names.
             *
             * @return Node attributes names collection.
             */
            std::vector<std::string> GetAttributes();

            /**
             * Get Cluster Node consistent ID.
             *
             * @return Cluster Node consistent ID.
             */
            std::string GetConsistentId();

            /**
             * Get collection of host names this node is known by.
             *
             * @return Collection of host names this node is known by.
             */
            std::vector<std::string> GetHostNames();

            /**
             * Get globally unique node ID. A new ID is generated every time a node restarts.
             *
             * @return Node Guid.
             */
            Guid GetId();

            /**
             * Check if cluster node started in client mode.
             *
             * @return True if in client mode and false otherwise.
             */
            bool IsClient();

            /**
             * Check whether or not this node is a daemon.
             *
             * @return True if is daemon and false otherwise.
             */
            bool IsDaemon();

            /**
             * Check whether or not this node is a local node.
             *
             * @return True if is local and false otherwise.
             */
            bool IsLocal();

            /**
             * Node order within grid topology.
             *
             * @return Node order.
             */
            int64_t GetOrder();

            /**
             * Get node version.
             *
             * @return Prodcut version.
             */
            const IgniteProductVersion& GetVersion();

        private:
            common::concurrent::SharedPointer<ignite::impl::cluster::ClusterNodeImpl> impl;
        };
    }
}

#endif //_IGNITE_CLUSTER_CLUSTER_NODE