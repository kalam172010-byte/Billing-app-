package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SyncMetrics
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoMedium
import java.util.Locale

/**
 * Offline Mode & Local Storage Modal:
 * - Manual Force Offline / Go Online toggle + live network detection
 * - Local persistent cache metrics (stores, products, offline bills, cache KB, pending cloud sync count)
 * - Manual Sync Now
 * - 1-click Backup Data (.json) & Restore Backup (.json)
 */
@Composable
fun OfflineSyncDialog(
    metrics: SyncMetrics,
    isBusy: Boolean,
    onToggleForceOffline: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOffline = metrics.forceOffline || !metrics.networkAvailable

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = if (isOffline) Amber700 else Emerald600
                        )
                        Column {
                            Text("Offline Mode & Local Cache", style = MaterialTheme.typography.titleLarge, color = Slate900)
                            Text("Persistent Firestore Disk Cache + JSON Backup", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Slate600)
                    }
                }

                // Force Offline / Go Online Switch Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOffline) Amber50 else Emerald50,
                    border = BorderStroke(1.dp, if (isOffline) Amber700.copy(alpha = 0.4f) else Emerald600.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (metrics.forceOffline) "Force Offline Mode Active" else "Automatic Cloud Sync",
                                style = MaterialTheme.typography.titleSmall,
                                color = Slate900
                            )
                            Text(
                                text = if (metrics.networkAvailable) "Device network: Online" else "Device network: No Internet",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOffline) Amber700 else Emerald700
                            )
                        }
                        Switch(
                            checked = metrics.forceOffline,
                            onCheckedChange = onToggleForceOffline,
                            colors = SwitchDefaults.colors(checkedTrackColor = Amber700),
                            modifier = Modifier.testTag("force_offline_switch")
                        )
                    }
                }

                // Local Cache Metrics Grid
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("LOCAL PERSISTENT STORAGE METRICS", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        MetricLine("Business Branches Cached", "${metrics.storeCount}")
                        MetricLine("Products Cached Offline", "${metrics.productCount}")
                        MetricLine("Digital Bills Stored", "${metrics.saleCount}")
                        MetricLine("Stock Audit Logs", "${metrics.stockLogCount}")
                        MetricLine("Estimated Local Cache Size", String.format(Locale.US, "%.1f KB", metrics.estimatedCacheKb))
                        MetricLine(
                            "Pending Cloud Sync Queue",
                            if (metrics.pendingWritesCount == 0) "0 (All Synced)" else "${metrics.pendingWritesCount} collection(s)"
                        )
                    }
                }

                Button(
                    onClick = onSyncNow,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_now_button")
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Now to Cloud Firestore")
                }

                HorizontalDivider(color = Slate200)

                Text("PORTABLE LOCAL BACKUP", style = MaterialTheme.typography.labelSmall, color = Slate500)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackupJson,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("backup_json_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Slate900)
                        Spacer(Modifier.width(6.dp))
                        Text("Backup .json", color = Slate900)
                    }
                    OutlinedButton(
                        onClick = onRestoreJson,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("restore_json_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp), tint = Slate900)
                        Spacer(Modifier.width(6.dp))
                        Text("Restore .json", color = Slate900)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate600)
        Text(value, style = TabularMonoMedium, color = Slate900)
    }
}
