# cs7ns6-groupwork

This is a groupwork for CS7NS6 - Distributed Systems, 2020.

- [cs7ns6-groupwork](#cs7ns6-groupwork)
  - [Introduciton](#introduciton)
  - [Requirements](#requirements)
  - [Specifications](#specifications)
    - [Architecture](#architecture)
  - [Implementation](#implementation)
  - [Individual Contribution](#individual-contribution)
  - [Summary](#summary)
  - [How](#how)
    - [Compile](#compile)
    - [Run](#run)
    - [Caveats](#caveats)
    - [Client Command](#client-command)
  - [Test](#test)
  - [Personal Contribution](#personal-contribution)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>


## Introduciton

This project aim to implement a distributed key-value store based on the RAFT algorithm.

A introduction about the RAFT algorithm can be found [here](https://raft.github.io/).

The original paper is [here](https://raft.github.io/raft.pdf) with the bibliography.

> Ongaro, Diego, and John Ousterhout. "In search of an understandable consensus algorithm." 2014 {USENIX} Annual Technical Conference ({USENIX}{ATC} 14). 2014.

```bib
@misc{ongaro2013search,
  title={In search of an understandable consensus algorithm (extended version)},
  author={Ongaro, Diego and Ousterhout, John},
  year={2013},
  publisher={Tech Report. May, 2014. http://ramcloud. stanford. edu/Raft. pdf}
}
```

## Requirements

## Specifications

Graph drawing using [diagrams.net (draw.io)](https://app.diagrams.net/).

The app has a google drive version and a desktop version.

We choose to keep `drawio` files inside the project repository.

### Architecture

[see doc/drawio-architecture.xml](doc/drawio-architecture.xml)

As you can see, there are mainly 4 modules and 2 loops running in a node.

## Implementation

## Individual Contribution

## Summary

## How

### Compile

Please use at least `jdk 1.8` to compile the project, with the MAVEN (`mvn`) toolchain installed.

To compile, run the compiling script `compile.sh` in the root of the project folder, a jar file `KVNode.jar` will appear at the root.

```text
$ sh compile.sh
```

### Run

The builds of the code are divided into:

- client: with `-c` flag
- node (server): without `-c` flag and with valid configs

Use `-h` or `--help` command to access the help.

```text
$ java -jar KVNode.jar -h
usage: KVNode.jar
 -c,--client              Initiate as a client.
 -h,--help                Help
 -o,--host <arg>          The host of server
 -p,--port <arg>          The port of server listening to
 -t,--target-host <arg>   The host of target server to join, omit this or
                          target-port to establish a new group (as the first and
                          the leader)
 -y,--target-port <arg>   The port of target server to join, omit this or
                          target-host to establish a new group (as the first and
                          the leader)
```

To run client, use:

```text
$ KVNODENAME=CLIENT java -jar KVNode.jar -c
```

To run a server node, use: (where `4111` is the port listening, while `4110` is the target/leader port)

```text
$ KVNODENAME=4111 java -jar KVNode.jar -p 4111 -y 4110
```

The host/IP `localhost` is omitted.

The ENV variable `KVNODENAME` is for logs (both program log and log entries) and data persistence on the file system:

- `KVNODENAME.log`: program log / output
- `KVNODENAME-error.log`: program log on error level
- `KVNODENAME_state/`: data storage
- `KVNODENAME_log/`: storage for log entries

### Caveats

Do notice to start nodes **ONE BEFORE OTHERS** with LEADER - FOLLOWER order to establish the cluster.

For example:

1. start a server on port `4111` first (without the flag `-y`): `KVNODENAME=4111 java -jar KVNode.jar -p 4111`
2. Until `4111` is ready as a **LEADER**, start another server (`4112`) using: `KVNODENAME=4112 java -jar KVNode.jar -p 4112 -y 4111`
3. Repeat step 2 to add more nodes

### Client Command

Program in client mode will read the lines input and parse the command.

`ip:port` is to determine which SERVER/NODE is going to communicate:

- `ip:port get KEY`
  - get a value using the `KEY`
- `ip:port set KEY VALUE`
  - set `VALUE` to the `KEY`
- `ip:port del KEY`
  - delete using the `KEY`
- `ip:port setFail ip2:port2`
  - give no response (fail the request handling) to the server `ip2:port2`

For example, getting a value of key `foo` from a node(`localhost:4111`):

```text
localhost:4111 get foo
```

## Test

1. Start a 3-instance cluster with leader first decided, we can see without specifying the complete list of nodes/peers the cluster still can be built. And nodes in service can be terminated as you want. A simplified (also dangerous) dynamic management based on detecting connection failures (e.g. heartbeat) is implemented.

    ```text
    $ KVNODENAME=4111 java -jar KVNode.jar -p 4111          # as leader
    $ KVNODENAME=4112 java -jar KVNode.jar -p 4112 -y 4111
    $ KVNODENAME=4113 java -jar KVNode.jar -p 4113 -y 4111
    ```

2. Start a 3-instance cluster with leader first decided, use the client to execute the command *one by one*:

    ```text
    localhost:4112 get foo
    localhost:4111 set foo bar
    localhost:4113 get foo
    ```

    We can see that we can get / set on different nodes.

    ```text
    ...
    12:19:37.872 [pool-1-thread-1] INFO  life.tannineo.cs7ns6.App - You just input:
    localhost:4112 get foo
    12:19:43.045 [pool-1-thread-1] INFO  life.tannineo.cs7ns6.App - request content : foo, url : localhost:4112, put response : ClientKVAck(result=null)
    12:19:43.046 [pool-1-thread-1] INFO  life.tannineo.cs7ns6.App - get foo=null
    localhost:4111 set foo bar
    12:19:49.792 [pool-1-thread-2] INFO  life.tannineo.cs7ns6.App - You just input:
    localhost:4111 set foo bar
    12:19:49.849 [pool-1-thread-2] INFO  life.tannineo.cs7ns6.App - request content : foo=bar, url : localhost:4111, put response : ClientKVAck(result=ok)
    12:19:49.849 [pool-1-thread-2] INFO  life.tannineo.cs7ns6.App - set foo ok
    localhost:4113 get foo
    12:19:54.990 [pool-1-thread-3] INFO  life.tannineo.cs7ns6.App - You just input:
    localhost:4113 get foo
    12:19:55.007 [pool-1-thread-3] INFO  life.tannineo.cs7ns6.App - request content : foo, url : localhost:4113, put response : ClientKVAck(result=Command(opt=null, key=foo, value=bar))
    12:19:55.007 [pool-1-thread-3] INFO  life.tannineo.cs7ns6.App - get foo=bar
    ```

3. Close `localhost:4111`, wait for about 10 sec (election task timeout), the rest 2 nodes will go for election. Then update data on `localhost:4112`:

    ```text
    localhost:4112 set foo barbar
    ```

    When successed, start `localhost:4111` to join the cluster as a follower: `KVNODENAME=4111 java -jar KVNode.jar -p 4111 -y ?WHOSTHELEADER?`, we can see the index persist and then updated by 1 when join the cluster (because we did one change). Then execute query on client:

    ```text
    localhost:4111 get foo
    ```

    We can get `barbar`, the new data.

    ```text
    ...
    localhost:4111 get foo
    12:07:42.285 [pool-1-thread-2] INFO life.tannineo.cs7ns6.App - You just input:
    localhost:4111 get foo
    12:07:42.308 [pool-1-thread-2] INFO  life.tannineo.cs7ns6.App - request content : foo, url : localhost:4111, put response : ClientKVAck(result=Command(opt=null, key=foo, value=barbar))
    12:07:42.308 [pool-1-thread-2] INFO  life.tannineo.cs7ns6.App - get foo=barbar
    ```

4. For partition, we test it on a 5-instance cluster. **Remove all the disk files generated for clean experiment**, and start 5 nodes.

    ```text
    $ KVNODENAME=4111 java -jar KVNode.jar -p 4111          # as leader
    $ KVNODENAME=4112 java -jar KVNode.jar -p 4112 -y 4111
    $ KVNODENAME=4113 java -jar KVNode.jar -p 4113 -y 4111
    $ KVNODENAME=4114 java -jar KVNode.jar -p 4114 -y 4111
    $ KVNODENAME=4115 java -jar KVNode.jar -p 4115 -y 4111
    ```

    In the client, we execute the commands in batch (the client command executor is also implemented concurrently, and can parse multiple lines).

    ```text
    localhost:4111 setFail localhost:4113
    localhost:4111 setFail localhost:4114
    localhost:4111 setFail localhost:4115
    localhost:4112 setFail localhost:4113
    localhost:4112 setFail localhost:4114
    localhost:4112 setFail localhost:4115
    localhost:4113 setFail localhost:4111
    localhost:4113 setFail localhost:4112
    localhost:4113 setFail localhost:4115
    localhost:4114 setFail localhost:4111
    localhost:4114 setFail localhost:4112
    localhost:4114 setFail localhost:4115
    localhost:4115 setFail localhost:4111
    localhost:4115 setFail localhost:4112
    localhost:4115 setFail localhost:4113
    localhost:4115 setFail localhost:4114
    ```

    The node are divided into `4111 & 4112 | 4113 & 4114 | 4115`. n = 3

    ```text
    ...
    12:45:31.059 [pool-1-thread-10] INFO  life.tannineo.cs7ns6.App - setFail localhost:4111 ok -> true
    12:45:31.059 [pool-1-thread-4] INFO  life.tannineo.cs7ns6.App - setFail localhost:4113 ok -> true
    12:45:31.059 [pool-1-thread-11] INFO  life.tannineo.cs7ns6.App - setFail localhost:4112 ok -> true
    12:45:31.059 [pool-1-thread-7] INFO  life.tannineo.cs7ns6.App - setFail localhost:4113 ok -> true
    12:45:31.059 [pool-1-thread-12] INFO  life.tannineo.cs7ns6.App - setFail localhost:4115 ok -> true
    12:45:31.060 [pool-1-thread-5] INFO  life.tannineo.cs7ns6.App - setFail localhost:4114 ok -> true
    12:45:31.060 [pool-1-thread-8] INFO  life.tannineo.cs7ns6.App - setFail localhost:4114 ok -> true
    12:45:31.060 [pool-1-thread-9] INFO  life.tannineo.cs7ns6.App - setFail localhost:4115 ok -> true
    12:45:31.060 [pool-1-thread-6] INFO  life.tannineo.cs7ns6.App - setFail localhost:4115 ok -> true
    12:45:31.074 [pool-1-thread-15] INFO  life.tannineo.cs7ns6.App - setFail localhost:4115 ok -> true
    12:45:31.075 [pool-1-thread-16] INFO  life.tannineo.cs7ns6.App - setFail localhost:4111 ok -> true
    12:45:31.075 [pool-1-thread-14] INFO  life.tannineo.cs7ns6.App - setFail localhost:4112 ok -> true
    12:45:31.075 [pool-1-thread-13] INFO  life.tannineo.cs7ns6.App - setFail localhost:4111 ok -> true
    12:45:31.075 [pool-1-thread-17] INFO  life.tannineo.cs7ns6.App - setFail localhost:4112 ok -> true
    12:45:31.075 [pool-1-thread-18] INFO  life.tannineo.cs7ns6.App - setFail localhost:4113 ok -> true
    12:45:31.319 [pool-1-thread-19] INFO  life.tannineo.cs7ns6.App - setFail localhost:4114 ok -> true

    ```

    `true` means start blocking. Execute the same command again to turn it into `false` (like a switch).

    Wait for election happens among the nodes.

    Partition merging features are not implemented.

## Personal Contribution
