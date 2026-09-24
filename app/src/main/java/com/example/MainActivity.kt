package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.GovernanceScreen
import com.example.ui.screens.TokenAllocationScreen
import com.example.ui.screens.TreasuryHistoryScreen
import com.example.ui.screens.WalletConnectScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PositiveGreen

class MainActivity : FragmentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "allocation"

        val items = listOf("allocation", "governance", "wallet", "history")
        val labels = listOf("Allocation", "Governance", "Hardware Key", "Treasury Flows")
        val icons = listOf(
          Icons.Default.PieChart,
          Icons.Default.AccountBalance,
          Icons.Default.Security,
          Icons.Default.ShowChart
        )

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            TopAppBar(
              title = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      Icons.Default.Token,
                      contentDescription = "DCDN Logo",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      "DCDN Treasury",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.ExtraBold,
                      letterSpacing = 0.5.sp
                    )
                    Text(
                      "Arbitrum One • Multi-Sig Cold Safe",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              },
              actions = {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.padding(end = 12.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(PositiveGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      "423.5M DCDN",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = CyanPrimary
                    )
                  }
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
              )
            )
          },
          bottomBar = {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 8.dp
            ) {
              items.forEachIndexed { index, route ->
                val selected = currentRoute == route
                NavigationBarItem(
                  icon = {
                    Icon(
                      icons[index],
                      contentDescription = labels[index]
                    )
                  },
                  label = {
                    Text(
                      labels[index],
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  selected = selected,
                  onClick = {
                    if (currentRoute != route) {
                      navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                          saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                  )
                )
              }
            }
          }
        ) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = "allocation",
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("allocation") { TokenAllocationScreen() }
            composable("governance") { GovernanceScreen(activity = this@MainActivity) }
            composable("wallet") { WalletConnectScreen(activity = this@MainActivity) }
            composable("history") { TreasuryHistoryScreen() }
          }
        }
      }
    }
  }
}
