#!/usr/bin/env bash
echo -e "\n\n\n\n\n\n\n\n"

sbt "test:runMain Pipeline.PipelineLauncher $1"

