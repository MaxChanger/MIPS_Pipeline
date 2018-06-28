#!/usr/bin/env bash

TESTER_BACKENDS=verilator sbt "test:runMain Pipeline.PipelineLauncher TopBP $1"
