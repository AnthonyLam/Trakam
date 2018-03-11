MJPG_PATH=/home/pi/mjpg-streamer/mjpg-streamer-experimental
export LD_LIBRARY_PATH=$MJPG_PATH

$MJPG_PATH/mjpg_streamer -i "input_http.so -p 4081" -o "output_http.so -l 0.0.0.0 -p 8090 -w ./www"
