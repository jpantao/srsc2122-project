# srsc2122-project (PA1)
> A secure “pay-per-view” real-time media streaming system

### Compiling
The following command creates the [srsc-project.jar](target/srsc-project.jar) file with all the
dependencies:
```
mvn package
```

### Running

TODO: fix commands

**SignalingServer**:
```
java -cp target/srsc-project.jar sigserver.SignalingServer <port>
```

**StreamServer**:
```
java -cp target/srsc-project.jar strserver.StreamServer <port>
```

**ProxyBox**:
```
java -cp target/srsc-project.jar proxybox.ProxyBox -user <username> -password <pwd> -keystore <keystore-file> -proxyinfo <proxyinfo-file>
```