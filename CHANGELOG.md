# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-02-24

### Changed

- Upgraded nostr-java from 1.2.0 to 2.0.0
- Migrated dependency artifacts to nostr-java 2.0 module structure:
  - `nostr-java-api` replaced by `nostr-java-core`
  - `nostr-java-crypto` replaced by `nostr-java-core`
  - `nostr-java-id` replaced by `nostr-java-identity`
  - `nostr-java-encryption` replaced by `nostr-java-identity`
  - Removed `nostr-java-base` (merged into core)
- Replaced `PubKeyTag` with `GenericTag` for NIP-46 event tag handling

## [0.1.1] - 2025-01-01

### Fixed

- Handle null passphrase in key management methods
- Address Qodana and SpotBugs static analysis issues
- Update Bottin repository link in README.md

### Changed

- Update Java version requirement to 21

## [0.1.0] - 2025-01-01

- Initial release
