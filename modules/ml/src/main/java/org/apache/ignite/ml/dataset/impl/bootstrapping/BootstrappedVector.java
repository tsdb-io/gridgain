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

package org.apache.ignite.ml.dataset.impl.bootstrapping;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.structures.LabeledVector;

/**
 * Represents vector with repetitions counters for subsamples in bootstrapped dataset.
 * Each counter shows the number of repetitions of the vector for the n-th sample.
 */
public class BootstrappedVector extends LabeledVector<Vector, Double> {
    /** Serial version uid. */
    private static final long serialVersionUID = -4583008673032917259L;

    /** Counters show the number of repetitions of the vector for the n-th sample. */
    private int[] counters;

    /**
     * Creates an instance of BootstrappedVector.
     *
     * @param features Features.
     * @param lb Label.
     * @param counters Repetitions counters.
     */
    public BootstrappedVector(Vector features, double lb, int[] counters) {
        super(features, lb);
        this.counters = counters;
    }

    /**
     * @return repetitions counters vector.
     */
    public int[] counters() {
        return counters;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        BootstrappedVector vector = (BootstrappedVector)o;
        return Arrays.equals(counters, vector.counters);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = super.hashCode();
        res = 31 * res + Arrays.hashCode(counters);
        return res;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(counters);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        counters = (int[]) in.readObject();
    }
}
