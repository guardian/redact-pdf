#!/usr/bin/env bash

sbt clean compile test debian:packageBin

set -e

(
    cd cdk
    npm ci
    npm run lint
    npm test
    npm run synth
    cd ..
)