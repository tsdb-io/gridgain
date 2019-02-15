﻿/*
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

namespace Apache.Ignite.Core.Tests.DataStructures
{
    using System.Linq;
    using NUnit.Framework;

    /// <summary>
    /// Atomic sequence test.
    /// </summary>
    public class AtomicSequenceTest : SpringTestBase
    {
        /** */
        private const string AtomicSeqName = "testAtomicSeq";

        /// <summary>
        /// Initializes a new instance of the <see cref="AtomicSequenceTest"/> class.
        /// </summary>
        public AtomicSequenceTest() : base("Config\\Compute\\compute-grid1.xml")
        {
            // No-op.
        }

        /** <inheritdoc /> */
        public override void TestSetUp()
        {
            base.TestSetUp();

            // Close test atomic if there is any
            Grid.GetAtomicSequence(AtomicSeqName, 0, true).Close();
        }

        /// <summary>
        /// Tests lifecycle of the AtomicSequence.
        /// </summary>
        [Test]
        public void TestCreateClose()
        {
            Assert.IsNull(Grid.GetAtomicSequence(AtomicSeqName, 10, false));

            // Nonexistent atomic returns null
            Assert.IsNull(Grid.GetAtomicSequence(AtomicSeqName, 10, false));

            // Create new
            var al = Grid.GetAtomicSequence(AtomicSeqName, 10, true);
            Assert.AreEqual(AtomicSeqName, al.Name);
            Assert.AreEqual(10, al.Read());
            Assert.AreEqual(false, al.IsClosed);

            // Get existing with create flag
            var al2 = Grid.GetAtomicSequence(AtomicSeqName, 5, true);
            Assert.AreEqual(AtomicSeqName, al2.Name);
            Assert.AreEqual(10, al2.Read());
            Assert.AreEqual(false, al2.IsClosed);

            // Get existing without create flag
            var al3 = Grid.GetAtomicSequence(AtomicSeqName, 5, false);
            Assert.AreEqual(AtomicSeqName, al3.Name);
            Assert.AreEqual(10, al3.Read());
            Assert.AreEqual(false, al3.IsClosed);

            al.Close();

            Assert.AreEqual(true, al.IsClosed);
            Assert.AreEqual(true, al2.IsClosed);
            Assert.AreEqual(true, al3.IsClosed);

            Assert.IsNull(Grid.GetAtomicSequence(AtomicSeqName, 10, false));
        }

        /// <summary>
        /// Tests modification methods.
        /// </summary>
        [Test]
        public void TestModify()
        {
            var atomics = Enumerable.Range(1, 10)
                .Select(x => Grid.GetAtomicSequence(AtomicSeqName, 5, true)).ToList();

            atomics.ForEach(x => Assert.AreEqual(5, x.Read()));

            Assert.AreEqual(10, atomics[0].Add(5));
            atomics.ForEach(x => Assert.AreEqual(10, x.Read()));

            Assert.AreEqual(11, atomics[0].Increment());
            atomics.ForEach(x => Assert.AreEqual(11, x.Read()));

            atomics.ForEach(x => x.BatchSize = 42);
            atomics.ForEach(x => Assert.AreEqual(42, x.BatchSize));
        }

        /// <summary>
        /// Tests multithreaded scenario.
        /// </summary>
        [Test]
        public void TestMultithreaded()
        {
            const int atomicCnt = 10;
            const int threadCnt = 5;
            const int iterations = 3000;

            // 10 atomics with same name
            var atomics = Enumerable.Range(1, atomicCnt)
                .Select(x => Grid.GetAtomicSequence(AtomicSeqName, 0, true)).ToList();

            // 5 threads increment 30000 times
            TestUtils.RunMultiThreaded(() =>
            {
                for (var i = 0; i < iterations; i++)
                    atomics.ForEach(x => x.Increment());
            }, threadCnt);

            atomics.ForEach(x => Assert.AreEqual(atomicCnt*threadCnt*iterations, x.Read()));
        }
    }
}