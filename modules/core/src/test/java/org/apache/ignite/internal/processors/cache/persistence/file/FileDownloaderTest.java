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

package org.apache.ignite.internal.processors.cache.persistence.file;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.testframework.GridTestUtils.runAsync;
import static org.junit.Assert.*;

/**
 * FileDownloader test
 */
public class FileDownloaderTest extends GridCommonAbstractTest {
    /** */
    private static final Path DOWNLOADER_PATH = new File("download").toPath();

    /** */
    private static final Path UPLOADER_PATH = new File("upload").toPath();

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        if (DOWNLOADER_PATH.toFile().exists())
            DOWNLOADER_PATH.toFile().delete();

        if (UPLOADER_PATH.toFile().exists())
            UPLOADER_PATH.toFile().delete();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        if (DOWNLOADER_PATH.toFile().exists())
            DOWNLOADER_PATH.toFile().delete();

        if (UPLOADER_PATH.toFile().exists())
            UPLOADER_PATH.toFile().delete();
    }

    /***
     *
     * @throws Exception If failed.
     */
    @Test
    public void test() throws Exception {
        assertTrue(UPLOADER_PATH.toFile().createNewFile());
        assertTrue(!DOWNLOADER_PATH.toFile().exists());

        PrintWriter writer = new PrintWriter(UPLOADER_PATH.toFile());

        for (int i = 0; i < 1_000_000; i++)
            writer.write("HELLO WORLD");

        writer.close();

        FileDownloader downloader = new FileDownloader(log, DOWNLOADER_PATH);

        InetSocketAddress address = downloader.start();

        GridFutureAdapter<Long> finishFut = new GridFutureAdapter<>();

        FileUploader uploader = new FileUploader(UPLOADER_PATH);

        SocketChannel sc = null;

        try {
            sc = SocketChannel.open(address);
        }
        catch (IOException e) {
            U.warn(log, "Fail connect to " + address, e);
        }

        CountDownLatch downLatch = new CountDownLatch(1);

        runAsync(() -> {
            downloader.download(finishFut);

            downLatch.countDown();
        });

        SocketChannel finalSc = sc;

        runAsync(() -> uploader.upload(finalSc, finishFut));

        finishFut.get();

        downloader.download(finishFut.get(), null);

        downLatch.await();

        assertTrue(DOWNLOADER_PATH.toFile().exists());

        assertEquals(UPLOADER_PATH.toFile().length(), DOWNLOADER_PATH.toFile().length());

        assertArrayEquals(Files.readAllBytes(UPLOADER_PATH), Files.readAllBytes(DOWNLOADER_PATH));
    }
}
