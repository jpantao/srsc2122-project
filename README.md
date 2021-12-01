# srsc2122-project (PA1)
> A secure “pay-per-view” real-time media streaming system

## Working
- SAPKDP complete specefication working and tested. Some parameters can even be configured. Furthermore the SignalingServer supports multiple connections/clients.


## TODO
- Fix integrity checks (currently they are only done over the payload excluding signatures)
- Voucher blacklist
- Nonce blacklist
- Replace keystore files on code

---

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
java -cp target/srsc-project.jar strserver.hjStreamServer <port>
```

### ProxyBox
```
java -cp target/srsc-project.jar proxybox.ProxyBox -user <username> -password <pwd> -keystore <keystore-file> -proxyinfo <proxyinfo-file>
```

### Generate voucher
```
java -cp target/srsc-project.jar extra.VoucherMinter
```
