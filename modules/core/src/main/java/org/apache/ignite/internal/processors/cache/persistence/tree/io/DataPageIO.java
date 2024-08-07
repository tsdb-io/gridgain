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

package org.apache.ignite.internal.processors.cache.persistence.tree.io;

import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.PageUtils;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.mvcc.MvccUtils;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRow;
import org.apache.ignite.internal.processors.cache.tree.mvcc.data.MvccUpdateResult;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.GridStringBuilder;

import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_CRD_COUNTER_NA;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_HINTS_BIT_OFF;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_HINTS_MASK;
import static org.apache.ignite.internal.processors.cache.mvcc.MvccUtils.MVCC_KEY_ABSENT_BEFORE_OFF;
import static org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO.EntryPart.CACHE_ID;
import static org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO.EntryPart.EXPIRE_TIME;
import static org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO.EntryPart.KEY;
import static org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO.EntryPart.MVCC_INFO;
import static org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO.EntryPart.VALUE;
import static org.apache.ignite.internal.processors.cache.persistence.tree.io.DataPageIO.EntryPart.VERSION;

/**
 * Data pages IO.
 */
public class DataPageIO extends AbstractDataPageIO<CacheDataRow> {
    /** */
    public static final int MVCC_INFO_SIZE = 40;

    /** */
    public static final IOVersions<DataPageIO> VERSIONS = new IOVersions<>(
        new DataPageIO(1)
    );

    /**
     * @param ver Page format version.
     */
    protected DataPageIO(int ver) {
        super(T_DATA, ver);
    }

    /** {@inheritDoc} */
    @Override protected void writeRowData(
        long pageAddr,
        int dataOff,
        int payloadSize,
        CacheDataRow row,
        boolean newRow
    ) throws IgniteCheckedException {
        assertPageType(pageAddr);

        long addr = pageAddr + dataOff;

        int cacheIdSize = row.cacheId() != 0 ? 4 : 0;
        int mvccInfoSize = row.mvccCoordinatorVersion() > 0 ? MVCC_INFO_SIZE : 0;

        if (newRow) {
            PageUtils.putShort(addr, 0, (short)payloadSize);
            addr += 2;

            if (mvccInfoSize > 0) {
                assert MvccUtils.mvccVersionIsValid(row.mvccCoordinatorVersion(), row.mvccCounter(), row.mvccOperationCounter());

                final int keyAbsentBeforeFlag = (row instanceof MvccUpdateResult) &&
                    ((MvccUpdateResult)row).isKeyAbsentBefore() ? (1 << MVCC_KEY_ABSENT_BEFORE_OFF) : 0;

                // xid_min.
                PageUtils.putLong(addr, 0, row.mvccCoordinatorVersion());
                PageUtils.putLong(addr, 8, row.mvccCounter());
                PageUtils.putInt(addr, 16, row.mvccOperationCounter() | (row.mvccTxState() << MVCC_HINTS_BIT_OFF) |
                    ((row.newMvccCoordinatorVersion() == MVCC_CRD_COUNTER_NA) ? keyAbsentBeforeFlag : 0));

                assert row.newMvccCoordinatorVersion() == MVCC_CRD_COUNTER_NA
                    || MvccUtils.mvccVersionIsValid(row.newMvccCoordinatorVersion(), row.newMvccCounter(), row.newMvccOperationCounter());

                // xid_max.
                PageUtils.putLong(addr, 20, row.newMvccCoordinatorVersion());
                PageUtils.putLong(addr, 28, row.newMvccCounter());
                PageUtils.putInt(addr, 36, row.newMvccOperationCounter() | (row.newMvccTxState() << MVCC_HINTS_BIT_OFF) |
                    ((row.newMvccCoordinatorVersion() == MVCC_CRD_COUNTER_NA) ? 0 : keyAbsentBeforeFlag));

                addr += mvccInfoSize;
            }

            if (cacheIdSize != 0) {
                PageUtils.putInt(addr, 0, row.cacheId());

                addr += cacheIdSize;
            }

            addr += row.key().putValue(addr);
        }
        else
            addr += (2 + mvccInfoSize + cacheIdSize + row.key().valueBytesLength(null));

        addr += row.value().putValue(addr);

        CacheVersionIO.write(addr, row.version(), false);
        addr += CacheVersionIO.size(row.version(), false);

        PageUtils.putLong(addr, 0, row.expireTime());
    }

    /** {@inheritDoc} */
    @Override protected void writeFragmentData(
        CacheDataRow row,
        ByteBuffer buf,
        int rowOff,
        int payloadSize
    ) throws IgniteCheckedException {
        assertPageType(buf);

        final int keySize = row.key().valueBytesLength(null);

        final int valSize = row.value().valueBytesLength(null);

        int written = writeFragment(row, buf, rowOff, payloadSize,
            MVCC_INFO, keySize, valSize);
        written += writeFragment(row, buf, rowOff + written, payloadSize - written,
            CACHE_ID, keySize, valSize);
        written += writeFragment(row, buf, rowOff + written, payloadSize - written,
            KEY, keySize, valSize);
        written += writeFragment(row, buf, rowOff + written, payloadSize - written,
            EXPIRE_TIME, keySize, valSize);
        written += writeFragment(row, buf, rowOff + written, payloadSize - written,
            VALUE, keySize, valSize);
        written += writeFragment(row, buf, rowOff + written, payloadSize - written,
            VERSION, keySize, valSize);

        assert written == payloadSize;
    }

    /**
     * Updates the expiration time for existing row that fits one data page.
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset inside the page that contains the row.
     * @param row Row.
     * @throws IgniteCheckedException If failed.
     */
    public void updateExpirationTime(
        long pageAddr,
        int dataOff,
        CacheDataRow row
    ) throws IgniteCheckedException {
        assertPageType(pageAddr);

        long addr = pageAddr + dataOff;

        int cacheIdSize = row.cacheId() != 0 ? 4 : 0;
        int mvccInfoSize = row.mvccCoordinatorVersion() > 0 ? MVCC_INFO_SIZE : 0;

        addr += (mvccInfoSize + cacheIdSize + row.key().valueBytesLength(null));

        addr += row.value().valueBytesLength(null);

        addr += CacheVersionIO.size(row.version(), false);

        PageUtils.putLong(addr, 0, row.expireTime());
    }

    /**
     * Updates the expiration time for existing row that does not fit one data page.
     *
     * @param row Cache data row with a new expiration time.
     * @param buf Buffer that contains the row.
     * @param payloadSize Available payload size in the buffer.
     * @param updatedBytes Number of bytes that were updated already.
     * @param scannedBytes Number of bytes that were scanned already.
     * @return Number of bytes related to the expiration time field that were updated.
     *      When the field was completely updated then Integer.MAX_VALUE is returned.
     *
     * @throws IgniteCheckedException If failed.
     */
    public int updateExpirationTimeFragmentData(
        CacheDataRow row,
        ByteBuffer buf,
        int payloadSize,
        int updatedBytes,
        int scannedBytes
    ) throws IgniteCheckedException {
        assertPageType(buf);

        final int keySize = row.key().valueBytesLength(null);

        final int valSize = row.value().valueBytesLength(null);

        final int startPos = calculateStartPosition(row, EXPIRE_TIME, keySize, valSize);
        final int finishPos = calculateFinishPosition(row, EXPIRE_TIME, keySize, valSize);

        // Need to find a position of expiration time.
        if (scannedBytes + payloadSize <= startPos) {
            // Don't have anything to update. Need scan the next fragment of the entry.
            return 0;
        }

        // Payload contains bytes that should be updated. Move buffer position to the start of payload.
        int expirationTimeOffset = ((startPos > scannedBytes) ? startPos - scannedBytes : 0);

        buf.position(buf.position() + expirationTimeOffset);

        // Minimum available size of bytes to update.
        int len = Math.min(finishPos - startPos - updatedBytes, payloadSize - expirationTimeOffset);

        assert len > 0 : "There are no bytes to update " +
            "[startPos=" + startPos + ", finishPos=" + finishPos + ", scannedBytes=" + scannedBytes + ", upatedBytes=" + updatedBytes +
            ", payloadSize=" + payloadSize + ", expirationTimeOffset=" + expirationTimeOffset + ", row=" + row + ']';

        writeExpireTimeFragment(buf, row.expireTime(), updatedBytes, len, 0);

        if (updatedBytes + len == finishPos - startPos)
            return Integer.MAX_VALUE;

        // Return number of bytes that were updated.
        return len;
    }

    /**
     * Updates fragmented data row with the given payload.
     *
     * @param pageAddr Page address.
     * @param itemId Item ID.
     * @param pageSize Page size.
     * @param linkToNextFragment Link to the next fragment.
     * @param payload Payload.
     */
    public void updateFragmentedData(
        long pageAddr,
        int itemId,
        int pageSize,
        long linkToNextFragment,
        byte[] payload
    ) {
        assertPageType(pageAddr);
        assert checkIndex(itemId) : "Invalid item Id [itemId=" + itemId + ']';
        assert payload != null : "Payload is null";

        int dataOff = getDataOffset(pageAddr, itemId, pageSize);

        PageUtils.putShort(pageAddr, dataOff, (short)(payload.length | FRAGMENTED_FLAG));
        dataOff += PAYLOAD_LEN_SIZE;

        PageUtils.putLong(pageAddr, dataOff, linkToNextFragment);
        dataOff += LINK_SIZE;

        PageUtils.putBytes(pageAddr, dataOff, payload);
    }

    /**
     * Calculates start position of the fragment defined by {@code type}.
     *
     * @param row Row.
     * @param type Type of the part of entry.
     * @param keySize Key size.
     * @param valSize Value size.
     * @return Start position.
     */
    private int calculateStartPosition(final CacheDataRow row, final EntryPart type, final int keySize, final int valSize) {
        final int prevLen;

        int cacheIdSize = row.cacheId() == 0 ? 0 : 4;
        int mvccInfoSize = row.mvccCoordinatorVersion() > 0 ? MVCC_INFO_SIZE : 0;

        switch (type) {
            case MVCC_INFO:
                prevLen = 0;

                break;

            case CACHE_ID:
                prevLen = mvccInfoSize;

                break;

            case KEY:
                prevLen = mvccInfoSize + cacheIdSize;

                break;

            case EXPIRE_TIME:
                prevLen = mvccInfoSize + cacheIdSize + keySize;

                break;

            case VALUE:
                prevLen = mvccInfoSize + cacheIdSize + keySize + 8;

                break;

            case VERSION:
                prevLen = mvccInfoSize + cacheIdSize + keySize + valSize + 8;

                break;

            default:
                throw new IllegalArgumentException("Unknown entry part type: " + type);
        }

        return prevLen;
    }

    /**
     * Calculates finish position of the fragment defined by {@code type}.
     *
     * @param row Row.
     * @param type Type of the part of entry.
     * @param keySize Key size.
     * @param valSize Value size.
     * @return Finish position.
     */
    private int calculateFinishPosition(final CacheDataRow row, final EntryPart type, final int keySize, final int valSize) {
        final int curLen;

        int cacheIdSize = row.cacheId() == 0 ? 0 : 4;
        int mvccInfoSize = row.mvccCoordinatorVersion() > 0 ? MVCC_INFO_SIZE : 0;

        switch (type) {
            case MVCC_INFO:
                curLen = mvccInfoSize;

                break;

            case CACHE_ID:
                curLen = mvccInfoSize + cacheIdSize;

                break;

            case KEY:
                curLen = mvccInfoSize + cacheIdSize + keySize;

                break;

            case EXPIRE_TIME:
                curLen = mvccInfoSize + cacheIdSize + keySize + 8;

                break;

            case VALUE:
                curLen = mvccInfoSize + cacheIdSize + keySize + valSize + 8;

                break;

            case VERSION:
                curLen = mvccInfoSize + cacheIdSize + keySize + valSize + CacheVersionIO.size(row.version(), false) + 8;

                break;

            default:
                throw new IllegalArgumentException("Unknown entry part type: " + type);
        }

        return curLen;
    }

    /**
     * Try to write fragment data.
     *
     * @param row Row.
     * @param buf Byte buffer.
     * @param rowOff Offset in row data bytes.
     * @param payloadSize Data length that should be written in this fragment.
     * @param type Type of the part of entry.
     * @param keySize Key size.
     * @param valSize Value size.
     * @return Actually written data.
     * @throws IgniteCheckedException If fail.
     */
    private int writeFragment(
        final CacheDataRow row,
        final ByteBuffer buf,
        final int rowOff,
        final int payloadSize,
        final EntryPart type,
        final int keySize,
        final int valSize
    ) throws IgniteCheckedException {
        if (payloadSize == 0)
            return 0;

        final int prevLen = calculateStartPosition(row, type, keySize, valSize);
        final int curLen = calculateFinishPosition(row, type, keySize, valSize);

        if (curLen <= rowOff)
            return 0;

        final int len = Math.min(curLen - rowOff, payloadSize);

        if (type == EXPIRE_TIME)
            writeExpireTimeFragment(buf, row.expireTime(), rowOff, len, prevLen);
        else if (type == CACHE_ID)
            writeCacheIdFragment(buf, row.cacheId(), rowOff, len, prevLen);
        else if (type == MVCC_INFO) {
            final int keyAbsentBeforeFlag = (row instanceof MvccUpdateResult) &&
                ((MvccUpdateResult)row).isKeyAbsentBefore() ? (1 << MVCC_KEY_ABSENT_BEFORE_OFF) : 0;

            writeMvccInfoFragment(buf,
                row.mvccCoordinatorVersion(),
                row.mvccCounter(),
                row.mvccOperationCounter() | (row.mvccTxState() << MVCC_HINTS_BIT_OFF) |
                    ((row.newMvccCoordinatorVersion() == MVCC_CRD_COUNTER_NA) ? keyAbsentBeforeFlag : 0),
                row.newMvccCoordinatorVersion(),
                row.newMvccCounter(),
                row.newMvccOperationCounter() | (row.newMvccTxState() << MVCC_HINTS_BIT_OFF) |
                    ((row.newMvccCoordinatorVersion() == MVCC_CRD_COUNTER_NA) ? 0 : keyAbsentBeforeFlag),
                len);
        }
        else if (type != VERSION) {
            // Write key or value.
            final CacheObject co = type == KEY ? row.key() : row.value();

            co.putValue(buf, rowOff - prevLen, len);
        }
        else
            writeVersionFragment(buf, row.version(), rowOff, len, prevLen);

        return len;
    }

    /**
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @param mvccCrd Mvcc coordinator.
     * @param mvccCntr Mvcc counter.
     * @param mvccOpCntr Operation counter.
     * @param txState Tx state hint.
     */
    public void updateNewVersion(long pageAddr, int dataOff, long mvccCrd, long mvccCntr, int mvccOpCntr, byte txState) {
        assertPageType(pageAddr);

        long addr = pageAddr + dataOff;

        updateNewVersion(addr, mvccCrd, mvccCntr,
            (mvccOpCntr & ~MVCC_HINTS_MASK) | ((int)txState << MVCC_HINTS_BIT_OFF));
    }

    /**
     * @param pageAddr Page address.
     * @param itemId Item ID.
     * @param pageSize Page size.
     * @param mvccCrd Mvcc coordinator.
     * @param mvccCntr Mvcc counter.
     * @param mvccOpCntr Operation counter.
     */
    public void updateNewVersion(long pageAddr, int itemId, int pageSize, long mvccCrd, long mvccCntr, int mvccOpCntr) {
        assertPageType(pageAddr);

        int dataOff = getDataOffset(pageAddr, itemId, pageSize);

        long addr = pageAddr + dataOff + (isFragmented(pageAddr, dataOff) ? 10 : 2);

        updateNewVersion(addr, mvccCrd, mvccCntr, mvccOpCntr);
    }

    /**
     * @param pageAddr Page address.
     * @param itemId Item ID.
     * @param pageSize Page size.
     * @param txState Tx state hint.
     */
    public void updateTxState(long pageAddr, int itemId, int pageSize, byte txState) {
        assertPageType(pageAddr);

        int dataOff = getDataOffset(pageAddr, itemId, pageSize);

        long addr = pageAddr + dataOff + (isFragmented(pageAddr, dataOff) ? 10 : 2);

        int opCntr = rawMvccOperationCounter(addr, 0);

        rawMvccOperationCounter(addr, 0, (opCntr & ~MVCC_HINTS_MASK) | ((int)txState << MVCC_HINTS_BIT_OFF));
    }

    /**
     * @param pageAddr Page address.
     * @param itemId Item ID.
     * @param pageSize Page size.
     * @param txState Tx state hint.
     */
    public void updateNewTxState(long pageAddr, int itemId, int pageSize, byte txState) {
        assertPageType(pageAddr);

        int dataOff = getDataOffset(pageAddr, itemId, pageSize);

        long addr = pageAddr + dataOff + (isFragmented(pageAddr, dataOff) ? 10 : 2);

        int opCntr = rawNewMvccOperationCounter(addr, 0);

        rawNewMvccOperationCounter(addr, 0, (opCntr & ~MVCC_HINTS_MASK) | ((int)txState << MVCC_HINTS_BIT_OFF));
    }

    /**
     * Marks row removed.
     *
     * @param addr Address.
     * @param mvccCrd Mvcc coordinator.
     * @param mvccCntr Mvcc counter.
     */
    private void updateNewVersion(long addr, long mvccCrd, long mvccCntr, int mvccOpCntr) {
        // Skip xid_min.
        addr += 20;

        PageUtils.putLong(addr, 0, mvccCrd);
        PageUtils.putLong(addr, 8, mvccCntr);
        PageUtils.putInt(addr, 16, mvccOpCntr);
    }

    /**
     * Returns MVCC coordinator number.
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @return MVCC coordinator number.
     */
    public long mvccCoordinator(long pageAddr, int dataOff) {
        long addr = pageAddr + dataOff;

        return PageUtils.getLong(addr, 0);
    }

    /**
     * Returns MVCC counter value.
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @return MVCC counter value.
     */
    public long mvccCounter(long pageAddr, int dataOff) {
        long addr = pageAddr + dataOff;

        return PageUtils.getLong(addr, 8);
    }

    /**
     * Returns MVCC operation counter raw value (with hints and flags).
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @return MVCC counter value.
     */
    public int rawMvccOperationCounter(long pageAddr, int dataOff) {
        long addr = pageAddr + dataOff;

        return PageUtils.getInt(addr, 16);
    }

    /**
     * Sets MVCC operation counter raw value (with hints and flags).
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @param opCntr MVCC counter value.
     */
    public void rawMvccOperationCounter(long pageAddr, int dataOff, int opCntr) {
        assertPageType(pageAddr);

        long addr = pageAddr + dataOff;

        PageUtils.putInt(addr, 16, opCntr);
    }

    /**
     * Returns new MVCC coordinator number.
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @return New MVCC coordinator number.
     */
    public long newMvccCoordinator(long pageAddr, int dataOff) {
        long addr = pageAddr + dataOff;

        // Skip xid_min.
        addr += 20;

        return PageUtils.getLong(addr, 0);
    }

    /**
     * Returns new MVCC counter value.
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @return New MVCC counter value.
     */
    public long newMvccCounter(long pageAddr, int dataOff) {
        long addr = pageAddr + dataOff;

        // Skip xid_min.
        addr += 20;

        return PageUtils.getLong(addr, 8);
    }

    /**
     * Returns MVCC operation counter raw value (with hints and flags).
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @return MVCC counter value.
     */
    public int rawNewMvccOperationCounter(long pageAddr, int dataOff) {
        long addr = pageAddr + dataOff;

        // Skip xid_min.
        addr += 20;

        return PageUtils.getInt(addr, 16);
    }

    /**
     * Sets MVCC new operation counter raw value (with hints and flags).
     *
     * @param pageAddr Page address.
     * @param dataOff Data offset.
     * @param opCntr MVCC operation counter value.
     */
    public void rawNewMvccOperationCounter(long pageAddr, int dataOff, int opCntr) {
        assertPageType(pageAddr);

        long addr = pageAddr + dataOff;

        // Skip xid_min.
        addr += 20;

        PageUtils.putInt(addr, 16, opCntr);
    }

    /**
     * @param buf Byte buffer.
     * @param ver Version.
     * @param rowOff Row offset.
     * @param len Length.
     * @param prevLen previous length.
     */
    private void writeVersionFragment(ByteBuffer buf, GridCacheVersion ver, int rowOff, int len, int prevLen) {
        int verSize = CacheVersionIO.size(ver, false);

        assert len <= verSize : len;

        if (verSize == len) { // Here we check for equality but not <= because version is the last.
            // Here we can write version directly.
            CacheVersionIO.write(buf, ver, false);
        }
        else {
            // We are in the middle of cache version.
            ByteBuffer verBuf = ByteBuffer.allocate(verSize);

            verBuf.order(buf.order());

            CacheVersionIO.write(verBuf, ver, false);

            buf.put(verBuf.array(), rowOff - prevLen, len);
        }
    }

    /**
     * @param buf Byte buffer.
     * @param expireTime Expire time.
     * @param rowOff Row offset.
     * @param len Length.
     * @param prevLen previous length.
     */
    private void writeExpireTimeFragment(ByteBuffer buf, long expireTime, int rowOff, int len, int prevLen) {
        int size = 8;
        if (size <= len)
            buf.putLong(expireTime);
        else {
            ByteBuffer timeBuf = ByteBuffer.allocate(size);

            timeBuf.order(buf.order());

            timeBuf.putLong(expireTime);

            buf.put(timeBuf.array(), rowOff - prevLen, len);
        }
    }

    /**
     * @param buf Buffer.
     * @param cacheId Cache ID.
     * @param rowOff Row offset.
     * @param len Length.
     * @param prevLen Prev length.
     */
    private void writeCacheIdFragment(ByteBuffer buf, int cacheId, int rowOff, int len, int prevLen) {
        if (cacheId == 0)
            return;

        int size = 4;

        if (size <= len)
            buf.putInt(cacheId);
        else {
            ByteBuffer cacheIdBuf = ByteBuffer.allocate(size);

            cacheIdBuf.order(buf.order());

            cacheIdBuf.putInt(cacheId);

            buf.put(cacheIdBuf.array(), rowOff - prevLen, len);
        }
    }

    /**
     * @param buf Byte buffer.
     * @param mvccCrd Coordinator version.
     * @param mvccCntr Counter.
     * @param mvccOpCntr Operation counter.
     * @param newMvccCrd New coordinator version.
     * @param newMvccCntr New counter version.
     * @param newMvccOpCntr New operation counter.
     * @param len Length.
     */
    private void writeMvccInfoFragment(ByteBuffer buf, long mvccCrd, long mvccCntr, int mvccOpCntr, long newMvccCrd,
        long newMvccCntr, int newMvccOpCntr, int len) {
        if (mvccCrd == 0)
            return;

        assert len >= MVCC_INFO_SIZE : "Mvcc info should fit on the one page!";

        assert MvccUtils.mvccVersionIsValid(mvccCrd, mvccCntr, mvccOpCntr);

        // xid_min.
        buf.putLong(mvccCrd);
        buf.putLong(mvccCntr);
        buf.putInt(mvccOpCntr);

        assert newMvccCrd == 0 || MvccUtils.mvccVersionIsValid(newMvccCrd, newMvccCntr, newMvccOpCntr);

        // xid_max.
        buf.putLong(newMvccCrd);
        buf.putLong(newMvccCntr);
        buf.putInt(newMvccOpCntr);
    }

    /** {@inheritDoc} */
    @Override protected void printPage(long addr, int pageSize, GridStringBuilder sb) throws IgniteCheckedException {
        sb.a("DataPageIO [\n");
        printPageLayout(addr, pageSize, sb);
        sb.a("\n]");
    }

    /**
     *
     */
    enum EntryPart {
        /** */
        KEY,

        /** */
        VALUE,

        /** */
        VERSION,

        /** */
        EXPIRE_TIME,

        /** */
        CACHE_ID,

        /** */
        MVCC_INFO
    }
}
