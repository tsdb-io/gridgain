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

package org.apache.ignite.testsuites;

import org.apache.ignite.igfs.IgfsFragmentizerSelfTest;
import org.apache.ignite.igfs.IgfsFragmentizerTopologySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsAtomicPrimaryMultiNodeSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsAtomicPrimarySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsAttributesSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsBackupsDualAsyncSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsBackupsDualSyncSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsBackupsPrimarySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsBlockMessageSystemPoolStarvationSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsCachePerBlockLruEvictionPolicySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsCacheSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsDualAsyncClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsDualSyncClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsLocalSecondaryFileSystemProxyClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsPrimaryClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsDataManagerSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsDualAsyncSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsDualSyncSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsFileInfoSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsFileMapSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsGroupDataBlockKeyMapperHashSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsMetaManagerSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsMetricsSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsModeResolverSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsModesSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsPrimaryMultiNodeSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsOneClientNodeTest;
import org.apache.ignite.internal.processors.igfs.IgfsPrimaryRelaxedConsistencyClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsPrimaryRelaxedConsistencyMultiNodeSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsPrimaryRelaxedConsistencySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsPrimarySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsProcessorSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsProcessorValidationSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsProxySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsLocalSecondaryFileSystemProxySelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsSecondaryFileSystemInjectionSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsServerManagerIpcEndpointRegistrationOnWindowsSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsStartCacheTest;
import org.apache.ignite.internal.processors.igfs.IgfsStreamsSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsTaskSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsLocalSecondaryFileSystemDualAsyncClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsLocalSecondaryFileSystemDualAsyncSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsLocalSecondaryFileSystemDualSyncClientSelfTest;
import org.apache.ignite.internal.processors.igfs.IgfsLocalSecondaryFileSystemDualSyncSelfTest;
import org.apache.ignite.internal.processors.igfs.split.IgfsByteDelimiterRecordResolverSelfTest;
import org.apache.ignite.internal.processors.igfs.split.IgfsFixedLengthRecordResolverSelfTest;
import org.apache.ignite.internal.processors.igfs.split.IgfsNewLineDelimiterRecordResolverSelfTest;
import org.apache.ignite.internal.processors.igfs.split.IgfsStringDelimiterRecordResolverSelfTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for Hadoop file system over Ignite cache.
 * Contains platform independent tests only.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    IgfsPrimarySelfTest.class,
    IgfsPrimaryMultiNodeSelfTest.class,

    IgfsPrimaryRelaxedConsistencySelfTest.class,
    IgfsPrimaryRelaxedConsistencyMultiNodeSelfTest.class,

    IgfsDualSyncSelfTest.class,
    IgfsDualAsyncSelfTest.class,

    IgfsLocalSecondaryFileSystemDualSyncSelfTest.class,
    IgfsLocalSecondaryFileSystemDualAsyncSelfTest.class,
    IgfsLocalSecondaryFileSystemDualSyncClientSelfTest.class,
    IgfsLocalSecondaryFileSystemDualAsyncClientSelfTest.class,

    //IgfsSizeSelfTest.class,
    IgfsAttributesSelfTest.class,
    IgfsFileInfoSelfTest.class,
    IgfsMetaManagerSelfTest.class,
    IgfsDataManagerSelfTest.class,
    IgfsProcessorSelfTest.class,
    IgfsProcessorValidationSelfTest.class,
    IgfsCacheSelfTest.class,

    IgfsServerManagerIpcEndpointRegistrationOnWindowsSelfTest.class,

    IgfsCachePerBlockLruEvictionPolicySelfTest.class,

    IgfsStreamsSelfTest.class,
    IgfsModesSelfTest.class,
    IgfsMetricsSelfTest.class,

    IgfsPrimaryClientSelfTest.class,
    IgfsPrimaryRelaxedConsistencyClientSelfTest.class,
    IgfsDualSyncClientSelfTest.class,
    IgfsDualAsyncClientSelfTest.class,

    IgfsOneClientNodeTest.class,

    IgfsModeResolverSelfTest.class,

    //IgfsPathSelfTest.class,
    IgfsFragmentizerSelfTest.class,
    IgfsFragmentizerTopologySelfTest.class,
    IgfsFileMapSelfTest.class,

    IgfsByteDelimiterRecordResolverSelfTest.class,
    IgfsStringDelimiterRecordResolverSelfTest.class,
    IgfsFixedLengthRecordResolverSelfTest.class,
    IgfsNewLineDelimiterRecordResolverSelfTest.class,

    IgfsTaskSelfTest.class,

    IgfsGroupDataBlockKeyMapperHashSelfTest.class,

    IgfsStartCacheTest.class,

    IgfsBackupsPrimarySelfTest.class,
    IgfsBackupsDualSyncSelfTest.class,
    IgfsBackupsDualAsyncSelfTest.class,

    IgfsBlockMessageSystemPoolStarvationSelfTest.class,

    // TODO: Enable when IGFS failover is fixed.
    //IgfsBackupFailoverSelfTest.class,

    IgfsProxySelfTest.class,
    IgfsLocalSecondaryFileSystemProxySelfTest.class,
    IgfsLocalSecondaryFileSystemProxyClientSelfTest.class,

    IgfsAtomicPrimarySelfTest.class,
    IgfsAtomicPrimaryMultiNodeSelfTest.class,

    IgfsSecondaryFileSystemInjectionSelfTest.class,
})
public class IgniteIgfsTestSuite {
}
