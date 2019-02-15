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

package org.apache.ignite.internal.processors.query.h2.database;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.pagemem.PageIdUtils;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.mvcc.MvccSnapshot;
import org.apache.ignite.internal.processors.cache.persistence.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO;
import org.apache.ignite.internal.processors.cache.tree.mvcc.search.MvccDataPageClosure;
import org.apache.ignite.internal.processors.query.h2.database.io.H2RowLinkIO;
import org.apache.ignite.internal.processors.query.h2.opt.H2Row;
import org.apache.ignite.internal.transactions.IgniteTxUnexpectedStateCheckedException;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.spi.indexing.IndexingQueryCacheFilter;

import static org.apache.ignite.internal.pagemem.PageIdUtils.pageId;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.isVisible;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.mvccVersionIsValid;

/**
 *
 */
public class H2TreeFilterClosure implements H2Tree.TreeRowClosure<H2Row, H2Row>, MvccDataPageClosure {
    /** */
    private final MvccSnapshot mvccSnapshot;

    /** */
    private final IndexingQueryCacheFilter filter;

    /** */
    private final GridCacheContext cctx;

    /** */
    private final IgniteLogger log;

    /**
     * @param filter Cache filter.
     * @param mvccSnapshot MVCC snapshot.
     * @param cctx Cache context.
     */
    public H2TreeFilterClosure(IndexingQueryCacheFilter filter, MvccSnapshot mvccSnapshot, GridCacheContext cctx,
        IgniteLogger log) {
        assert (filter != null || mvccSnapshot != null) && cctx != null ;

        this.filter = filter;
        this.mvccSnapshot = mvccSnapshot;
        this.cctx = cctx;
        this.log = log;
    }

    /** {@inheritDoc} */
    @Override public boolean apply(BPlusTree<H2Row, H2Row> tree, BPlusIO<H2Row> io,
        long pageAddr, int idx)  throws IgniteCheckedException {
        return (filter  == null || applyFilter((H2RowLinkIO)io, pageAddr, idx))
            && (mvccSnapshot == null || applyMvcc((H2RowLinkIO)io, pageAddr, idx));
    }

    /**
     * @param io Row IO.
     * @param pageAddr Page address.
     * @param idx Item index.
     * @return {@code True} if row passes the filter.
     */
    private boolean applyFilter(H2RowLinkIO io, long pageAddr, int idx) {
        assert filter != null;

        return filter.applyPartition(PageIdUtils.partId(pageId(io.getLink(pageAddr, idx))));
    }

    /**
     * @param io Row IO.
     * @param pageAddr Page address.
     * @param idx Item index.
     * @return {@code True} if row passes the filter.
     */
    private boolean applyMvcc(H2RowLinkIO io, long pageAddr, int idx) throws IgniteCheckedException {
        assert io.storeMvccInfo() : io;

        long rowCrdVer = io.getMvccCoordinatorVersion(pageAddr, idx);
        long rowCntr = io.getMvccCounter(pageAddr, idx);
        int rowOpCntr = io.getMvccOperationCounter(pageAddr, idx);

        assert mvccVersionIsValid(rowCrdVer, rowCntr, rowOpCntr);

        try {
            return isVisible(cctx, mvccSnapshot, rowCrdVer, rowCntr, rowOpCntr, io.getLink(pageAddr, idx));
        }
        catch (IgniteTxUnexpectedStateCheckedException e) {
            // TODO this catch must not be needed if we switch Vacuum to data page scan
            // We expect the active tx state can be observed by read tx only in the cases when tx has been aborted
            // asynchronously and node hasn't received finish message yet but coordinator has already removed it from
            // the active txs map. Rows written by this tx are invisible to anyone and will be removed by the vacuum.
            if (log.isDebugEnabled())
                log.debug( "Unexpected tx state on index lookup. " + X.getFullStackTrace(e));

            return false;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean applyMvcc(DataPageIO io, long dataPageAddr, int itemId, int pageSize) throws IgniteCheckedException {
        try {
            return isVisible(cctx, mvccSnapshot, io, dataPageAddr, itemId, pageSize);
        }
        catch (IgniteTxUnexpectedStateCheckedException e) {
            // TODO this catch must not be needed if we switch Vacuum to data page scan
            // We expect the active tx state can be observed by read tx only in the cases when tx has been aborted
            // asynchronously and node hasn't received finish message yet but coordinator has already removed it from
            // the active txs map. Rows written by this tx are invisible to anyone and will be removed by the vacuum.
            if (log.isDebugEnabled())
                log.debug( "Unexpected tx state on index lookup. " + X.getFullStackTrace(e));

            return false;
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(H2TreeFilterClosure.class, this);
    }
}
