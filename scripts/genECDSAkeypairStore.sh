#!/bin/bash

keytool -genkeypair -groupname secp256r1 -alias $1 -keyalg EC -sigalg SHA256withECDSA -storetype pkcs12 -keystore ./keystores/$1.keystore -storepass srsc2122 \
	-dname "CN=PayPerView Streaming, OU=SRSC-2122, O=NOVA-STT, L=Caparica, ST=Almada, C=PT"
