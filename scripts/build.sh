#!/usr/bin/env bash

sbt clean compile test debian:packageBin normalisePackageName

set -e

(
    cd cdk
    bun install --frozen-lockfile
    bun lint
    bun test
    bun synth
)