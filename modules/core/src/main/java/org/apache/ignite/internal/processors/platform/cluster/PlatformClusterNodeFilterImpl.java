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

package org.apache.ignite.internal.processors.platform.cluster;

import org.apache.ignite.Ignite;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.binary.BinaryRawWriterEx;
import org.apache.ignite.internal.processors.platform.PlatformAbstractPredicate;
import org.apache.ignite.internal.processors.platform.PlatformContext;
import org.apache.ignite.internal.processors.platform.memory.PlatformMemory;
import org.apache.ignite.internal.processors.platform.memory.PlatformOutputStream;
import org.apache.ignite.internal.processors.platform.utils.PlatformUtils;
import org.apache.ignite.resources.IgniteInstanceResource;

/**
 * Interop cluster node filter.
 */
public class PlatformClusterNodeFilterImpl extends PlatformAbstractPredicate implements PlatformClusterNodeFilter, Binarylizable {
    /** */
    private static final long serialVersionUID = 0L;

    private int handleId = -1;

    /**
     * {@link java.io.Externalizable} support.
     */
    public PlatformClusterNodeFilterImpl() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param pred .Net binary predicate.
     * @param ctx Kernal context.
     */
    public PlatformClusterNodeFilterImpl(Object pred, PlatformContext ctx) {
        super(pred, 0, ctx);

        init();
    }

    /* Initializes the cluster node filter */
    public void init(){
        try (PlatformMemory mem = ctx.memory().allocate()) {
            PlatformOutputStream out = mem.output();

            BinaryRawWriterEx writer = ctx.writer(out);

            writer.writeObject(pred);
            out.synchronize();

            handleId = ctx.gateway().clusterNodeFilterCreate(mem.pointer());
        }
    }

    /** {@inheritDoc} */
    @Override public boolean apply(ClusterNode clusterNode)  {

        if(ctx == null){
            return false;
        }

        if(handleId < 0){
            init();
        }

        try (PlatformMemory mem = ctx.memory().allocate()) {
            PlatformOutputStream out = mem.output();

            BinaryRawWriterEx writer = ctx.writer(out);

            writer.writeLong(handleId);
            ctx.writeNode(writer, clusterNode);

            out.synchronize();

            int res = ctx.gateway().clusterNodeFilterApply(mem.pointer());

            // Should handle .NET exceptions
            return res == 1;
        }
    }

    /**
     * @param ignite Ignite instance.
     */
    @IgniteInstanceResource
    public void setIgniteInstance(Ignite ignite) {
        ctx = PlatformUtils.platformContext(ignite);
    }

    /**
     * @return Filter itself
     */
    public Object getInternalPredicate() {
        return pred;
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
        writer.writeObject("pred", pred);
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
        pred = reader.readObject("pred");
    }
}
