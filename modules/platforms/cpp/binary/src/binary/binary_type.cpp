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

#include "ignite/binary/binary_type.h"

namespace ignite
{
    namespace binary
    {
        IGNORE_SIGNED_OVERFLOW
        int32_t GetBinaryStringHashCode(const char* val)
        {
            if (val)
            {
                int32_t hash = 0;

                int i = 0;

                while (true)
                {
                    char c = *(val + i++);

                    if (c == '\0')
                        break;

                    if ('A' <= c && c <= 'Z')
                        c = c | 0x20;

                    hash = 31 * hash + c;
                }

                return hash;
            }

            return 0;
        }
    }
}
