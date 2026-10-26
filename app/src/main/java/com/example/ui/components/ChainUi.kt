package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WarningAmber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/** "2,100,000" / "1,234.5678" — full precision up to 4 decimals, never rounded up. */
fun formatTokens(value: BigDecimal): String {
    val scaled = value.setScale(4, RoundingMode.DOWN).stripTrailingZeros()
    return DecimalFormat("#,##0.####").format(scaled)
}

/** Compact form for chips: 21M, 2.1M, 950K. */
fun formatTokensCompact(value: BigDecimal): String {
    val v = value.toDouble()
    return when {
        v >= 1_000_000_000 -> DecimalFormat("#,##0.##").format(v / 1_000_000_000) + "B"
        v >= 1_000_000 -> DecimalFormat("#,##0.##").format(v / 1_000_000) + "M"
        v >= 1_000 -> DecimalFormat("#,##0.##").format(v / 1_000) + "K"
        else -> DecimalFormat("#,##0.####").format(v)
    }
}

fun percentOf(part: BigDecimal, whole: BigDecimal): Double =
    if (whole.signum() == 0) 0.0 else part.divide(whole, 6, RoundingMode.HALF_UP).toDouble() * 100.0

fun shortAddress(address: String): String =
    if (address.length > 12) "${address.take(6)}…${address.takeLast(4)}" else address

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "No browser available to open $url", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun LoadingCard(label: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Loading $label from chain…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun UnavailableCard(label: String, reason: String, onRetry: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("$label unavailable", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onRetry != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun NotDeployedCard(networkName: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = WarningAmber)
            Spacer(Modifier.width(12.dp))
            Text("DCDN is not deployed on $networkName yet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Label + monospace address. Tap copies; the trailing link opens the explorer. */
@Composable
fun AddressRow(label: String, address: String, explorerUrl: String? = null) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboard.setText(AnnotatedString(address))
                Toast.makeText(context, "Copied $label address", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
        Icon(
            Icons.Default.ContentCopy, contentDescription = "Copy",
            modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (explorerUrl != null) {
            TextButton(onClick = { openUrl(context, explorerUrl) }) { Text("View") }
        }
    }
}
