#!/bin/bash

mvn clean
mvn package

cp ./target/KVNode-1.0-SNAPSHOT.jar ./KVNode.jar
