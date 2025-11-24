# Programmatically Adding an nsec to nsecBunker

## Overview

This document explains how nsecBunker manages keys and provides methods for programmatically adding nsec keys to the system.

## What is nsecBunker?

nsecBunker is a self-hosted daemon that provides decentralized key delegation for the Nostr protocol. It operates as a remote event-signing service that:

- Stores encrypted private keys (nsecs) on behalf of users
- Provides granular permission controls over who can use these keys
- Supports policy-based access control with time-limited tokens
- Enables remote key management through a secure admin interface

## Architecture

### Key Storage

nsecBunker uses a **dual-storage architecture**:

1. **Config File (JSON)**: Stores encrypted nsec keys
   - Location: `config/nsecbunker.json` (default)
   - Encryption: AES-256-CBC with SHA-256 hashed passphrase
   - Structure: `{ keys: { [keyName]: { iv: string, data: string } } }`

2. **Database (SQLite/Prisma)**: Stores key metadata and access control
   - Key metadata (keyName, pubkey, timestamps)
   - KeyUser relationships (which users can access which keys)
   - Policies, tokens, and signing conditions
   - Audit logs

### Key Lifecycle

```
1. Add Key → Encrypt with passphrase → Store in config file
2. Start Daemon → Load encrypted keys from config
3. Unlock Key → Decrypt with passphrase → Load into memory
4. Sign Events → Use decrypted key for signing
```

## Existing Methods to Add Keys

### 1. CLI Interactive Mode (Current)

```bash
npm run nsecbunkerd -- add --name "my-key"
```

This prompts interactively for:
- Passphrase to encrypt the key
- The nsec to add

**Source:** `src/commands/add.ts`

### 2. Remote Admin Interface

The admin interface can create keys remotely via NIP-46 RPC:

```typescript
// Method: create_new_key
// Params: [keyName, passphrase, nsec?]
```

If no nsec is provided, it generates a new one.

**Source:** `src/daemon/admin/commands/create_new_key.ts`

## Programmatic Key Addition

### No Code Changes Required

The existing codebase already exports the necessary function for programmatic key addition: `saveEncrypted`

### Option 1: Using the Exported Function

Create a script that imports and uses the existing `saveEncrypted` function:

```typescript
// add-key-programmatic.ts
import { saveEncrypted } from './src/commands/add.js';

async function addKeyProgrammatically(
    configPath: string,
    nsec: string,
    passphrase: string,
    keyName: string
) {
    try {
        await saveEncrypted(configPath, nsec, passphrase, keyName);
        console.log(`Successfully added key: ${keyName}`);
    } catch (error) {
        console.error(`Failed to add key: ${error.message}`);
        throw error;
    }
}

// Usage
const CONFIG_PATH = 'config/nsecbunker.json';
const NSEC = 'nsec1...'; // Your nsec
const PASSPHRASE = 'your-secure-passphrase';
const KEY_NAME = 'my-automated-key';

addKeyProgrammatically(CONFIG_PATH, NSEC, PASSPHRASE, KEY_NAME)
    .then(() => console.log('Done!'))
    .catch(err => console.error(err));
```

### Option 2: Using the Admin RPC Interface

If nsecBunker is already running, you can use the admin interface remotely:

```typescript
import NDK, { NDKPrivateKeySigner, NDKNostrRpc } from '@nostr-dev-kit/ndk';

async function addKeyViaRPC(
    adminNsec: string,
    bunkerNpub: string,
    keyName: string,
    passphrase: string,
    nsecToAdd?: string
) {
    const ndk = new NDK({
        explicitRelayUrls: ['wss://relay.nsecbunker.com'],
        signer: new NDKPrivateKeySigner(adminNsec)
    });

    await ndk.connect();

    const rpc = new NDKNostrRpc(ndk, ndk.signer!);

    const params = nsecToAdd
        ? [keyName, passphrase, nsecToAdd]
        : [keyName, passphrase];

    const response = await rpc.sendRequest(
        bunkerNpub,
        'create_new_key',
        params,
        24134
    );

    console.log('Key created:', response);
    return response;
}
```

### Option 3: Direct Config Manipulation

You can also manually manipulate the config file:

```typescript
import crypto from 'crypto';
import fs from 'fs';

function encryptNsec(nsec: string, passphrase: string): { iv: string, data: string } {
    const algorithm = 'aes-256-cbc';
    const key = crypto.createHash('sha256').update(passphrase).digest();
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv(algorithm, key, iv);
    let encrypted = cipher.update(nsec);
    encrypted = Buffer.concat([encrypted, cipher.final()]);

    return {
        iv: iv.toString('hex'),
        data: encrypted.toString('hex'),
    };
}

function addKeyDirectly(configPath: string, keyName: string, nsec: string, passphrase: string) {
    // Read existing config
    const configData = fs.readFileSync(configPath, 'utf8');
    const config = JSON.parse(configData);

    // Encrypt the nsec
    const { iv, data } = encryptNsec(nsec, passphrase);

    // Add to config
    config.keys = config.keys || {};
    config.keys[keyName] = { iv, data };

    // Write back
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2));

    console.log(`Key ${keyName} added to config`);
}
```

## Database Registration

Note that adding a key to the config file does **not** automatically create a database record. The database record is created when:

1. The key is unlocked/loaded during daemon startup
2. A user attempts to use the key and a KeyUser record is created

To create the database record programmatically:

```typescript
import { PrismaClient } from '@prisma/client';
import { nip19, getPublicKey } from 'nostr-tools';

const prisma = new PrismaClient();

async function registerKeyInDatabase(keyName: string, nsec: string) {
    // Decode nsec to get the private key
    const decoded = nip19.decode(nsec);
    const privkey = decoded.data as string;

    // Derive the public key
    const pubkey = getPublicKey(privkey);

    // Create database record
    await prisma.key.create({
        data: {
            keyName: keyName,
            pubkey: pubkey
        }
    });

    console.log(`Database record created for ${keyName}`);
}
```

## Complete Example: Full Key Addition

Here's a complete script that adds a key both to the config and database:

```typescript
import { saveEncrypted } from './src/commands/add.js';
import { PrismaClient } from '@prisma/client';
import { nip19 } from 'nostr-tools';
import { getPublicKey } from 'nostr-tools';

const prisma = new PrismaClient();

async function addKeyComplete(
    configPath: string,
    keyName: string,
    nsec: string,
    passphrase: string,
    registerInDb: boolean = true
) {
    // Step 1: Save encrypted key to config
    await saveEncrypted(configPath, nsec, passphrase, keyName);
    console.log(`✓ Key encrypted and saved to config`);

    if (registerInDb) {
        // Step 2: Register in database
        const decoded = nip19.decode(nsec);
        const privkey = decoded.data as string;
        const pubkey = getPublicKey(privkey);

        await prisma.key.upsert({
            where: { keyName: keyName },
            update: { pubkey: pubkey },
            create: {
                keyName: keyName,
                pubkey: pubkey
            }
        });

        console.log(`✓ Key registered in database`);
    }

    console.log(`✓ Key ${keyName} fully configured`);
}

// Usage
addKeyComplete(
    'config/nsecbunker.json',
    'automated-key-1',
    'nsec1...',
    'secure-passphrase',
    true
).then(() => {
    console.log('Complete!');
    process.exit(0);
}).catch(err => {
    console.error('Error:', err);
    process.exit(1);
});
```

## Security Considerations

1. **Passphrase Security**: The passphrase is used to derive the encryption key via SHA-256. Store passphrases securely.

2. **Config File Permissions**: Ensure the config file has appropriate permissions (e.g., `chmod 600`).

3. **Memory Security**: When the daemon is running, decrypted keys are held in memory. Ensure the host system is secure.

4. **Nsec Validation**: Always validate the nsec format before adding:
   ```typescript
   import { nip19 } from 'nostr-tools';

   function validateNsec(nsec: string): boolean {
       try {
           const decoded = nip19.decode(nsec);
           return decoded.type === 'nsec';
       } catch {
           return false;
       }
   }
   ```

## Starting Keys After Addition

After adding a key programmatically, you have two options:

### Option 1: Restart the Daemon

```bash
npm run nsecbunkerd start --admin <your-admin-npub>
```

This will prompt for passphrases for all locked keys.

### Option 2: Unlock via Admin Interface

If the daemon is running, use the `unlock_key` RPC method:

```typescript
await rpc.sendRequest(
    bunkerNpub,
    'unlock_key',
    [keyName, passphrase],
    24134
);
```

## Summary

**Code changes required:** ❌ No

**Methods available:**
1. Use exported `saveEncrypted` function (recommended)
2. Use admin RPC interface (for running daemons)
3. Direct config file manipulation (advanced)

**Best practice workflow:**
1. Use `saveEncrypted` to add the key to config
2. Optionally create database record with Prisma
3. Restart daemon or unlock via RPC
4. Key is ready to use

The existing codebase is well-structured for programmatic key management without requiring any modifications.
