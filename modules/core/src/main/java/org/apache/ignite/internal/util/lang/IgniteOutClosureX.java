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

package org.apache.ignite.internal.util.lang;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgniteOutClosure;

/**
 * Convenient out-closure subclass that allows for thrown grid exception. This class
 * implements {@link #apply()} method that calls {@link #applyx()} method and properly
 * wraps {@link IgniteCheckedException} into {@link GridClosureException} instance.
 */
public abstract class IgniteOutClosureX<T> implements IgniteOutClosure<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public T apply() {
        try {
            return applyx();
        }
        catch (IgniteCheckedException e) {
            throw F.wrap(e);
        }
    }

    /**
     * Out-closure body that can throw {@link IgniteCheckedException}.
     *
     * @return Element.
     * @throws IgniteCheckedException Thrown in case of any error condition inside of the closure.
     */
    public abstract T applyx() throws IgniteCheckedException;
}