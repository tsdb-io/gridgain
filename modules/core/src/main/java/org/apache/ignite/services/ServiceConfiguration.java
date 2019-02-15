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

package org.apache.ignite.services;

import java.io.Serializable;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.lang.IgnitePredicate;

/**
 * Managed service configuration. In addition to deploying managed services by
 * calling any of the provided {@code deploy(...)} methods, managed services
 * can also be automatically deployed on startup by specifying them in {@link org.apache.ignite.configuration.IgniteConfiguration}
 * like so:
 * <pre name="code" class="java">
 * IgniteConfiguration gridCfg = new IgniteConfiguration();
 *
 * GridServiceConfiguration svcCfg1 = new GridServiceConfiguration();
 *
 * svcCfg1.setName("myClusterSingletonService");
 * svcCfg1.setMaxPerNodeCount(1);
 * svcCfg1.setTotalCount(1);
 * svcCfg1.setService(new MyClusterSingletonService());
 *
 * GridServiceConfiguration svcCfg2 = new GridServiceConfiguration();
 *
 * svcCfg2.setName("myNodeSingletonService");
 * svcCfg2.setMaxPerNodeCount(1);
 * svcCfg2.setService(new MyNodeSingletonService());
 *
 * gridCfg.setServiceConfiguration(svcCfg1, svcCfg2);
 * ...
 * Ignition.start(gridCfg);
 * </pre>
 * The above configuration can also be specified in a Spring configuration file.
 */
public class ServiceConfiguration implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Service name. */
    protected String name;

    /** Service instance. */
    @GridToStringExclude
    private Service svc;

    /** Total count. */
    protected int totalCnt;

    /** Max per-node count. */
    protected int maxPerNodeCnt;

    /** Cache name. */
    protected String cacheName;

    /** Affinity key. */
    protected Object affKey;

    /** Node filter. */
    @GridToStringExclude
    protected IgnitePredicate<ClusterNode> nodeFilter;

    /**
     * Gets service name.
     * <p>
     * This parameter is mandatory when deploying a service.
     *
     * @return Service name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets service name.
     * <p>
     * This parameter is mandatory when deploying a service.
     *
     * @param name Service name.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setName(String name) {
        this.name = name;

        return this;
    }

    /**
     * Gets service instance.
     * <p>
     * This parameter is mandatory when deploying a service.
     *
     * @return Service instance.
     */
    public Service getService() {
        return svc;
    }

    /**
     * Sets service instance.
     * <p>
     * This parameter is mandatory when deploying a service.
     *
     * @param svc Service instance.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setService(Service svc) {
        this.svc = svc;

        return this;
    }

    /**
     * Gets total number of deployed service instances in the cluster, {@code 0} for unlimited.
     * <p>
     * At least one of {@code getTotalCount()} or {@link #getMaxPerNodeCount()} values must be positive.
     *
     * @return Total number of deployed service instances in the cluster, {@code 0} for unlimited.
     */
    public int getTotalCount() {
        return totalCnt;
    }

    /**
     * Sets total number of deployed service instances in the cluster, {@code 0} for unlimited.
     * <p>
     * At least one of {@code getTotalCount()} or {@link #getMaxPerNodeCount()} values must be positive.
     *
     * @param totalCnt Total number of deployed service instances in the cluster, {@code 0} for unlimited.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setTotalCount(int totalCnt) {
        this.totalCnt = totalCnt;

        return this;
    }

    /**
     * Gets maximum number of deployed service instances on each node, {@code 0} for unlimited.
     * <p>
     * At least one of {@code getMaxPerNodeCount()} or {@link #getTotalCount()} values must be positive.
     *
     * @return Maximum number of deployed service instances on each node, {@code 0} for unlimited.
     */
    public int getMaxPerNodeCount() {
        return maxPerNodeCnt;
    }

    /**
     * Sets maximum number of deployed service instances on each node, {@code 0} for unlimited.
     * <p>
     * At least one of {@code getMaxPerNodeCount()} or {@link #getTotalCount()} values must be positive.
     *
     * @param maxPerNodeCnt Maximum number of deployed service instances on each node, {@code 0} for unlimited.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setMaxPerNodeCount(int maxPerNodeCnt) {
        this.maxPerNodeCnt = maxPerNodeCnt;

        return this;
    }

    /**
     * Gets cache name used for key-to-node affinity calculation.
     * <p>
     * This parameter is optional and is set only when deploying service based on key-affinity.
     * <p/>
     * <b>NOTE:</b> If the cache is destroyed, the service will be undeployed automatically.
     *
     * @return Cache name, possibly {@code null}.
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Sets cache name used for key-to-node affinity calculation.
     * <p>
     * This parameter is optional and is set only when deploying service based on key-affinity.
     *
     * @param cacheName Cache name, possibly {@code null}.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setCacheName(String cacheName) {
        this.cacheName = cacheName;

        return this;
    }

    /**
     * Gets affinity key used for key-to-node affinity calculation.
     * <p>
     * This parameter is optional and is set only when deploying service based on key-affinity.
     *
     * @return Affinity key, possibly {@code null}.
     */
    public Object getAffinityKey() {
        return affKey;
    }

    /**
     * Sets affinity key used for key-to-node affinity calculation.
     * <p>
     * This parameter is optional and is set only when deploying service based on key-affinity.
     *
     * @param affKey Affinity key, possibly {@code null}.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setAffinityKey(Object affKey) {
        this.affKey = affKey;

        return this;
    }

    /**
     * Gets node filter used to filter nodes on which the service will be deployed.
     * <p>
     * This parameter is optional. If not provided service may be deployed on any or all
     * nodes in the grid, based on configuration.
     *
     * @return Node filter used to filter nodes on which the service will be deployed, possibly {@code null}.
     */
    public IgnitePredicate<ClusterNode> getNodeFilter() {
        return nodeFilter;
    }

    /**
     * Sets node filter used to filter nodes on which the service will be deployed.
     * <p>
     * This parameter is optional. If not provided service may be deployed on any or all
     * nodes in the grid, based on configuration.
     *
     * @param nodeFilter Node filter used to filter nodes on which the service will be deployed, possibly {@code null}.
     * @return {@code this} for chaining.
     */
    public ServiceConfiguration setNodeFilter(IgnitePredicate<ClusterNode> nodeFilter) {
        this.nodeFilter = nodeFilter;

        return this;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (!equalsIgnoreNodeFilter(o))
            return false;

        ServiceConfiguration that = (ServiceConfiguration)o;

        if (nodeFilter != null && that.nodeFilter != null) {
            if (!nodeFilter.getClass().equals(that.nodeFilter.getClass()))
                return false;
        }
        else if (nodeFilter != null || that.nodeFilter != null)
            return false;

        return true;
    }

    /**
     * Checks if configurations are equal ignoring the node filter. Node filters control on which
     * nodes the services are deployed and often can be ignored for equality checks.
     *
     * @param o Other configuration.
     * @return {@code True} if configurations are equal, {@code false} otherwise.
     */
    @SuppressWarnings("RedundantIfStatement")
    public boolean equalsIgnoreNodeFilter(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        ServiceConfiguration that = (ServiceConfiguration)o;

        if (maxPerNodeCnt != that.maxPerNodeCnt)
            return false;

        if (totalCnt != that.totalCnt)
            return false;

        if (affKey != null ? !affKey.equals(that.affKey) : that.affKey != null)
            return false;

        if (cacheName != null ? !cacheName.equals(that.cacheName) : that.cacheName != null)
            return false;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        if (svc != null ? !svc.getClass().equals(that.svc.getClass()) : that.svc != null)
            return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        String svcCls = svc == null ? "" : svc.getClass().getSimpleName();
        String nodeFilterCls = nodeFilter == null ? "" : nodeFilter.getClass().getSimpleName();

        return S.toString(ServiceConfiguration.class, this, "svcCls", svcCls, "nodeFilterCls", nodeFilterCls);
    }
}
