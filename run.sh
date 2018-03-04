#!/bin/bash

# Make directories
mkdir logs
mkdir img

# Build and run go server
printf "Running go server\n"
cd server
go build
cd ..
./server/server &
echo $!

# Run python stuff
printf "Running python program\n"
source lib/venv/bin/activate
python3 lib/protocol.py &
echo $1