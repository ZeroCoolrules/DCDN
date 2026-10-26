package com.example

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.repository.ChainEvent
import com.example.repository.EventCategory
import com.example.repository.SafeSnapshot
import com.example.ui.screens.GovernanceScreen
import com.example.ui.screens.TokenAllocationScreen
import com.example.ui.screens.TreasuryHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LoadState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.math.BigInteger

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val safe = SafeSnapshot(
    address = "0xd8cb95124fbc5611Ad834ac3efDff85048FA0724",
    owners = listOf("0xeE97b0BC5d978d4e3B1f10F5Ecc4490492405B6B"),
    threshold = 1,
    nonce = BigInteger.ZERO,
    version = "1.4.1"
  )

  @Test
  fun tokenAllocation_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme { Surface { TokenAllocationScreen(state = LoadState.Ready(ExampleUnitTest.sampleSnapshot())) } }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/token_allocation.png")
  }

  @Test
  fun tokenAllocation_unavailable_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme { Surface { TokenAllocationScreen(state = LoadState.Unavailable("Unable to reach https://sepolia.base.org")) } }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/token_allocation_unavailable.png")
  }

  @Test
  fun governance_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface { GovernanceScreen(safeState = LoadState.Ready(safe), tokenState = LoadState.Ready(ExampleUnitTest.sampleSnapshot())) }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/governance.png")
  }

  @Test
  fun history_screenshot() {
    val events = listOf(
      ChainEvent("0x75c7c2c2bff82d47735b429b162d5f00b99f588cbb5299a0dacf6f28ab146d0c", 47503841, 2441,
        "2026-09-30T13:46:10.000000Z", EventCategory.ADMIN, "Ownership transferred",
        from = "0x856720DD7a807bE6ad69d97e164d049A4b80A912", to = safe.address),
      ChainEvent("0xc74d59dd7f95bd99df87e5c475606d7db81f206df7af3d47b2f34dc313d00b62", 47334968, 12,
        "2026-09-26T15:57:04.000000Z", EventCategory.ADMIN, "Minter added",
        to = "0x0Ec2924F933bbf4591157E7A500feCEf4e26653C"),
      ChainEvent("0x9b2212ea09d252401c1c61c9beb732fb7fe6a2fc9c61b1c2dcb7eba2f8c165fc", 47334963, 149,
        "2026-09-26T15:56:54.000000Z", EventCategory.TRANSFER, "Mint",
        from = "0x0000000000000000000000000000000000000000", to = "0x375CEf117533803cB287ad80Ddca179117e63BEE",
        amount = BigDecimal("2100000"))
    )
    composeTestRule.setContent {
      MyApplicationTheme { Surface { TreasuryHistoryScreen(state = LoadState.Ready(events)) } }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/history.png")
  }
}
