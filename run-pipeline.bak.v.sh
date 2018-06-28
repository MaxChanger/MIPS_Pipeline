#!/usr/bin/env bash

TESTER_BACKENDS=verilator sbt "test:runMain Pipeline.PipelineLauncher Top $1"
