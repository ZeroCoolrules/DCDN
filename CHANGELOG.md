# Changelog

## 2026-09-30 — Base Sepolia, real on-chain data only

**Why:** the app shipped with demo data. It showed invented balances, proposals and payouts, and it had
Arbitrum and Ethereum addresses that are not DCDN contracts. Several of those addresses belong to other
projects' real mainnet contracts (Uniswap V2 router, USDC, LINK, DAI). Any failed RPC call quietly fell
back to made-up numbers. This release makes every figure in the app come from the chain, or say
"unavailable".

### Changed
- `config/DeploymentConfig.kt`: only real deployments are listed. Base Sepolia has the DCDN token, the
  owner Safe, the treasury and the dev fund. Base mainnet is present but set to "not deployed". The fake
  Arbitrum, Ethereum and Sepolia configs and the unused ABI string table are gone.
- `repository/WalletRepository.kt`: now read-only. The `privateKey` / `RawTransactionManager` signing
  path is removed, so the app never holds a key. There are no fallback values (such as 100M balances,
  a nonce of 14 or simulated hashes). Failures come back as `Result.failure`.
- `ui/screens/TokenAllocationScreen.kt`: the donut chart and table are built from live `totalSupply`,
  `MAX_SUPPLY`, `remainingSupply`, `pendingLockedRewards` and the balances. The slices add up exactly
  to MAX_SUPPLY.
- `ui/screens/GovernanceScreen.kt`: shows the live Safe (owners, threshold, nonce, version) and checks
  that `owner()` is the Safe. It links out to Safe{Wallet} (home, queue, Transaction Builder) for
  proposing and signing. The demo Timelock proposals are removed, because no Timelock is deployed.
- `ui/screens/TreasuryHistoryScreen.kt`: real contract events from the Blockscout API (no key needed),
  decoded locally from topic0. The public RPC limits `eth_getLogs` to 1,000 blocks, so it cannot be
  used for this.
- `MainActivity.kt`: a single `TreasuryViewModel` shared by all tabs. The header shows the live supply,
  and there is a refresh button.

### Added
- `repository/ExplorerRepository.kt`, `viewmodel/TreasuryViewModel.kt`, `ui/components/ChainUi.kt`
- `ui/screens/WalletLookupScreen.kt`: a read-only lookup for any address. It shows the DCDN and ETH
  balance, whether the address is a minter, whether it is a Safe owner, and whether it is exempt from
  the max-wallet limit.
- Tests: config, decoding and allocation unit tests, four Roborazzi screenshots, and an opt-in
  `LiveChainTest` (`LIVE_CHAIN=1`).

### Removed (still available in git history)
- `ui/screens/WalletConnectScreen.kt` ("Hardware Key" tab). Its Scan button never registered an NFC
  reader. The only path that worked was `simulateNfcHandshake()`, which returned a hard-coded signer
  and signature. The biometric step auto-approved when no activity was available. Hardware signing
  belongs in Safe{Wallet}, which supports Ledger and Trezor. `security/NfcProtocolConstants.kt` and
  `BiometricAuthManager.kt` are kept.
- `viewmodel/GovernanceViewModel.kt`, `ui/components/RechartsTokenFlowView.kt` (a WebView chart of
  invented monthly reserves) and `data/model/TokenAllocation.kt`.
