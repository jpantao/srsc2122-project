# srsc2122-project (PA1)
> A secure “pay-per-view” real-time media streaming system

## Questions
* How to better encapsulate the protocols?
* What's the ciphersuite for the PBE in SAPKDP?
* The session key mentioned in the spec is supposed to be the keys associated to each movie?

## Compiling
The following command creates the [srsc-project.jar](target/srsc-project.jar) file with all the
dependencies:
```
mvn package
```

## Running

### SignalingServer
Default port is 8888.
```
java -cp target/srsc-project.jar sigserver.SignalingServer <port>
```

### StreamServer
Default port is 9999.
```
java -cp target/srsc-project.jar strserver.StreamServer <port>
```

## ProxyBox:
```
java -cp target/srsc-project.jar proxybox.ProxyBox -user <username> -password <pwd> -keystore <keystore-file> -proxyinfo <proxyinfo-file>
```