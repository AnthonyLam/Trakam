#!/bin/bash

# Make directories
mkdir logs
mkdir img

# Build and run go server
printf "Running go server\n"
./server/server 1&> ./go.log &
echo $!

# Run python stuff
printf "Running python program\n"
source lib/venv/bin/activate
python3 lib/protocol.py 1&> ./python.log &
echo $!
