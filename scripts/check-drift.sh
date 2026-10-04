#!/usr/bin/env bash
# Every source file that imports nothing from Minecraft, Fabric, Mod Menu or
# LWJGL is pure logic and must be byte-identical in all version directories,
# and so must the lang files.
# Fixing a parser bug in one copy and forgetting the others fails the build.
set -euo pipefail
cd "$(dirname "$0")/.."

ref=versions/1.21.11
others=()
for dir in versions/*/; do
  dir=${dir%/}
  [ "$dir" = "$ref" ] || others+=("$dir")
done

fail=0
checked=0
while IFS= read -r -d '' file; do
  if [[ "$file" == *.java ]] && grep -qE '^import (static )?(net\.minecraft|net\.fabricmc|com\.terraformersmc|org\.lwjgl|com\.mojang)' "$file"; then
    continue
  fi
  checked=$((checked + 1))
  rel=${file#"$ref"/}
  for other in "${others[@]}"; do
    if ! cmp -s "$file" "$other/$rel"; then
      echo "drift: $rel differs in $other (or is missing there)"
      fail=1
    fi
  done
done < <(find "$ref/src/main/java" "$ref/src/test/java" -name '*.java' -print0
         find "$ref/src/main/resources/assets" -name '*.json' -print0)

if [ "$fail" -eq 0 ]; then
  echo "no drift in $checked pure files across: $ref ${others[*]}"
fi
exit "$fail"
