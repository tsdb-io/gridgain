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

import ObjectID from 'bson-objectid';
import omit from 'lodash/fp/omit';
import {CacheModes, AtomicityModes, ShortCache} from '../types';
import {Menu} from 'app/types';

export default class Caches {
    static $inject = ['$http'];

    cacheModes: Menu<CacheModes> = [
        {value: 'LOCAL', label: 'LOCAL'},
        {value: 'REPLICATED', label: 'REPLICATED'},
        {value: 'PARTITIONED', label: 'PARTITIONED'}
    ];

    atomicityModes: Menu<AtomicityModes> = [
        {value: 'ATOMIC', label: 'ATOMIC'},
        {value: 'TRANSACTIONAL', label: 'TRANSACTIONAL'},
        {value: 'TRANSACTIONAL_SNAPSHOT', label: 'TRANSACTIONAL_SNAPSHOT'}
    ];

    constructor(private $http: ng.IHttpService) {}

    saveCache(cache) {
        return this.$http.post('/api/v1/configuration/caches/save', cache);
    }

    getCache(cacheID: string) {
        return this.$http.get(`/api/v1/configuration/caches/${cacheID}`);
    }

    removeCache(cacheID: string) {
        return this.$http.post(`/api/v1/configuration/caches/remove/${cacheID}`);
    }

    getBlankCache() {
        return {
            _id: ObjectID.generate(),
            evictionPolicy: {},
            cacheMode: 'PARTITIONED',
            atomicityMode: 'ATOMIC',
            readFromBackup: true,
            copyOnRead: true,
            cacheStoreFactory: {
                CacheJdbcBlobStoreFactory: {
                    connectVia: 'DataSource'
                },
                CacheHibernateBlobStoreFactory: {
                    hibernateProperties: []
                }
            },
            writeBehindCoalescing: true,
            nearConfiguration: {},
            sqlFunctionClasses: [],
            domains: []
        };
    }

    toShortCache(cache: any): ShortCache {
        return {
            _id: cache._id,
            name: cache.name,
            backups: cache.backups,
            cacheMode: cache.cacheMode,
            atomicityMode: cache.atomicityMode
        };
    }

    normalize = omit(['__v', 'space', 'clusters']);

    nodeFilterKinds = [
        {value: 'IGFS', label: 'IGFS nodes'},
        {value: 'Custom', label: 'Custom'},
        {value: null, label: 'Not set'}
    ];

    memoryModes = [
        {value: 'ONHEAP_TIERED', label: 'ONHEAP_TIERED'},
        {value: 'OFFHEAP_TIERED', label: 'OFFHEAP_TIERED'},
        {value: 'OFFHEAP_VALUES', label: 'OFFHEAP_VALUES'}
    ];

    diskPageCompression = [
        {value: 'SKIP_GARBAGE', label: 'SKIP_GARBAGE'},
        {value: 'ZSTD', label: 'ZSTD'},
        {value: 'LZ4', label: 'LZ4'},
        {value: 'SNAPPY', label: 'SNAPPY'},
        {value: null, label: 'Disabled'}
    ];

    offHeapMode = {
        _val(cache) {
            return (cache.offHeapMode === null || cache.offHeapMode === void 0) ? -1 : cache.offHeapMode;
        },
        onChange: (cache) => {
            const offHeapMode = this.offHeapMode._val(cache);
            switch (offHeapMode) {
                case 1:
                    return cache.offHeapMaxMemory = cache.offHeapMaxMemory > 0 ? cache.offHeapMaxMemory : null;
                case 0:
                case -1:
                    return cache.offHeapMaxMemory = cache.offHeapMode;
                default: break;
            }
        },
        required: (cache) => cache.memoryMode === 'OFFHEAP_TIERED',
        offheapDisabled: (cache) => !(cache.memoryMode === 'OFFHEAP_TIERED' && this.offHeapMode._val(cache) === -1),
        default: 'Disabled'
    };

    offHeapModes = [
        {value: -1, label: 'Disabled'},
        {value: 1, label: 'Limited'},
        {value: 0, label: 'Unlimited'}
    ];

    offHeapMaxMemory = {
        min: 1
    };

    memoryMode = {
        default: 'ONHEAP_TIERED',
        offheapAndDomains: (cache) => {
            return !(cache.memoryMode === 'OFFHEAP_VALUES' && cache.domains.length);
        }
    };

    evictionPolicy = {
        required: (cache) => {
            return (cache.memoryMode || this.memoryMode.default) === 'ONHEAP_TIERED'
                && cache.offHeapMaxMemory > 0
                && !cache.evictionPolicy.kind;
        },
        values: [
            {value: 'LRU', label: 'LRU'},
            {value: 'FIFO', label: 'FIFO'},
            {value: 'SORTED', label: 'Sorted'},
            {value: null, label: 'Not set'}
        ],
        kind: {
            default: 'Not set'
        },
        maxMemorySize: {
            min: (evictionPolicy) => {
                const policy = evictionPolicy[evictionPolicy.kind];

                if (!policy)
                    return true;

                const maxSize = policy.maxSize === null || policy.maxSize === void 0
                    ? this.evictionPolicy.maxSize.default
                    : policy.maxSize;

                return maxSize ? 0 : 1;
            },
            default: 0
        },
        maxSize: {
            min: (evictionPolicy) => {
                const policy = evictionPolicy[evictionPolicy.kind];

                if (!policy)
                    return true;

                const maxMemorySize = policy.maxMemorySize === null || policy.maxMemorySize === void 0
                    ? this.evictionPolicy.maxMemorySize.default
                    : policy.maxMemorySize;

                return maxMemorySize ? 0 : 1;
            },
            default: 100000
        }
    };

    cacheStoreFactory = {
        kind: {
            default: 'Not set'
        },
        values: [
            {value: 'CacheJdbcPojoStoreFactory', label: 'JDBC POJO store factory'},
            {value: 'CacheJdbcBlobStoreFactory', label: 'JDBC BLOB store factory'},
            {value: 'CacheHibernateBlobStoreFactory', label: 'Hibernate BLOB store factory'},
            {value: null, label: 'Not set'}
        ],
        storeDisabledValueOff: (cache, value) => {
            return cache && cache.cacheStoreFactory.kind ? true : !value;
        },
        storeEnabledReadOrWriteOn: (cache) => {
            return cache && cache.cacheStoreFactory.kind ? (cache.readThrough || cache.writeThrough) : true;
        }
    };

    writeBehindFlush = {
        min: (cache) => {
            return cache.writeBehindFlushSize === 0 && cache.writeBehindFlushFrequency === 0
                ? 1
                : 0;
        }
    };

    getCacheBackupsCount(cache: ShortCache) {
        return this.shouldShowCacheBackupsCount(cache)
            ? (cache.backups || 0)
            : void 0;
    }

    shouldShowCacheBackupsCount(cache: ShortCache) {
        return cache && cache.cacheMode === 'PARTITIONED';
    }
}
