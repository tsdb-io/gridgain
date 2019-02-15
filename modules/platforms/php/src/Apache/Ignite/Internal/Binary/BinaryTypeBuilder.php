<?php
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

namespace Apache\Ignite\Internal\Binary;

use Apache\Ignite\Type\ComplexObjectType;

class BinaryTypeBuilder
{
    private $type;
    private $schema;
    private $fromStorage;
    
    private function __construct()
    {
        $this->type = null;
        $this->schema = null;
        $this->fromStorage = false;
    }
            
    public static function fromTypeName(string $typeName): BinaryTypeBuilder
    {
        $result = new BinaryTypeBuilder();
        $result->init($typeName);
        return $result;
    }

    public static function fromTypeId(BinaryCommunicator $communicator, int $typeId, ?int $schemaId): BinaryTypeBuilder
    {
        $result = new BinaryTypeBuilder();
        $type = $communicator->getTypeStorage()->getType($typeId, $schemaId);
        if ($type) {
            $result->type = $type;
            if ($schemaId !== null) {
                $result->schema = $type->getSchema($schemaId);
                if (!$result->schema) {
                    BinaryUtils::serializationError(
                        false,
                        sprintf('schema id "%d" specified for complex object of type "%s" not found', $schemaId, $type->getName()));
                }
                $result->fromStorage = true;
            } else {
                $result->schema = new BinarySchema();
            }
            return $result;
        }
        $result->init(null);
        $result->getType()->setId($typeId);
        return $result;
    }

    public static function fromObject(object $object, ComplexObjectType $complexObjectType = null): BinaryTypeBuilder
    {
        if ($complexObjectType) {
            return BinaryTypeBuilder::fromComplexObjectType($complexObjectType, $object);
        } else {
            $result = new BinaryTypeBuilder();
            $result->fromComplexObject(new ComplexObjectType(), $object);
            return $result;
        }
    }

    public static function fromComplexObjectType(ComplexObjectType $complexObjectType, object $object): BinaryTypeBuilder
    {
        $result = new BinaryTypeBuilder();
        $typeInfo = BinaryTypeStorage::getByComplexObjectType($complexObjectType);
        if ($typeInfo) {
            $result->type = $typeInfo[0];
            $result->schema = $typeInfo[1];
            $result->fromStorage = true;
        } else {
            $result->fromComplexObject($complexObjectType, $object);
            BinaryTypeStorage::setByComplexObjectType($complexObjectType, $result->type, $result->schema);
        }
        return $result;
    }
    
    public static function calcTypeName(ComplexObjectType $complexObjectType, object $object): string
    {
        $typeName = $complexObjectType->getIgniteTypeName();
        if (!$typeName) {
            $typeName = $object ? get_class($object) : null;
        }
        return $typeName;
    }

    public function getType(): BinaryType
    {
        return $this->type;
    }

    public function getTypeId(): int
    {
        return $this->type->getId();
    }

    public function getTypeName(): string
    {
        return $this->type->getName();
    }

    public function getSchema(): BinarySchema
    {
        return $this->schema;
    }
    
    public function getSchemaId(): int
    {
        return $this->schema->getId();
    }

    public function getFields(): array
    {
        return $this->type->getFields();
    }

    public function getField(int $fieldId): ?BinaryField
    {
        return $this->type->getField($fieldId);
    }

    public function setField(string $fieldName, int $fieldTypeCode = 0): void
    {
        $fieldId = BinaryField::calculateId($fieldName);
        if (!$this->type->hasField($fieldId) || !$this->schema->hasField($fieldId) ||
            $this->type->getField($fieldId)->getTypeCode() !== $fieldTypeCode) {
            $this->beforeModify();
            $this->type->setField(new BinaryField($fieldName, $fieldTypeCode));
            $this->schema->addField($fieldId);
        }
    }

    public function removeField(string $fieldName): void
    {
        $fieldId = BinaryField::calculateId($fieldName);
        if ($this->type->hasField($fieldId)) {
            $this->beforeModify();
            $this->type->removeField($fieldId);
            $this->schema->removeField($fieldId);
        }
    }

    public function finalize(BinaryCommunicator $communicator): void
    {
        $this->schema->finalize();
        $communicator->getTypeStorage()->addType($this->type, $this->schema);
    }

    private function fromComplexObject(ComplexObjectType $complexObjectType, object $object): void
    {
        $typeName = BinaryTypeBuilder::calcTypeName($complexObjectType, $object);
        $this->init($typeName);
        $this->setFields($complexObjectType, $object);
    }

    private function init(?string $typeName): void
    {
        $this->type = new BinaryType($typeName);
        $this->schema = new BinarySchema();
    }

    private function beforeModify(): void
    {
        if ($this->fromStorage) {
            $this->type = $this->type->cloneType();
            $this->schema = $this->schema->cloneSchema();
            $this->fromStorage = false;
        }
    }

    private function setFields(ComplexObjectType $complexObjectType, object $object): void
    {
        try {
            $reflect = new \ReflectionClass($object);
            $properties  = $reflect->getProperties(\ReflectionProperty::IS_PUBLIC);
            foreach ($properties as $property) {
                if ($property->isStatic()) {
                    continue;
                }
                $fieldName = $property->getName();
                $fieldType = $complexObjectType->getFieldType($fieldName);
                if (!$fieldType) {
                    $fieldValue = $property->getValue($object);
                    $fieldType = BinaryUtils::calcObjectType($fieldValue);
                }
                $this->setField($fieldName, BinaryUtils::getTypeCode($fieldType));
            }
        } catch (\ReflectionException $e) {
            BinaryUtils::serializationError(true, sprintf('class "%s" does not exist', get_class($object)));
        }
    }
}
