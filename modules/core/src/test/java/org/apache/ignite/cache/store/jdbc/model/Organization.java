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

package org.apache.ignite.cache.store.jdbc.model;

import java.io.Serializable;

/**
 * Organization definition.
 */
public class Organization implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Value for id. */
    private Integer id;

    /** Value for name. */
    private String name;

    /** Value for city. */
    private String city;

    /**
     * Empty constructor.
     */
    public Organization() {
        // No-op.
    }

    /**
     * Full constructor.
     */
    public Organization(
        Integer id,
        String name,
        String city
    ) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    /**
     * Gets id.
     *
     * @return Value for id.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id New value for id.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Gets name.
     *
     * @return Value for name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name New value for name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets city.
     *
     * @return Value for city.
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets city.
     *
     * @param city New value for city.
     */
    public void setCity(String city) {
        this.city = city;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Organization))
            return false;

        Organization that = (Organization)o;

        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        if (city != null ? !city.equals(that.city) : that.city != null)
            return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = id != null ? id.hashCode() : 0;

        res = 31 * res + (name != null ? name.hashCode() : 0);

        res = 31 * res + (city != null ? city.hashCode() : 0);

        return res;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "Organization [id=" + id +
            ", name=" + name +
            ", city=" + city +
            "]";
    }
}
