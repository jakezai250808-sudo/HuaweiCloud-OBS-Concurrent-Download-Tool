#!/usr/bin/env bash
set -euo pipefail

mvn -q -pl master -am test
