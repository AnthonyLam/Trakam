#!/bin/bash

# Make directories
mkdir logs
mkdir img

# Build and run go server
printf "Running go server"
go build ./server
./Trakam &
echo $!

# Run python stuff
printf "Running python program"
source lib/venv/bin/activate
python3 lib/protocol.py &
echo $1