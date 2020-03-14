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

Use `-h` or `--help` command to access the help.

```text
$ java -jar KVNode.jar -h
usage: KVNode.jar
 -h,--help                Help
 -n,--name <arg>          The name of the server, omit to use a timestamped
                          name. The server will read the persistent files (data,
                          logs) due to the name.
 -o,--host <arg>          The host of server
 -p,--port <arg>          The port of server listening to
 -t,--target-host <arg>   The host of target server to join, omit this or
                          target-port to establish a new group (as the first and
                          the leader)
 -y,--target-port <arg>   The port of target server to join, omit this or
                          target-host to establish a new group (as the first and
                          the leader)
```


### Test

## Design

Graph drawing using [diagrams.net (draw.io)](https://app.diagrams.net/).

The app has a google drive version and a desktop version.

We choose to keep files inside the project repository.

## About
