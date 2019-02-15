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

package org.apache.ignite.testframework.configvariations;

import java.util.Arrays;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.internal.util.typedef.internal.SB;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.jetbrains.annotations.Nullable;

/**
 * Configurations variations factory.
 */
public class ConfigVariationsFactory implements ConfigFactory {
    /** */
    private final ConfigParameter<IgniteConfiguration>[][] igniteParams;

    /** */
    private final int[] igniteCfgVariation;

    /** */
    private final ConfigParameter<CacheConfiguration>[][] cacheParams;

    /** */
    private final int[] cacheCfgVariation;

    /** */
    private int backups = -1;

    /**
     * @param igniteParams Ignite Params.
     * @param igniteCfgVariation Ignite configuration variation.
     * @param cacheParams Cache Params.
     * @param cacheCfgVariation Cache config variation.
     */
    public ConfigVariationsFactory(ConfigParameter<IgniteConfiguration>[][] igniteParams,
        int[] igniteCfgVariation,
        @Nullable ConfigParameter<CacheConfiguration>[][] cacheParams,
        @Nullable int[] cacheCfgVariation) {
        this.igniteParams = igniteParams;
        this.igniteCfgVariation = igniteCfgVariation;
        this.cacheParams = cacheParams;
        this.cacheCfgVariation = cacheCfgVariation;
    }

    /** {@inheritDoc} */
    @Override public IgniteConfiguration getConfiguration(String igniteInstanceName, IgniteConfiguration srcCfg) {
        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setFailureHandler(new NoOpFailureHandler());

        if (srcCfg != null)
            copyDefaultsFromSource(cfg, srcCfg);

        if (igniteParams == null)
            return cfg;

        for (int i = 0; i < igniteCfgVariation.length; i++) {
            int var = igniteCfgVariation[i];

            ConfigParameter<IgniteConfiguration> cfgC = igniteParams[i][var];

            if (cfgC != null)
                cfgC.apply(cfg);
        }

        return cfg;
    }

    /**
     * @param cfg Config.
     * @param srcCfg Source config.
     */
    private static void copyDefaultsFromSource(IgniteConfiguration cfg, IgniteConfiguration srcCfg) {
        cfg.setIgniteInstanceName(srcCfg.getIgniteInstanceName());
        cfg.setGridLogger(srcCfg.getGridLogger());
        cfg.setNodeId(srcCfg.getNodeId());
        cfg.setIgniteHome(srcCfg.getIgniteHome());
        cfg.setMBeanServer(srcCfg.getMBeanServer());
        cfg.setMetricsLogFrequency(srcCfg.getMetricsLogFrequency());
        cfg.setConnectorConfiguration(srcCfg.getConnectorConfiguration());
        cfg.setCommunicationSpi(srcCfg.getCommunicationSpi());
        cfg.setNetworkTimeout(srcCfg.getNetworkTimeout());
        cfg.setDiscoverySpi(srcCfg.getDiscoverySpi());
        cfg.setCheckpointSpi(srcCfg.getCheckpointSpi());
        cfg.setIncludeEventTypes(srcCfg.getIncludeEventTypes());

        // Specials.
        ((TcpCommunicationSpi)cfg.getCommunicationSpi()).setSharedMemoryPort(-1);
        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setForceServerMode(true);
        cfg.getTransactionConfiguration().setTxSerializableEnabled(true);
    }

    /**
     * @return Description.
     */
    public String getIgniteConfigurationDescription() {
        if (igniteParams == null)
            return "";

        SB sb = new SB("[");

        for (int i = 0; i < igniteCfgVariation.length; i++) {
            int var = igniteCfgVariation[i];

            ConfigParameter<IgniteConfiguration> cfgC = igniteParams[i][var];

            if (cfgC != null) {
                sb.a(cfgC.name());

                if (i + 1 < igniteCfgVariation.length)
                    sb.a(", ");
            }
        }

        sb.a("]");

        return sb.toString();

    }

    /** {@inheritDoc} */
    @Override public CacheConfiguration cacheConfiguration(String igniteInstanceName) {
        if (cacheParams == null || cacheCfgVariation == null)
            throw new IllegalStateException("Failed to configure cache [cacheParams=" + Arrays.deepToString(cacheParams)
                + ", cacheCfgVariation=" + Arrays.toString(cacheCfgVariation) + "]");

        CacheConfiguration cfg = new CacheConfiguration();

        for (int i = 0; i < cacheCfgVariation.length; i++) {
            int var = cacheCfgVariation[i];

            ConfigParameter<CacheConfiguration> cfgC = cacheParams[i][var];

            if (cfgC != null)
                cfgC.apply(cfg);
        }

        if (backups > 0)
            cfg.setBackups(backups);

        return cfg;
    }

    /**
     * @return Description.
     */
    public String getCacheConfigurationDescription() {
        if (cacheCfgVariation == null)
            return "";

        SB sb = new SB("[");

        for (int i = 0; i < cacheCfgVariation.length; i++) {
            int var = cacheCfgVariation[i];

            ConfigParameter cfgC = cacheParams[i][var];

            if (cfgC != null) {
                sb.a(cfgC.name());

                if (i + 1 < cacheCfgVariation.length)
                    sb.a(", ");
            }
        }

        if (backups > 0)
            sb.a(", backups=").a(backups);

        sb.a("]");

        return sb.toString();
    }

    /**
     * @param backups New backups.
     */
    public void backups(int backups) {
        this.backups = backups;
    }
}
