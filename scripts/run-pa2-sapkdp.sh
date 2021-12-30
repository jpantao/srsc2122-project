#!/bin/bash


(java -cp target/srsc-project-PA2.jar sigserver.SignalingServer > /dev/null) &
sleep 3
(java -cp target/srsc-project-PA2.jar proxybox.ProxyBox -sapkdp_only -user jpantao -password password -keystore keystores/proxybox.keystore -proxyinfo config/proxybox.properties -movie $1 -voucher $2) > /dev/null

