﻿/*
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

#pragma warning disable 618
namespace Apache.Ignite.Core.Tests.Cache
{
    using System.Linq;
    using Apache.Ignite.Core.Cache;
    using Apache.Ignite.Core.Cache.Configuration;
    using NUnit.Framework;

    /// <summary>
    /// Memory metrics tests.
    /// </summary>
    public class MemoryMetricsTest
    {
        /** */
        private const string MemoryPolicyWithMetrics = "plcWithMetrics";

        /** */
        private const string MemoryPolicyNoMetrics = "plcNoMetrics";

        /// <summary>
        /// Tests the memory metrics.
        /// </summary>
        [Test]
        public void TestMemoryMetrics()
        {
            var ignite = StartIgniteWithTwoPolicies();

            // Verify metrics.
            var metrics = ignite.GetMemoryMetrics().OrderBy(x => x.Name).ToArray();
            Assert.AreEqual(5, metrics.Length);  // two defined plus system, TxLog and volatile.

            var emptyMetrics = metrics[0];
            Assert.AreEqual(MemoryPolicyNoMetrics, emptyMetrics.Name);
            AssertMetricsAreEmpty(emptyMetrics);

            var memMetrics = metrics[1];
            Assert.AreEqual(MemoryPolicyWithMetrics, memMetrics.Name);
            Assert.Greater(memMetrics.AllocationRate, 0);
            Assert.AreEqual(0, memMetrics.EvictionRate);
            Assert.AreEqual(0, memMetrics.LargeEntriesPagesPercentage);
            Assert.Greater(memMetrics.PageFillFactor, 0);
            Assert.Greater(memMetrics.TotalAllocatedPages, 1000);

            var sysMetrics = metrics[2];
            Assert.AreEqual("sysMemPlc", sysMetrics.Name);
            AssertMetricsAreNotEmpty(sysMetrics);

            var txLogMetrics = metrics[3];
            Assert.AreEqual("TxLog", txLogMetrics.Name);
            AssertMetricsAreEmpty(txLogMetrics);

            var volatileMetrics = metrics[4];
            Assert.AreEqual("volatileDsMemPlc", volatileMetrics.Name);
            AssertMetricsAreEmpty(volatileMetrics);

            // Metrics by name.
            emptyMetrics = ignite.GetMemoryMetrics(MemoryPolicyNoMetrics);
            Assert.AreEqual(MemoryPolicyNoMetrics, emptyMetrics.Name);
            AssertMetricsAreEmpty(emptyMetrics);

            memMetrics = ignite.GetMemoryMetrics(MemoryPolicyWithMetrics);
            Assert.AreEqual(MemoryPolicyWithMetrics, memMetrics.Name);
            Assert.Greater(memMetrics.AllocationRate, 0);
            Assert.AreEqual(0, memMetrics.EvictionRate);
            Assert.AreEqual(0, memMetrics.LargeEntriesPagesPercentage);
            Assert.Greater(memMetrics.PageFillFactor, 0);
            Assert.Greater(memMetrics.TotalAllocatedPages, 1000);

            sysMetrics = ignite.GetMemoryMetrics("sysMemPlc");
            Assert.AreEqual("sysMemPlc", sysMetrics.Name);
            AssertMetricsAreNotEmpty(sysMetrics);

            // Invalid name.
            Assert.IsNull(ignite.GetMemoryMetrics("boo"));
        }

        /// <summary>
        /// Asserts that metrics are empty.
        /// </summary>
        private static void AssertMetricsAreEmpty(IMemoryMetrics metrics)
        {
            Assert.AreEqual(0, metrics.AllocationRate);
            Assert.AreEqual(0, metrics.EvictionRate);
            Assert.AreEqual(0, metrics.LargeEntriesPagesPercentage);
            Assert.AreEqual(0, metrics.PageFillFactor);
        }

        /// <summary>
        /// Asserts that metrics are not empty.
        /// </summary>
        private static void AssertMetricsAreNotEmpty(IMemoryMetrics metrics)
        {
            Assert.Greater(metrics.AllocationRate, 0);
            Assert.Greater(metrics.PageFillFactor, 0);
            Assert.Greater(metrics.TotalAllocatedPages, 100);
        }

        /// <summary>
        /// Starts the ignite with two policies.
        /// </summary>
        private static IIgnite StartIgniteWithTwoPolicies()
        {
            var cfg = new IgniteConfiguration(TestUtils.GetTestConfiguration())
            {
                DataStorageConfiguration = null,
                MemoryConfiguration = new MemoryConfiguration
                {
                    DefaultMemoryPolicyName = MemoryPolicyWithMetrics,
                    MemoryPolicies = new[]
                    {
                        new MemoryPolicyConfiguration
                        {
                            Name = MemoryPolicyWithMetrics,
                            MetricsEnabled = true
                        },
                        new MemoryPolicyConfiguration
                        {
                            Name = MemoryPolicyNoMetrics,
                            MetricsEnabled = false
                        }
                    }
                }
            };

            var ignite = Ignition.Start(cfg);

            // Create caches and do some things with them.
            var cacheNoMetrics = ignite.CreateCache<int, int>(new CacheConfiguration("cacheNoMetrics")
            {
                MemoryPolicyName = MemoryPolicyNoMetrics
            });

            cacheNoMetrics.Put(1, 1);
            cacheNoMetrics.Get(1);

            var cacheWithMetrics = ignite.CreateCache<int, int>(new CacheConfiguration("cacheWithMetrics")
            {
                MemoryPolicyName = MemoryPolicyWithMetrics
            });

            cacheWithMetrics.Put(1, 1);
            cacheWithMetrics.Get(1);

            return ignite;
        }

        /// <summary>
        /// Tears down the test.
        /// </summary>
        [TearDown]
        public void TearDown()
        {
            Ignition.StopAll(true);
        }
    }
}
