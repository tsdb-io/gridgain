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

namespace Apache.Ignite.Core.DataStructures
{
    /// <summary>
    /// Represents a distributed atomic long value.
    /// </summary>
    public interface IAtomicLong
    {
        /// <summary>
        /// Gets the name of this atomic long.
        /// </summary>
        /// <value>
        /// Name of this atomic long.
        /// </value>
        string Name { get; }

        /// <summary>
        /// Returns current value.
        /// </summary>
        /// <returns>Current value of the atomic long.</returns>
        long Read();

        /// <summary>
        /// Increments current value and returns result.
        /// </summary>
        /// <returns>Current value of the atomic long.</returns>
        long Increment();

        /// <summary>
        /// Adds specified value to the current value and returns result.
        /// </summary>
        /// <param name="value">The value to add.</param>
        /// <returns>Current value of the atomic long.</returns>
        long Add(long value);

        /// <summary>
        /// Decrements current value and returns result.
        /// </summary>
        /// <returns>Current value of the atomic long.</returns>
        long Decrement();

        /// <summary>
        /// Sets current value to a specified value and returns the original value.
        /// </summary>
        /// <param name="value">The value to set.</param>
        /// <returns>Original value of the atomic long.</returns>
        long Exchange(long value);

        /// <summary>
        /// Compares current value with specified value for equality and, if they are equal, replaces current value.
        /// </summary>
        /// <param name="value">The value to set.</param>
        /// <param name="comparand">The value that is compared to the current value.</param>
        /// <returns>Original value of the atomic long.</returns>
        long CompareExchange(long value, long comparand);

        /// <summary>
        /// Determines whether this instance was removed from cache.
        /// </summary>
        /// <returns>True if this atomic was removed from cache; otherwise, false.</returns>
        bool IsClosed();

        /// <summary>
        /// Closes this instance.
        /// </summary>
        void Close();
    }
}
