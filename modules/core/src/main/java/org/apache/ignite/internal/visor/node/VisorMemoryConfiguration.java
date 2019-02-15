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

package org.apache.ignite.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for memory configuration.
 */
public class VisorMemoryConfiguration extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Size of a memory chunk reserved for system cache initially. */
    private long sysCacheInitSize;

    /** Size of memory for system cache. */
    private long sysCacheMaxSize;

    /** Page size. */
    private int pageSize;

    /** Concurrency level. */
    private int concLvl;

    /** Name of DataRegion to be used as default. */
    private String dfltMemPlcName;

    /** Size of memory (in bytes) to use for default DataRegion. */
    private long dfltMemPlcSize;

    /** Memory policies. */
    private List<VisorMemoryPolicyConfiguration> memPlcs;

    /**
     * Default constructor.
     */
    public VisorMemoryConfiguration() {
        // No-op.
    }

    /**
     * Create data transfer object.
     *
     * @param memCfg Memory configuration.
     */
    public VisorMemoryConfiguration(DataStorageConfiguration memCfg) {
        assert memCfg != null;

        sysCacheInitSize = memCfg.getSystemRegionInitialSize();
        sysCacheMaxSize = memCfg.getSystemRegionMaxSize();
        pageSize = memCfg.getPageSize();
        concLvl = memCfg.getConcurrencyLevel();
//        dfltMemPlcName = memCfg.getDefaultDataRegionName();
        //dfltMemPlcSize = memCfg.getDefaultDataRegionSize();

        DataRegionConfiguration[] plcs = memCfg.getDataRegionConfigurations();

        if (!F.isEmpty(plcs)) {
            memPlcs = new ArrayList<>(plcs.length);

            for (DataRegionConfiguration plc : plcs)
                memPlcs.add(new VisorMemoryPolicyConfiguration(plc));
        }
    }

    /**
     * @return Concurrency level.
     */
    public int getConcurrencyLevel() {
        return concLvl;
    }

    /**
     * @return Initial size of a memory region reserved for system cache.
     */
    public long getSystemCacheInitialSize() {
        return sysCacheInitSize;
    }

    /**
     * @return Maximum memory region size reserved for system cache.
     */
    public long getSystemCacheMaxSize() {
        return sysCacheMaxSize;
    }

    /**
     * @return Page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @return Name of DataRegion to be used as default.
     */
    public String getDefaultMemoryPolicyName() {
        return dfltMemPlcName;
    }

    /**
     * @return Default memory policy size.
     */
    public long getDefaultMemoryPolicySize() {
        return dfltMemPlcSize;
    }

    /**
     * @return Collection of DataRegionConfiguration objects.
     */
    public List<VisorMemoryPolicyConfiguration> getMemoryPolicies() {
        return memPlcs;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeLong(sysCacheInitSize);
        out.writeLong(sysCacheMaxSize);
        out.writeInt(pageSize);
        out.writeInt(concLvl);
        U.writeString(out, dfltMemPlcName);
        out.writeLong(dfltMemPlcSize);
        U.writeCollection(out, memPlcs);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        sysCacheInitSize = in.readLong();
        sysCacheMaxSize = in.readLong();
        pageSize = in.readInt();
        concLvl = in.readInt();
        dfltMemPlcName = U.readString(in);
        dfltMemPlcSize = in.readLong();
        memPlcs = U.readList(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorMemoryConfiguration.class, this);
    }
}
