#!/bin/bash

declare -A usedMods

for i in $1
do
  modlist=$($JAVA11_HOME/bin/jdeps -q --ignore-missing-deps --multi-release 11 --print-module-deps $i)
  while IFS=',' read -ra mods; do
      for mod in "${mods[@]}"; do
          usedMods[$mod]=true
      done
  done <<< "$modlist"
done

for KEY in "${!usedMods[@]}"; do
  printf "%s," $KEY
done

echo "java.compiler,jdk.compiler,jdk.crypto.cryptoki,jdk.scripting.nashorn"