# jWormhole Client

This is the client code for the JVM- and SSH-based localhost tunnel tool, jWormhole. For more info
about jWormhole, see the [jWormhole server](https://github.com/vvasabi/jwormhole-client) repo.


## Requirements

* jWormhole server (obviously)
* Java 8 (due to the use of Lambda expressions)


## Installation

* Compile with `mvn package`.
* Move `target/jwormhole-client-1.0-SNAPSHOT-all-deps.jar` and `scripts/jwh` to any directory on
  your `$PATH`, such as `/usr/local/jwormhole/bin` or `$HOME/bin`.
* Create a configuration file (see the next section).


## Configuration

Below is the default configuration. Create a file at $HOME/.jwormhole/client.properties with the
following content. Uncomment options that need to be overridden.

```
# Connection to jWormhole server
#jwormhole.client.default.serverSshHost =
#jwormhole.client.default.serverSshPort = 22
#jwormhole.client.default.serverUsername =
#jwormhole.client.default.serverControllerPort = 12700

# Time to send keepalive message or reestablish broken connections in seconds
#jwormhole.client.default.keepaliveInterval = 20
```

The above lines create a default server configuration. That is, jWormhole will connect to this
server if no `-s` argument is specified. To configure more servers, add the following lines for each
server:

```
#jwormhole.client.serverName.serverSshHost =
#jwormhole.client.serverName.serverSshPort = 22
#jwormhole.client.serverName.serverUsername =
#jwormhole.client.serverName.serverControllerPort = 12700
#jwormhole.client.serverName.keepaliveInterval = 20
```

Replace `serverName` with any name desired. Then, the server can be specified using the `-s`
argument. For example, `jwh -s serverName 8080`.


## Usage
Start jWormhole by specifying the local port that needs to be proxied:

```
jwh <port>
```

Custom host name (the wildcard part of a domain) can be specified using `-n` argument:

```
jwh -n xyz <port>
```

To specify a jWormhole server other than the default one, use `-s` argument. See the configuration
section.


## License

```
  Copyright 2014 Brad Chen

  Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
