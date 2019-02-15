::
::                   GridGain Community Edition Licensing
::                   Copyright 2019 GridGain Systems, Inc.
::
:: Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
:: Restriction; you may not use this file except in compliance with the License. You may obtain a
:: copy of the License at
::
:: http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software distributed under the
:: License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
:: KIND, either express or implied. See the License for the specific language governing permissions
:: and limitations under the License.
::
:: Commons Clause Restriction
::
:: The Software is provided to you by the Licensor under the License, as defined below, subject to
:: the following condition.
::
:: Without limiting other conditions in the License, the grant of rights under the License will not
:: include, and the License does not grant to you, the right to Sell the Software.
:: For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
:: under the License to provide to third parties, for a fee or other consideration (including without
:: limitation fees for hosting or consulting/ support services related to the Software), a product or
:: service whose value derives, entirely or substantially, from the functionality of the Software.
:: Any license notice or attribution required by the License must also include this Commons Clause
:: License Condition notice.
::
:: For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
:: the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
:: Edition software provided with this notice.
::

::
:: SSL certificates generation.
::

::
:: Preconditions:
::  1. If needed, download Open SSL for Windows from "https://wiki.openssl.org/index.php/Binaries".
::   and unpack it to some folder.
::  2. If needed, install JDK 8 or newer. We need "keytool" from "JDK/bin."
::  3. Create "openssl.cnf" in some folder.
::     You may use "https://github.com/openssl/openssl/blob/master/apps/openssl.cnf" as template.
::  4. If needed, add "opensll" & "keytool" to PATH variable.
::
::  NOTE: In case of custom SERVER_DOMAIN_NAME you may need to tweak your "etc/hosts" file.
::

:: Set Open SSL variables.
set RANDFILE=_path_where_open_ssl_was_unpacked\.rnd
set OPENSSL_CONF=_path_where_open_ssl_was_unpacked\openssl.cnf

:: Certificates password.
set PWD=p123456

:: Server.
set SERVER_DOMAIN_NAME=localhost
set SERVER_EMAIL=support@test.com

:: Client.
set CLIENT_DOMAIN_NAME=localhost
set CLIENT_EMAIL=client@test.com

:: Cleanup.
del server.*
del client.*
del ca.*

:: Generate server config.
(
echo [req]
echo prompt                 = no
echo distinguished_name     = dn
echo req_extensions         = req_ext

echo [ dn ]
echo countryName            = RU
echo stateOrProvinceName    = Test
echo localityName           = Test
echo organizationName       = Apache
echo commonName             = %SERVER_DOMAIN_NAME%
echo organizationalUnitName = IT
echo emailAddress           = %SERVER_EMAIL%

echo [ req_ext ]
echo subjectAltName         = @alt_names

echo [ alt_names ]
echo DNS.1                  = %SERVER_DOMAIN_NAME%
) > "server.cnf"

:: Generate client config.
(
echo [req]
echo prompt                 = no
echo distinguished_name     = dn
echo req_extensions         = req_ext

echo [ dn ]
echo countryName            = RU
echo stateOrProvinceName    = Test
echo localityName           = Test
echo organizationName       = Apache
echo commonName             = %CLIENT_DOMAIN_NAME%
echo organizationalUnitName = IT
echo emailAddress           = %CLIENT_EMAIL%

echo [ req_ext ]
echo subjectAltName         = @alt_names

echo [ alt_names ]
echo DNS.1                  = %CLIENT_DOMAIN_NAME%
) > "client.cnf"

:: Generate certificates.
openssl genrsa -des3 -passout pass:%PWD% -out server.key 1024
openssl req -new -passin pass:%PWD% -key server.key -config server.cnf -out server.csr

openssl req -new -newkey rsa:1024 -nodes -keyout ca.key -x509 -days 365 -config server.cnf -out ca.crt

openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -extensions req_ext -extfile server.cnf -out server.crt
openssl rsa -passin pass:%PWD% -in server.key -out server.nopass.key

openssl req -new -utf8 -nameopt multiline,utf8 -newkey rsa:1024 -nodes -keyout client.key -config client.cnf -out client.csr
openssl x509 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 02 -out client.crt

openssl pkcs12 -export -in server.crt -inkey server.key -certfile server.crt -out server.p12 -passin pass:%PWD% -passout pass:%PWD%
openssl pkcs12 -export -in client.crt -inkey client.key -certfile ca.crt -out client.p12 -passout pass:%PWD%
openssl pkcs12 -export -in ca.crt -inkey ca.key -certfile ca.crt -out ca.p12 -passout pass:%PWD%

keytool -importkeystore -srckeystore server.p12 -srcstoretype PKCS12 -destkeystore server.jks -deststoretype JKS -noprompt -srcstorepass %PWD% -deststorepass %PWD%
keytool -importkeystore -srckeystore client.p12 -srcstoretype PKCS12 -destkeystore client.jks -deststoretype JKS -noprompt -srcstorepass %PWD% -deststorepass %PWD%
keytool -importkeystore -srckeystore ca.p12 -srcstoretype PKCS12 -destkeystore ca.jks -deststoretype JKS -noprompt -srcstorepass %PWD% -deststorepass %PWD%

openssl x509 -text -noout -in server.crt
openssl x509 -text -noout -in client.crt
openssl x509 -text -noout -in ca.crt
