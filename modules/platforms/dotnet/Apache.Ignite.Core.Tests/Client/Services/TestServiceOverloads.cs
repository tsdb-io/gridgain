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

namespace Apache.Ignite.Core.Tests.Client.Services
{
    using Apache.Ignite.Core.Services;
    using Apache.Ignite.Core.Tests.Client.Cache;

    /// <summary>
    /// Tests service with overloaded methods.
    /// </summary>
    public class TestServiceOverloads : ITestServiceOverloads, IService
    {
        /** <inheritdoc /> */
        public bool Foo()
        {
            return true;
        }

        /** <inheritdoc /> */
        public int Foo(int x)
        {
            return 1;
        }

        /** <inheritdoc /> */
        public int Foo(uint x)
        {
            return 2;
        }

        /** <inheritdoc /> */
        public int Foo(byte x)
        {
            return 3;
        }

        /** <inheritdoc /> */
        public int Foo(short x)
        {
            return 4;
        }

        /** <inheritdoc /> */
        public int Foo(ushort x)
        {
            return 5;
        }

        /** <inheritdoc /> */
        public int Foo(Person x)
        {
            return 6;
        }

        /** <inheritdoc /> */
        public int Foo(int[] x)
        {
            return 8;
        }

        /** <inheritdoc /> */
        public int Foo(object[] x)
        {
            return 9;
        }

        /** <inheritdoc /> */
        public int Foo(Person[] x)
        {
            return 10;
        }

        /** <inheritdoc /> */
        public void Init(IServiceContext context)
        {
            // No-op.
        }

        /** <inheritdoc /> */
        public void Execute(IServiceContext context)
        {
            // No-op.
        }

        /** <inheritdoc /> */
        public void Cancel(IServiceContext context)
        {
            // No-op.
        }
    }
}
