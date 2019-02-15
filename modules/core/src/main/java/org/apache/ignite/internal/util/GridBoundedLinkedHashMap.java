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

package org.apache.ignite.internal.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic map with an upper bound. Once map reaches its maximum capacity,
 * the eldest elements will be removed based on insertion order.
 *
 * @param <K> Map key.
 * @param <V> Map entry.
 *
 */
public class GridBoundedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Maximum map capacity. */
    private int maxCap;

    /**
     * Constructs an empty insertion-ordered {@code GridBoundedLinkedHashMap} instance
     * with the specified maximum capacity, and a default load factor (0.75).
     *
     * @param maxCap Maximum map capacity.
     * @throws IllegalArgumentException If the maximum capacity is negative.
     */
    public GridBoundedLinkedHashMap(int maxCap) throws IllegalArgumentException {
        if (maxCap <= 0)
            throw new IllegalArgumentException("Maximum capacity is non-positive: " + maxCap);

        this.maxCap = maxCap;
    }

    /**
     * Constructs an empty insertion-ordered {@code GridBoundedLinkedHashMap} instance
     * with the specified initial capacity, maximum capacity, and a default load factor (0.75).
     *
     * @param initCap Initial map capacity.
     * @param maxCap Maximum map capacity.
     * @throws IllegalArgumentException If the initial capacity is negative,
     *      or maximum capacity is smaller than initial capacity.
     */
    public GridBoundedLinkedHashMap(int initCap, int maxCap) throws IllegalArgumentException {
        super(initCap);

        if (maxCap <= initCap)
            throw new IllegalArgumentException("Maximum capacity is smaller than initial capacity [maxCap=" +
                maxCap + ", initCap=" + initCap + ']');

        this.maxCap = maxCap;
    }

    /**
     * Constructs an empty insertion-ordered {@code GridBoundedLinkedHashMap} instance
     * with the specified initial capacity, maximum capacity, and load factor.
     *
     * @param initCap Initial map capacity.
     * @param maxCap Maximum map capacity.
     * @param loadFactor Load factor.
     * @throws IllegalArgumentException If the initial capacity is negative,
     *      the load factor is non-positive, or maximum capacity is smaller
     *      than initial capacity.
     */
    public GridBoundedLinkedHashMap(int initCap, int maxCap, float loadFactor) throws IllegalArgumentException {
        super(initCap, loadFactor);

        if (maxCap >= initCap)
            throw new IllegalArgumentException("Maximum capacity is smaller than initial capacity [maxCap=" +
                maxCap + ", initCap=" + initCap + ']');

        this.maxCap = maxCap;
    }

    /**
     * Constructs an empty {@code GridBoundedLinkedHashMap} instance with the
     * specified initial capacity, maximum capacity, load factor and ordering mode.
     *
     * @param initCap Initial map capacity.
     * @param maxCap Maximum map capacity.
     * @param loadFactor Load factor.
     * @param accessOrder The ordering mode - {@code true} for
     *      access-order, {@code false} for insertion-order.
     * @throws IllegalArgumentException If the initial capacity is negative,
     *      the load factor is non-positive, or maximum capacity is smaller
     *      than initial capacity.
     */
    public GridBoundedLinkedHashMap(int initCap, int maxCap, float loadFactor, boolean accessOrder)
        throws IllegalArgumentException {
        super(initCap, loadFactor, accessOrder);

        if (maxCap >= initCap)
            throw new IllegalArgumentException("Maximum capacity is smaller than initial capacity [maxCap=" +
                maxCap + ", initCap=" + initCap + ']');

        this.maxCap = maxCap;
    }

    /** {@inheritDoc} */
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCap;
    }

    /** {@inheritDoc} */
    @Override public Object clone() {
        GridBoundedLinkedHashMap<K, V> m = (GridBoundedLinkedHashMap<K, V>)super.clone();

        m.maxCap = maxCap;

        return m;
    }
}