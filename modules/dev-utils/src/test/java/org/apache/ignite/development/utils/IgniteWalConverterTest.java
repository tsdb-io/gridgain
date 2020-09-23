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

package org.apache.ignite.development.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord;
import org.apache.ignite.internal.processors.cache.persistence.wal.serializer.RecordV1Serializer;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * Test for IgniteWalConverter
 */
public class IgniteWalConverterTest extends GridCommonAbstractTest {
    /** */
    public static final String PERSON_NAME_PREFIX = "Name ";

    /** Flag "skip CRC calculation" in system property save before test and restore after. */
    private String beforeIgnitePdsSkipCrc;

    /** Flag "skip CRC calculation" in RecordV1Serializer save before test and restore after. */
    private boolean beforeSkipCrc;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        beforeIgnitePdsSkipCrc = System.getProperty(IgniteSystemProperties.IGNITE_PDS_SKIP_CRC);
        beforeSkipCrc = RecordV1Serializer.skipCrc;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids(true);

        cleanPersistenceDir();

        super.afterTest();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        if (beforeIgnitePdsSkipCrc != null)
            System.setProperty(IgniteSystemProperties.IGNITE_PDS_SKIP_CRC, beforeIgnitePdsSkipCrc);
        else
            System.clearProperty(IgniteSystemProperties.IGNITE_PDS_SKIP_CRC);

        RecordV1Serializer.skipCrc = beforeSkipCrc;

        super.afterTestsStopped();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        final IgniteConfiguration igniteConfiguration = super.getConfiguration(igniteInstanceName);

        igniteConfiguration.setDataStorageConfiguration(getDataStorageConfiguration());

        final CacheConfiguration cacheConfiguration = new CacheConfiguration<>()
            .setName(DEFAULT_CACHE_NAME)
            .setCacheMode(CacheMode.PARTITIONED)
            .setBackups(0)
            .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
            .setIndexedTypes(PersonKey.class, Person.class);

        igniteConfiguration.setCacheConfiguration(cacheConfiguration);

        return igniteConfiguration;
    }

    /** @return DataStorageConfiguration. */
    private DataStorageConfiguration getDataStorageConfiguration() {
        final DataStorageConfiguration dataStorageConfiguration = new DataStorageConfiguration()
            .setWalSegmentSize(4 * 1024 * 1024)
            .setWalMode(WALMode.LOG_ONLY)
            .setCheckpointFrequency(1000)
            .setWalCompactionEnabled(true)
            .setDefaultDataRegionConfiguration(getDataRegionConfiguration());

        return dataStorageConfiguration;
    }

    /** @return DataRegionConfiguration. */
    private DataRegionConfiguration getDataRegionConfiguration() {
        final DataRegionConfiguration dataRegionConfiguration = new DataRegionConfiguration()
            .setPersistenceEnabled(true)
            .setMaxSize(100L * 1024 * 1024);

        return dataRegionConfiguration;
    }

    /**
     * Checking utility IgniteWalConverter
     * <ul>
     *     <li>Start node</li>
     *     <li>Create cache with <a href="https://apacheignite.readme.io/docs/indexes#section-registering-indexed-types">Registering Indexed Types</a></li>
     *     <li>Put several entity</li>
     *     <li>Stop node</li>
     *     <li>Read wal with specifying binaryMetadata</li>
     *     <li>Check that the output contains all DataRecord with previously added entities</li>
     * </ul>
     *
     * @throws Exception If failed.
     */
    @Test
    public void testIgniteWalConverter() throws Exception {
        final List<Person> list = new LinkedList<>();

        final String nodeFolder = createWal(list);

        final ByteArrayOutputStream outByte = new ByteArrayOutputStream();

        final PrintStream out = new PrintStream(outByte);

        final IgniteWalConverterArguments arg = new IgniteWalConverterArguments(
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_PATH, false),
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH, false),
            DataStorageConfiguration.DFLT_PAGE_SIZE,
            new File(U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_BINARY_METADATA_PATH, false), nodeFolder),
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_MARSHALLER_PATH, false),
            true,
            null,
            null, null, null, null, true,true
        );

        IgniteWalConverter.convert(out, arg);

        final String result = outByte.toString();

        int index = 0;

        for (Person person : list) {
            boolean find = false;

            index = result.indexOf("DataRecord", index);

            if (index > 0) {
                index = result.indexOf("PersonKey", index + 10);

                if (index > 0) {
                    index = result.indexOf("id=" + person.getId(), index + 9);

                    if (index > 0) {
                        index = result.indexOf(person.getClass().getSimpleName(), index + 4);

                        if (index > 0) {
                            index = result.indexOf("id=" + person.getId(), index + person.getClass().getSimpleName().length());

                            if (index > 0) {
                                index = result.indexOf("name=" + person.getName(), index + 4);

                                find = index > 0;
                            }
                        }
                    }
                }
            }

            assertTrue("DataRecord for Person(id=" + person.getId() + ") not found", find);
        }
    }

    /**
     * Checking utility IgniteWalConverter with out binary_meta
     * <ul>
     *     <li>Start node</li>
     *     <li>Create cache with <a href="https://apacheignite.readme.io/docs/indexes#section-registering-indexed-types">Registering Indexed Types</a></li>
     *     <li>Put several entity</li>
     *     <li>Stop node</li>
     *     <li>Read wal with <b>out</b> specifying binaryMetadata</li>
     *     <li>Check that the output contains all DataRecord and in DataRecord not empty key and value</li>
     * </ul>
     *
     * @throws Exception If failed.
     */
    @Test
    public void testIgniteWalConverterWithOutBinaryMeta() throws Exception {
        final List<Person> list = new LinkedList<>();

        createWal(list);

        final ByteArrayOutputStream outByte = new ByteArrayOutputStream();

        final PrintStream out = new PrintStream(outByte);

        final IgniteWalConverterArguments arg = new IgniteWalConverterArguments(
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_PATH, false),
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH, false),
            DataStorageConfiguration.DFLT_PAGE_SIZE,
            null,
            null,
            false,
            null,
            null, null, null, null, true,true
        );

        IgniteWalConverter.convert(out, arg);

        final String result = outByte.toString();

        int index = 0;

        for (Person person : list) {
            boolean find = false;

            index = result.indexOf("DataRecord", index);

            if (index > 0) {
                index = result.indexOf(" v = [", index + 10);

                if (index > 0) {
                    int start = index + 6;

                    index = result.indexOf("]", start);

                    if (index > 0) {
                        final String value = result.substring(start, index);

                        find = new String(Base64.getDecoder().decode(value)).contains(person.getName());
                    }
                }
            }

            assertTrue("DataRecord for Person(id=" + person.getId() + ") not found", find);
        }
    }

    /**
     * Checking utility IgniteWalConverter on broken WAL
     * <ul>
     *     <li>Start node</li>
     *     <li>Create cache with <a href="https://apacheignite.readme.io/docs/indexes#section-registering-indexed-types">Registering Indexed Types</a></li>
     *     <li>Put several entity</li>
     *     <li>Stop node</li>
     *     <li>Change byte in DataRecord value</li>
     *     <li>Read wal</li>
     *     <li>Check one error when reading WAL</li>
     * </ul>
     *
     * @throws Exception If failed.
     */
    @Test
    public void testIgniteWalConverterWithBrokenWal() throws Exception {
        final List<Person> list = new LinkedList<>();

        final String nodeFolder = createWal(list);

        final File walDir = U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_PATH, false);

        final File wal = new File(walDir, nodeFolder + File.separator + "0000000000000000.wal");

        try (RandomAccessFile raf = new RandomAccessFile(wal, "rw")) {
            raf.seek(RecordV1Serializer.HEADER_RECORD_SIZE); // HeaderRecord

            byte findByte[] = (PERSON_NAME_PREFIX + 0).getBytes();

            boolean find = false;

            while (!find) {
                int recordTypeIndex = raf.read();

                if (recordTypeIndex > 0) {
                    recordTypeIndex--;

                    final long idx = raf.readLong();

                    final int fileOff = Integer.reverseBytes(raf.readInt());

                    final int len = Integer.reverseBytes(raf.readInt());

                    if (recordTypeIndex == WALRecord.RecordType.DATA_RECORD.index()) {
                        int i = 0;

                        int b;

                        while (!find && (b = raf.read()) >= 0) {
                            if (findByte[i] == b) {
                                i++;

                                if (i == findByte.length)
                                    find = true;
                            }
                            else
                                i = 0;
                        }
                        if (find) {
                            raf.seek(raf.getFilePointer() - 1);

                            raf.write(' ');
                        }
                    }

                    raf.seek(fileOff + len);
                }
            }
        }

        final ByteArrayOutputStream outByte = new ByteArrayOutputStream();

        final PrintStream out = new PrintStream(outByte);

        final IgniteWalConverterArguments arg = new IgniteWalConverterArguments(
            walDir,
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH, false),
            DataStorageConfiguration.DFLT_PAGE_SIZE,
            new File(U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_BINARY_METADATA_PATH, false), nodeFolder),
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_MARSHALLER_PATH, false),
            true,
            null,
            null, null, null, null, true,true
        );

        IgniteWalConverter.convert(out, arg);

        final String result = outByte.toString();

        int index = 0;

        int countErrorRead = 0;

        for (Person person : list) {
            boolean find = false;

            index = result.indexOf("DataRecord", index);

            if (index > 0) {
                index = result.indexOf("PersonKey", index + 10);

                if (index > 0) {
                    index = result.indexOf("id=" + person.getId(), index + 9);

                    if (index > 0) {
                        index = result.indexOf(person.getClass().getSimpleName(), index + 4);

                        if (index > 0) {
                            index = result.indexOf("id=" + person.getId(), index + person.getClass().getSimpleName().length());

                            if (index > 0) {
                                index = result.indexOf("name=" + person.getName(), index + 4);

                                find = index > 0;
                            }
                        }
                    }
                }
            }

            if (!find)
                countErrorRead++;
        }
        assertEquals(1, countErrorRead);
    }

    /**
     * Checking utility IgniteWalConverter on unreadable WAL
     * <ul>
     *     <li>Start node</li>
     *     <li>Create cache with <a href="https://apacheignite.readme.io/docs/indexes#section-registering-indexed-types">Registering Indexed Types</a></li>
     *     <li>Put several entity</li>
     *     <li>Stop node</li>
     *     <li>Change byte RecordType in second DataRecord</li>
     *     <li>Read wal</li>
     *     <li>Check contains one DataRecord in output before error when reading WAL</li>
     * </ul>
     *
     * @throws Exception If failed.
     */
    @Test
    public void testIgniteWalConverterWithUnreadableWal() throws Exception {
        final List<Person> list = new LinkedList<>();

        final String nodeFolder = createWal(list);

        final File walDir = U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_PATH, false);

        final File wal = new File(walDir, nodeFolder + File.separator + "0000000000000000.wal");

        try (RandomAccessFile raf = new RandomAccessFile(wal, "rw")) {
            raf.seek(RecordV1Serializer.HEADER_RECORD_SIZE); // HeaderRecord

            int find = 0;

            while (find < 2) {
                int recordTypeIndex = raf.read();

                if (recordTypeIndex > 0) {
                    recordTypeIndex--;

                    if (recordTypeIndex == WALRecord.RecordType.DATA_RECORD.index()) {
                        find++;

                        if (find == 2) {
                            raf.seek(raf.getFilePointer() - 1);

                            raf.write(Byte.MAX_VALUE);
                        }
                    }

                    final long idx = raf.readLong();

                    final int fileOff = Integer.reverseBytes(raf.readInt());

                    final int len = Integer.reverseBytes(raf.readInt());

                    raf.seek(fileOff + len);
                }
            }
        }

        final ByteArrayOutputStream outByte = new ByteArrayOutputStream();

        final PrintStream out = new PrintStream(outByte);

        final IgniteWalConverterArguments arg = new IgniteWalConverterArguments(
            walDir,
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_WAL_ARCHIVE_PATH, false),
            DataStorageConfiguration.DFLT_PAGE_SIZE,
            new File(U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_BINARY_METADATA_PATH, false), nodeFolder),
            U.resolveWorkDirectory(U.defaultWorkDirectory(), DataStorageConfiguration.DFLT_MARSHALLER_PATH, false),
            true,
            null,
            null, null, null, null, true,true
        );

        IgniteWalConverter.convert(out, arg);

        final String result = outByte.toString();

        int index = 0;

        int countErrorRead = 0;

        for (Person person : list) {
            boolean find = false;

            index = result.indexOf("DataRecord", index);

            if (index > 0) {
                index = result.indexOf("PersonKey", index + 10);

                if (index > 0) {
                    index = result.indexOf("id=" + person.getId(), index + 9);

                    if (index > 0) {
                        index = result.indexOf(person.getClass().getSimpleName(), index + 4);

                        if (index > 0) {
                            index = result.indexOf("id=" + person.getId(), index + person.getClass().getSimpleName().length());

                            if (index > 0) {
                                index = result.indexOf("name=" + person.getName(), index + 4);

                                find = index > 0;
                            }
                        }
                    }
                }
            }

            if (!find)
                countErrorRead++;
        }

        assertEquals(9, countErrorRead);
    }

    /**
     * Common part
     * <ul>
     *    <li>Start node</li>
     *    <li>Create cache with <a href="https://apacheignite.readme.io/docs/indexes#section-registering-indexed-types">Registering Indexed Types</a></li>
     *    <li>Put several entity</li>
     * </ul>
     *
     * @param list Returns entities that have been added.
     * @return Node folder name.
     * @throws Exception
     */
    private String createWal(List<Person> list) throws Exception {
        String nodeFolder;

        try (final IgniteEx node = startGrid(0)) {
            node.cluster().active(true);

            nodeFolder = node.context().pdsFolderResolver().resolveFolders().folderName();

            final IgniteCache<PersonKey, Person> cache = node.cache(DEFAULT_CACHE_NAME);

            for (int i = 0; i < 10; i++) {
                final PersonKey key = new PersonKey(i);

                final Person value;

                if (i % 2 == 0)
                    value = new Person(i, PERSON_NAME_PREFIX + i);
                else
                    value = new PersonEx(i, PERSON_NAME_PREFIX + i, "Additional information " + i, "Description " + i);

                cache.put(key, value);

                list.add(value);
            }
        }

        return nodeFolder;
    }
}
