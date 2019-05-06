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

package org.apache.ignite.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for node metrics configuration properties.
 */
public class VisorMetricsConfiguration extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Metrics expired time. */
    private long expTime;

    /** Number of node metrics stored in memory. */
    private int histSize;

    /** Frequency of metrics log printout. */
    private long logFreq;

    /**
     * Default constructor.
     */
    public VisorMetricsConfiguration() {
        // No-op.
    }

    /**
     * Create transfer object for node metrics configuration properties.
     *
     * @param c Grid configuration.
     */
    public VisorMetricsConfiguration(IgniteConfiguration c) {
        expTime = c.getMetricsExpireTime();
        histSize = c.getMetricsHistorySize();
        logFreq = c.getMetricsLogFrequency();
    }

    /**
     * @return Metrics expired time.
     */
    public long getExpireTime() {
        return expTime;
    }

    /**
     * @return Number of node metrics stored in memory.
     */
    public int getHistorySize() {
        return histSize;
    }

    /**
     * @return Frequency of metrics log printout.
     */
    public long getLoggerFrequency() {
        return logFreq;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeLong(expTime);
        out.writeInt(histSize);
        out.writeLong(logFreq);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        expTime = in.readLong();
        histSize = in.readInt();
        logFreq = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorMetricsConfiguration.class, this);
    }
}
