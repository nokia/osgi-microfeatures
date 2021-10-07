#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <target instance name>"
    exit 1
fi

if [ ! -d instance ]; then
    echo "instance not found."
    exit 1
fi

if [ -d $1 ]; then
    echo "instance already exists: $1"
    exit 1
fi

target=$1

cp -r instance $target
sed -i "s/instance\.name=instance/instance\.name=$target/g" $target/system.cfg
