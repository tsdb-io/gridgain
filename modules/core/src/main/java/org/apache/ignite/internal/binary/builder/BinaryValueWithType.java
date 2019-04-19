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

package org.apache.ignite.internal.binary.builder;

import org.apache.ignite.internal.binary.BinaryWriterExImpl;
import org.apache.ignite.internal.binary.GridBinaryMarshaller;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 *
 */
class BinaryValueWithType implements BinaryLazyValue {
    /** */
    private byte type;

    /** */
    private Object val;

    /**
     * @param type Type
     * @param val Value.
     */
    BinaryValueWithType(byte type, Object val) {
        this.type = type;
        this.val = val;
    }

    /** {@inheritDoc} */
    @Override public void writeTo(BinaryWriterExImpl writer, BinaryBuilderSerializer ctx) {
        if (val instanceof BinaryBuilderSerializationAware)
            ((BinaryBuilderSerializationAware)val).writeTo(writer, ctx);
        else
            ctx.writeValue(writer, val, type == GridBinaryMarshaller.COL, type == GridBinaryMarshaller.MAP);
    }

    /**
     * @return Type ID.
     */
    public int typeId() {
        return type;
    }

    /** {@inheritDoc} */
    @Override public Object value() {
        if (val instanceof BinaryLazyValue)
            return ((BinaryLazyValue)val).value();

        return val;
    }

    /**
     * @param val New value.
     */
    public void value(Object val) {
        this.val = val;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(BinaryValueWithType.class, this);
    }
}
