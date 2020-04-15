# cs7ns6-groupwork

This is a groupwork for CS7NS6 - Distributed Systems, 2020.

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

## How

### Compile

Using `openjdk 12.0`.

To compile, run the compiling script in the root of the project folder, a jar file `KVNode.jar` will appear at the root.

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

The host `localhost` is omitted.

The env variable `KVNODENAME` is for logs and data persistence on the file system.

### Client Command

`ip:port` is to determine which SERVER/NODE is going to communicate:

- `ip:port get KEY`
  - get a value using the `KEY`
- `ip:port set KEY VALUE`
  - set `VALUE` to the `KEY`
- `ip:port del KEY`
  - delete using the `KEY`
- `ip:port setFail ip2:port2`
  - give no response (fail the request handling) to the server `ip2:port2`

For example, getting a value of key `foo` from a node:

```text
localhost:4111 get foo
```

## Design

Graph drawing using [diagrams.net (draw.io)](https://app.diagrams.net/).

The app has a google drive version and a desktop version.

We choose to keep `drawio` files inside the project repository.

### Architecture

[see doc/drawio-architecture.xml](doc/drawio-architecture.xml)

As you can see from the , there are mainly 4 modules and 2 loops running in a node.

#### Architecture - StateMachine

#### Architecture - LogModule

#### Architecture - Consensus

#### Architecture - Communication

#### Architecture - Loops

## Test

1. start a 3-instance cluster

```
localhost:4111 setFail localhost:4114
localhost:4112 setFail localhost:4114
localhost:4113 setFail localhost:4114
localhost:4114 setFail localhost:4111
localhost:4114 setFail localhost:4112
localhost:4114 setFail localhost:4113

```

2.

```
localhost:4111 setFail localhost:4115
localhost:4111 setFail localhost:4114
localhost:4112 setFail localhost:4115
localhost:4112 setFail localhost:4114
localhost:4113 setFail localhost:4115
localhost:4113 setFail localhost:4114
localhost:4114 setFail localhost:4111
localhost:4114 setFail localhost:4112
localhost:4114 setFail localhost:4113
localhost:4115 setFail localhost:4111
localhost:4115 setFail localhost:4112
localhost:4115 setFail localhost:4113

```

## About
