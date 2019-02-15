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

import _ from 'lodash';

import negate from 'lodash/negate';
import isNil from 'lodash/isNil';
import isEmpty from 'lodash/isEmpty';
import mixin from 'lodash/mixin';

const nonNil = negate(isNil);
const nonEmpty = negate(isEmpty);

mixin({
    nonNil,
    nonEmpty
});

export class EmptyBean {
    /**
     * @param {String} clsName
     */
    constructor(clsName) {
        this.properties = [];
        this.arguments = [];

        this.clsName = clsName;
    }

    isEmpty() {
        return false;
    }

    nonEmpty() {
        return !this.isEmpty();
    }

    isComplex() {
        return nonEmpty(this.properties) || !!_.find(this.arguments, (arg) => arg.clsName === 'MAP');
    }

    nonComplex() {
        return !this.isComplex();
    }

    findProperty(name) {
        return _.find(this.properties, {name});
    }
}

export class Bean extends EmptyBean {
    /**
     * @param {String} clsName
     * @param {String} id
     * @param {Object} src
     * @param {Object} dflts
     */
    constructor(clsName, id, src, dflts = {}) {
        super(clsName);

        this.id = id;

        this.src = src;
        this.dflts = dflts;
    }

    factoryMethod(name) {
        this.factoryMtd = name;

        return this;
    }

    /**
     * @param acc
     * @param clsName
     * @param model
     * @param name
     * @param {Function} nonEmpty Non empty function.
     * @param {Function} mapper Mapper function.
     * @returns {Bean}
     * @private
     */
    _property(acc, clsName, model, name, nonEmpty = () => true, mapper = (val) => val) {
        if (!this.src)
            return this;

        const value = mapper(_.get(this.src, model));

        if (nonEmpty(value) && value !== _.get(this.dflts, model))
            acc.push({clsName, name, value});

        return this;
    }

    isEmpty() {
        return isEmpty(this.arguments) && isEmpty(this.properties);
    }

    constructorArgument(clsName, value) {
        this.arguments.push({clsName, value});

        return this;
    }

    stringConstructorArgument(model) {
        return this._property(this.arguments, 'java.lang.String', model, null, nonEmpty);
    }

    intConstructorArgument(model) {
        return this._property(this.arguments, 'int', model, null, nonNil);
    }

    boolConstructorArgument(model) {
        return this._property(this.arguments, 'boolean', model, null, nonNil);
    }

    classConstructorArgument(model) {
        return this._property(this.arguments, 'java.lang.Class', model, null, nonEmpty);
    }

    pathConstructorArgument(model) {
        return this._property(this.arguments, 'PATH', model, null, nonEmpty);
    }

    constantConstructorArgument(model) {
        if (!this.src)
            return this;

        const value = _.get(this.src, model);
        const dflt = _.get(this.dflts, model);

        if (nonNil(value) && nonNil(dflt) && value !== dflt.value)
            this.arguments.push({clsName: dflt.clsName, constant: true, value});

        return this;
    }

    propertyConstructorArgument(value, hint = '') {
        this.arguments.push({clsName: 'PROPERTY', value, hint});

        return this;
    }

    /**
     * @param {String} id
     * @param {EmptyBean|Bean} value
     * @returns {Bean}
     */
    beanConstructorArgument(id, value) {
        this.arguments.push({clsName: 'BEAN', id, value});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} model
     * @param {Array.<Object>} entries
     * @returns {Bean}
     */
    mapConstructorArgument(id, model, entries) {
        if (!this.src)
            return this;

        const dflt = _.get(this.dflts, model);

        if (nonEmpty(entries) && nonNil(dflt) && entries !== dflt.entries) {
            this.arguments.push({
                clsName: 'MAP',
                id,
                keyClsName: dflt.keyClsName,
                keyField: dflt.keyField || 'name',
                valClsName: dflt.valClsName,
                valField: dflt.valField || 'value',
                entries
            });
        }

        return this;
    }

    valueOf(path) {
        return _.get(this.src, path) || _.get(this.dflts, path + '.value') || _.get(this.dflts, path);
    }

    includes(...paths) {
        return this.src && _.every(paths, (path) => {
            const value = _.get(this.src, path);
            const dflt = _.get(this.dflts, path);

            return nonNil(value) && value !== dflt;
        });
    }

    prop(clsName, name, value) {
        this.properties.push({clsName, name, value});
    }

    boolProperty(model, name = model) {
        return this._property(this.properties, 'boolean', model, name, nonNil);
    }

    byteProperty(model, name = model) {
        return this._property(this.properties, 'byte', model, name, nonNil);
    }

    intProperty(model, name = model) {
        return this._property(this.properties, 'int', model, name, nonNil);
    }

    longProperty(model, name = model) {
        return this._property(this.properties, 'long', model, name, nonNil);
    }

    floatProperty(model, name = model) {
        return this._property(this.properties, 'float', model, name, nonNil);
    }

    doubleProperty(model, name = model) {
        return this._property(this.properties, 'double', model, name, nonNil);
    }

    property(name, value, hint) {
        this.properties.push({clsName: 'PROPERTY', name, value, hint});

        return this;
    }

    propertyChar(name, value, hint) {
        this.properties.push({clsName: 'PROPERTY_CHAR', name, value, hint});

        return this;
    }

    propertyInt(name, value, hint) {
        this.properties.push({clsName: 'PROPERTY_INT', name, value, hint});

        return this;
    }

    stringProperty(model, name = model, mapper) {
        return this._property(this.properties, 'java.lang.String', model, name, nonEmpty, mapper);
    }

    pathProperty(model, name = model) {
        return this._property(this.properties, 'PATH', model, name, nonEmpty);
    }

    classProperty(model, name = model) {
        return this._property(this.properties, 'java.lang.Class', model, name, nonEmpty);
    }

    enumProperty(model, name = model) {
        if (!this.src)
            return this;

        const value = _.get(this.src, model);
        const dflt = _.get(this.dflts, model);

        if (nonNil(value) && nonNil(dflt) && value !== dflt.value)
            this.properties.push({clsName: dflt.clsName, name, value: dflt.mapper ? dflt.mapper(value) : value});

        return this;
    }

    emptyBeanProperty(model, name = model) {
        if (!this.src)
            return this;

        const cls = _.get(this.src, model);
        const dflt = _.get(this.dflts, model);

        if (nonEmpty(cls) && cls !== dflt)
            this.properties.push({clsName: 'BEAN', name, value: new EmptyBean(cls)});

        return this;
    }

    /**
     * @param {String} name
     * @param {EmptyBean|Bean} value
     * @returns {Bean}
     */
    beanProperty(name, value) {
        this.properties.push({clsName: 'BEAN', name, value});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} name
     * @param {Array} items
     * @param {String} typeClsName
     * @returns {Bean}
     */
    arrayProperty(id, name, items, typeClsName = 'java.lang.String') {
        if (items && items.length)
            this.properties.push({clsName: 'ARRAY', id, name, items, typeClsName});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} name
     * @param {Array} items
     * @param {String} typeClsName
     * @returns {Bean}
     */
    varArgProperty(id, name, items, typeClsName = 'java.lang.String') {
        if (items && items.length)
            this.properties.push({clsName: 'ARRAY', id, name, items, typeClsName, varArg: true});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} name
     * @param {Array} items
     * @param {String} typeClsName
     * @param {String} implClsName
     * @returns {Bean}
     */
    collectionProperty(id, name, items, typeClsName = 'java.lang.String', implClsName = 'java.util.ArrayList') {
        if (items && items.length)
            this.properties.push({id, name, items, clsName: 'COLLECTION', typeClsName, implClsName});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} model
     * @param {String} [name]
     * @param {Boolean} [ordered]
     * @returns {Bean}
     */
    mapProperty(id, model, name = model, ordered = false) {
        if (!this.src)
            return this;

        const entries = _.isString(model) ? _.get(this.src, model) : model;
        const dflt = _.isString(model) ? _.get(this.dflts, model) : _.get(this.dflts, name);

        if (nonEmpty(entries) && nonNil(dflt) && entries !== dflt.entries) {
            this.properties.push({
                clsName: 'MAP',
                id,
                name,
                ordered,
                keyClsName: dflt.keyClsName,
                keyField: dflt.keyField || 'name',
                valClsName: dflt.valClsName,
                valField: dflt.valField || 'value',
                entries
            });
        }

        return this;
    }

    propsProperty(id, model, name = model) {
        if (!this.src)
            return this;

        const entries = _.get(this.src, model);

        if (nonEmpty(entries))
            this.properties.push({clsName: 'java.util.Properties', id, name, entries});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} name
     * @param {EmptyBean|Bean} value
     */
    dataSource(id, name, value) {
        if (value)
            this.properties.push({clsName: 'DATA_SOURCE', id, name, value});

        return this;
    }

    /**
     * @param {String} id
     * @param {String} name
     * @param {Array<String>} eventTypes
     */
    eventTypes(id, name, eventTypes) {
        this.properties.push({clsName: 'EVENT_TYPES', id, name, eventTypes});
    }
}
