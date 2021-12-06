#!/bin/bash

mkdir -p "log"

#read -p "Open the network stream address udp://@224.7.7.7:7777 on vlc and press enter to start."
echo 'Launching VLC...'
($(which vlc) udp://@224.7.7.7:7777 >> /dev/null 2>&1) &

echo 'Launching SignalingServer...'
(java -cp target/srsc-project-PA2.jar sigserver.SignalingServer > log/sigserver.log) &
sleep 3
echo 'Launching StreamingServerUDP...'
(java -cp target/srsc-project-PA2.jar strserver.StreamingServerUDP > log/strserver.log) &
sleep 3
echo 'Launching ProxyBox...'
java -cp target/srsc-project-PA2.jar proxybox.ProxyBox -user jpantao -password password -keystore keystores/proxybox.keystore -proxyinfo config/proxybox.properties -movie $1 -voucher $2 | tee log/proxybox.log



