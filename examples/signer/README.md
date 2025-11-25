# Simple Signer Example

This example shows how to connect to nsecBunker and sign an event using `NsecBunkerSigner`.

## Usage
1. Set environment variables:
   - `BUNKER_PUBKEY`
   - `CLIENT_PRIVKEY`
   - `RELAY_URL`
2. Run:
```bash
mvn -q -pl nsecbunker-client -am compile
java -cp target/classes:../nsecbunker-client/target/classes \
  xyz.tcheeric.nsecbunker.examples.SignerExample
```
