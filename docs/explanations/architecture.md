# Architecture Overview

```mermaid
flowchart TD
  A[Application] --> B[NsecBunker Admin Client]
  A --> C[NsecBunker Signer]

  subgraph Admin
    B --> B1[KeyManager]
    B --> B2[PolicyManager]
    B --> B3[PermissionManager]
    B --> B4[TokenManager]
  end

  subgraph Signer
    C --> C1[RequestExecutor]
    C --> C2[BatchSigner]
  end

  B & C --> D[NIP-46 Protocol]
  D --> E[Relays]
  E --> F[nsecBunker]
```

## Modules
- `nsecbunker-admin`: admin ops over NIP-46 (kind 24134)
- `nsecbunker-client`: remote signer, batching, request queue/executor
- `nsecbunker-account`: account/NIP-05/wallet stubs
- `nsecbunker-spring-boot-starter`: auto-config, health/info

## Cross-Cutting
- Models in `nsecbunker-core`
- Connection utilities in `nsecbunker-connection`
- Protocol helpers in `nsecbunker-protocol`
