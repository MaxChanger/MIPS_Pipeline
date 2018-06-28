#!/usr/bin/env bash
echo -e "\n\n\n\n\n\n\n\n"

sbt "test:runMain Pipeline.Launcher Top --backend-name verilator"
