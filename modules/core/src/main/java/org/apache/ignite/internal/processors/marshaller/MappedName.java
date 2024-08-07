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

package org.apache.ignite.internal.processors.marshaller;

import java.io.Serializable;
import java.util.Objects;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Contains mapped class name and boolean flag showing whether this mapping was accepted by other nodes or not.
 */
public final class MappedName implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final String clsName;

    /** */
    private final boolean accepted;

    /**
     * @param clsName Class name.
     * @param accepted Accepted.
     */
    public MappedName(String clsName, boolean accepted) {
        this.clsName = clsName;
        this.accepted = accepted;
    }

    /**
     *
     */
    public String className() {
        return clsName;
    }

    /**
     *
     */
    public boolean accepted() {
        return accepted;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MappedName name = (MappedName)o;

        return accepted == name.accepted && Objects.equals(clsName, name.clsName);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return Objects.hash(clsName, accepted);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(MappedName.class, this);
    }
}
