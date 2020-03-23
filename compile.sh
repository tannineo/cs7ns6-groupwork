#!/bin/bash

mvn clean package

cp ./target/KVNode-1.0-SNAPSHOT.jar ./KVNode.jar
