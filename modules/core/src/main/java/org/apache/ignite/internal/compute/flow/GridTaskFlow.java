/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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
package org.apache.ignite.internal.compute.flow;

public class GridTaskFlow {
    private final GridFlowElement rootNode;

    private final FlowResultAggregator aggregator;

    public GridTaskFlow(
        GridFlowElement rootNode,
        FlowResultAggregator aggregator
    ) {
        this.rootNode = rootNode;
        this.aggregator = aggregator;
    }

    public GridFlowElement rootNode() {
        return rootNode;
    }

    public FlowResultAggregator aggregator() {
        return aggregator;
    }
}