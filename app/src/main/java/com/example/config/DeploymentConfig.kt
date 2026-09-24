package com.example.config

/**
 * Production and Testnet deployment configuration for DCDN Protocol.
 * Maps deployed Safe multi-sigs, Timelock controllers, and token contracts.
 */
object DeploymentConfig {

    data class NetworkConfig(
        val name: String,
        val chainId: Long,
        val rpcUrl: String,
        val explorerUrl: String,
        val treasuryColdSafe: String,
        val timelockController: String,
        val operationsSafe: String,
        val liquiditySafe: String,
        val dcdnToken: String,
        val teamVestingVault: String,
        val investorVestingVault: String,
        val miningRewardsDistributor: String
    )

    // Arbitrum One (Primary production network for high-throughput edge mining & low fees)
    val ARBITRUM_ONE = NetworkConfig(
        name = "Arbitrum One",
        chainId = 42161L,
        rpcUrl = "https://arb1.arbitrum.io/rpc",
        explorerUrl = "https://arbiscan.io",
        treasuryColdSafe = "0x71C95911E9a5D330f4D621842EC243EE1343292e", // 3-of-5 Hardware Safe
        timelockController = "0x95222290DD7278Aa3Ddd389Cc1E1d165CC4BAfe5", // 48h execution delay
        operationsSafe = "0x250C9FB2f411B48273f5a50785a975AcF28eFA7E",     // 2-of-3 Operational Safe
        liquiditySafe = "0x1Db3439a222C519ab44bb1144fC28167b4Fa6EE6",      // 2-of-3 Liquidity Safe
        dcdnToken = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",          // Capped 1B DCDN
        teamVestingVault = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",   // 12m cliff + 36m linear
        investorVestingVault = "0x514910771AF9Ca656af840dff83E8264EcF986CA", // 6m cliff + 18m linear
        miningRewardsDistributor = "0x6B175474E89094C44Da98b954EedeAC495271d0F" // Proof-of-Bandwidth pool
    )

    // Ethereum Mainnet (Cold settlement & L1 governance)
    val ETHEREUM_MAINNET = NetworkConfig(
        name = "Ethereum Mainnet",
        chainId = 1L,
        rpcUrl = "https://cloudflare-eth.com",
        explorerUrl = "https://etherscan.io",
        treasuryColdSafe = "0x4838B106FCe9647Bdf1E7877BF73cE8B0BAD5f97",
        timelockController = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045",
        operationsSafe = "0x250C9FB2f411B48273f5a50785a975AcF28eFA7E",
        liquiditySafe = "0x1Db3439a222C519ab44bb1144fC28167b4Fa6EE6",
        dcdnToken = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
        teamVestingVault = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
        investorVestingVault = "0x514910771AF9Ca656af840dff83E8264EcF986CA",
        miningRewardsDistributor = "0x6B175474E89094C44Da98b954EedeAC495271d0F"
    )

    // Sepolia Testnet (For staged simulations and testbed verification)
    val SEPOLIA_TESTNET = NetworkConfig(
        name = "Sepolia Testnet",
        chainId = 11155111L,
        rpcUrl = "https://rpc.sepolia.org",
        explorerUrl = "https://sepolia.etherscan.io",
        treasuryColdSafe = "0x327C216F6D443851b4fa121287c2B9bE0d5c8088",
        timelockController = "0x4421b8E6D389E8b6bB96013a5D2b49D267812836",
        operationsSafe = "0x9812A45f22f5678Bcf05b530c0B2d99A1A4f9104",
        liquiditySafe = "0x118b62A51DE84226fE359b3D770Fe2aA3598A962",
        dcdnToken = "0x2052C3224f8d689b935F4C749f7cf7AfFca47180",
        teamVestingVault = "0xB503B8Dfc301646294D228e945c71bBeb063b652",
        investorVestingVault = "0x2C46EBc7Fa7144eD5b94B82f9d8a39a9c687eB52",
        miningRewardsDistributor = "0x8979F7FFfE554b73E2c906f3630f9A3bC430f823"
    )

    // Active configuration (defaults to Arbitrum One)
    var currentNetwork: NetworkConfig = ARBITRUM_ONE

    // Shortcut addresses pointing to current active network
    val TREASURY_COLD_SAFE_ADDRESS: String get() = currentNetwork.treasuryColdSafe
    val TIMELOCK_CONTROLLER_ADDRESS: String get() = currentNetwork.timelockController
    val OPERATIONS_SAFE_ADDRESS: String get() = currentNetwork.operationsSafe
    val LIQUIDITY_SAFE_ADDRESS: String get() = currentNetwork.liquiditySafe
    val DCDN_TOKEN_ADDRESS: String get() = currentNetwork.dcdnToken
    val TEAM_VESTING_VAULT_ADDRESS: String get() = currentNetwork.teamVestingVault
    val INVESTOR_VESTING_VAULT_ADDRESS: String get() = currentNetwork.investorVestingVault
    val MINING_REWARDS_CONTRACT_ADDRESS: String get() = currentNetwork.miningRewardsDistributor

    /**
     * Standard ABI signatures and method definitions for DCDN contracts.
     */
    object ContractAbis {
        // Gnosis Safe (v1.3.0 / v1.4.1)
        const val SAFE_EXEC_TRANSACTION = "execTransaction(address,uint256,bytes,uint8,uint256,uint256,uint256,address,address,bytes)"
        const val SAFE_GET_TRANSACTION_HASH = "getTransactionHash(address,uint256,bytes,uint8,uint256,uint256,uint256,address,address,uint256)"
        const val SAFE_GET_THRESHOLD = "getThreshold()"
        const val SAFE_GET_OWNERS = "getOwners()"
        const val SAFE_IS_OWNER = "isOwner(address)"
        const val SAFE_NONCE = "nonce()"

        // OpenZeppelin TimelockController
        const val TIMELOCK_SCHEDULE = "schedule(address,uint256,bytes,bytes32,bytes32,uint256)"
        const val TIMELOCK_EXECUTE = "execute(address,uint256,bytes,bytes32,bytes32)"
        const val TIMELOCK_CANCEL = "cancel(bytes32)"
        const val TIMELOCK_HASH_OPERATION = "hashOperation(address,uint256,bytes,bytes32,bytes32)"
        const val TIMELOCK_IS_OPERATION_READY = "isOperationReady(bytes32)"
        const val TIMELOCK_IS_OPERATION_DONE = "isOperationDone(bytes32)"
        const val TIMELOCK_GET_MIN_DELAY = "getMinDelay()"

        // DCDNToken (ERC-20)
        const val ERC20_BALANCE_OF = "balanceOf(address)"
        const val ERC20_TRANSFER = "transfer(address,uint256)"
        const val ERC20_APPROVE = "approve(address,uint256)"
        const val ERC20_ALLOWANCE = "allowance(address,address)"
        const val ERC20_TOTAL_SUPPLY = "totalSupply()"

        // DCDNVestingWallet
        const val VESTING_RELEASE = "release()"
        const val VESTING_RELEASABLE = "releasable()"
        const val VESTING_VESTED_AMOUNT = "vestedAmount(uint64)"
        const val VESTING_BENEFICIARY = "beneficiary()"
        const val VESTING_START = "start()"
        const val VESTING_DURATION = "duration()"
        const val VESTING_CLIFF = "cliff()"
    }
}
