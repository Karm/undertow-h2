Dead simple H2 example server for testing the protocol.

Usage:

```
java -Dbind.address=127.0.0.1 -Dbind.port=8444 -Dpassphrase=changeit
 \ -Dkeystore.type=JKS -Dkeystore.path=./localhost.jks -Dtruststore.path=./localhost.jks -jar target/undertow-h2.jar
```
