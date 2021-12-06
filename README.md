# srsc2122-project (PA2)

> Overlayed secure “pay-per-view” real-time media streaming system supported by configurable TLS Channels

## Work Done

### SAPKDP

- SAPKDP complete specification working and tested (only added the movieID field to the ticket);
- Runs over TCP only;
- Some parameters can be configured in the [config file](config/sapkdp.properties);
- SignalingServer supports multiple concurrent connections/clients;
- Alterations:
    - Added the **movieID** field to the ticket.

### RTSTP

- RTSTP complete specification;
- Runs over UDP only;
- The server supports only one movie request.

### Additional Notes

- Registered users' database can be found in [users.json](resources/users.json) ;
- Available movies' database can be found in [movies.json](resources/movies.json);
- Configuration files are under the [config](config) directory;
- Keystore files are under the [keystores](keystores) directory;
- Movie files are under the [movies](movies) directory;

[comment]: <> (- Ciphersuites tested:)

[comment]: <> (    - AES/CTR/NoPadding HmacSHA512.)

---

## Compiling

The following command creates the [srsc-project-PA2.jar](target/srsc-project-PA2.jar) file with all the dependencies:

```
mvn package
```

## Running

> Note: The instruction on this section assume the user is at the [root](.) of the project

### SignalingServer

Default port is 8888. Any changes should also be reflected in the [proxybox.properties](config/proxybox.properties)
file.

```bash
java -cp target/srsc-project.jar sigserver.SignalingServer <port>

# Example:
java -cp target/srsc-project.jar sigserver.SignalingServer
```

### StreamingServer

Default port is 9999. Any changes should also be reflected in the [proxybox.properties](config/proxybox.properties)
file.

```bash
java -cp target/srsc-project-PA2.jar strserver.StreamingServerUDP <port>

# Example:
java -cp target/srsc-project-PA2.jar strserver.StreamingServerUDP 
```

### ProxyBox

The `-movie`, and `-storepass` arguments are optional and their default values are respectively **monsters**, and **
srsc2021**. The multicast address for the mpegplayers can be set in
the [proxybox.properties](config/proxybox.properties) file.

```bash
java -cp target/srsc-project-PA2.jar proxybox.ProxyBox -user <username> -password <pwd> -keystore <keystore-file> -proxyinfo <proxyinfo-file> -movie <movieID> -storepass <keystore-password>

# Example:
java -cp target/srsc-project-PA2.jar proxybox.ProxyBox -user jpantao -password password -keystore keystores/proxybox.keystore -proxyinfo config/proxybox.properties -movie cars -voucher resources/coin_3040021e45931ef.voucher
```

### Generate voucher

```bash
java -cp target/srsc-project-PA2.jar extra.VoucherMinter
```

### With the run-PA2.sh script

Run the three components with the default configs:

```bash
# Available movieID's: cars, and monsters.
./scripts/run-pa2.sh <movieID> <voucherFile>

#Example
./scripts/run-pa2.sh cars resources/coin_3040021e45931ef.voucher
```

A [log](log) directory will be created by the script with the stdout of the 3 components.





