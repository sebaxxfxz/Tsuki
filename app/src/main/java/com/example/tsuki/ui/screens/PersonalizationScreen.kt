package com.example.tsuki.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tsuki.R
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.TSukiBackupRepository
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import kotlinx.coroutines.launch

private data class ContentLanguageOption(val tag: String, val label: String)

private val contentLanguageOptions = listOf(
    ContentLanguageOption("es", "Español"),
    ContentLanguageOption("es-419", "Español (Latinoamérica)"),
    ContentLanguageOption("en", "English"),
    ContentLanguageOption("pt", "Portugués"),
    ContentLanguageOption("fr", "Français"),
    ContentLanguageOption("de", "Deutsch"),
    ContentLanguageOption("it", "Italiano"),
    ContentLanguageOption("ja", "日本語"),
    ContentLanguageOption("ko", "한국어")
)

private val contentCountryOptions = listOf(
    "ES", "MX", "AR", "CL", "CO", "PE", "US", "GB", "BR", "PT", "FR", "DE", "IT", "JP", "KR"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { HomePreferences(context) }
    val scope = rememberCoroutineScope()
    val contentLanguage by prefs.contentLanguageTag.collectAsStateWithLifecycle(initialValue = "es")
    val contentCountry by prefs.contentCountry.collectAsStateWithLifecycle(initialValue = "ES")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pers_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        modifier = modifier
    ) { padding ->
        var showResetDialog by remember { mutableStateOf(false) }
        var resetSuccess by remember { mutableStateOf(false) }
        val backupRepo = remember { TSukiBackupRepository(context) }
        var exportMsg by remember { mutableStateOf<String?>(null) }
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.exportSubscriptionsAsNewPipe(it)
                    val msg = if (res.isSuccess) context.getString(R.string.pers_export_newpipe_ok) else context.getString(R.string.pers_export_newpipe_fail)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.importNewPipe(it)
                    val msg = if (res.isSuccess) context.getString(R.string.pers_import_newpipe_ok, res.getOrNull() ?: 0) else context.getString(R.string.pers_import_newpipe_fail)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val exportMasterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.exportMaster(it)
                    Toast.makeText(context, if (res.isSuccess) context.getString(R.string.pers_backup_ok) else context.getString(R.string.pers_backup_fail), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val importMasterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.importMaster(it)
                    Toast.makeText(context, if (res.isSuccess) context.getString(R.string.pers_restore_ok) else context.getString(R.string.pers_restore_fail), Toast.LENGTH_SHORT).show()
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.pers_interests_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.pers_interests_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    prefs.setOnboardingDone(false)
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.pers_sub))
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(stringResource(R.string.pers_content), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.pers_content_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(stringResource(R.string.pers_lang), style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(contentLanguageOptions, key = { it.tag }) { option ->
                                FilterChip(
                                    selected = contentLanguage == option.tag,
                                    onClick = { scope.launch { prefs.setContentLanguage(option.tag) } },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                        Text(stringResource(R.string.pers_country), style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(contentCountryOptions, key = { it }) { code ->
                                FilterChip(
                                    selected = contentCountry == code,
                                    onClick = { scope.launch { prefs.setContentCountry(code) } },
                                    label = { Text(code) }
                                )
                            }
                        }
                        Text(
                            "Los cambios se aplican al volver a cargar el feed.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.pers_export), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pers_export_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(onClick = { exportLauncher.launch("tsuki_subs_${System.currentTimeMillis()}.json") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export NewPipe")
                            }
                            FilledTonalButton(onClick = { importLauncher.launch("application/json") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import NewPipe")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { exportMasterLauncher.launch("tsuki_backup_${System.currentTimeMillis()}.json") }, modifier = Modifier.weight(1f)) { Text("Backup") }
                            OutlinedButton(onClick = { importMasterLauncher.launch("application/json") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.pers_restore)) }
                        }
                        if (exportMsg != null) Text(exportMsg ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.pers_brain_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        stringResource(R.string.pers_brain_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(stringResource(R.string.pers_brain_reset_btn))
                    }
                    if (resetSuccess) {
                        Text(
                            stringResource(R.string.pers_brain_reset_success),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.pers_reset_title)) },
                text = { Text(stringResource(R.string.pers_reset_text)) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                TSukiNeuroEngine.resetBrain(context)
                                resetSuccess = true
                                showResetDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.pers_reset_go))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}
