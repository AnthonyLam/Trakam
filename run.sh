#!/bin/bash

# Make directories
mkdir logs
mkdir img

# Build and run go server
printf "Running go server\n"
./server/server &> ./go.log
echo $!

# Run python stuff
printf "Running python program\n"
source lib/venv/bin/activate
python3 lib/protocol.py &> ./python.log
echo $1
