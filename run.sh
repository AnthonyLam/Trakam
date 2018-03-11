#!/bin/bash

MJPG_PATH=/home/pi/mjpg-streamer/mjpg-streamer-experimental
export LD_LIBRARY_PATH=$MJPG_PATH

# Make directories
mkdir logs
mkdir img

# Build and run go server
printf "Running go server\n"
/home/pi/Trakam/server/server 1&> ./go.log &
echo $!

# Run python stuff
printf "Running python program\n"
source /home/pi/Trakam/lib/venv/bin/activate
python3 /home/pi/Trakam/lib/protocol.py 1&> ./python.log &
echo $!

printf "Running mjpg streamer\n"
$MJPG_PATH/mjpg_streamer -i "input_file.so -d 0.01 -f /var/img/" -o "output_http.so -l 0.0.0.0 -p 8090 -w ./www" &
echo $!
