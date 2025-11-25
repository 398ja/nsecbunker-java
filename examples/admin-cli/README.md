# Admin CLI Example

Minimal command-line admin example showing key listing.

## Usage
```bash
export BUNKER_PUBKEY=npub1...
export ADMIN_PRIVKEY=nsec1...
export RELAY_URL=wss://relay.example.com

mvn -q -pl nsecbunker-admin -am compile
java -cp target/classes:../nsecbunker-admin/target/classes \
  xyz.tcheeric.nsecbunker.examples.AdminCliExample list-keys
```
