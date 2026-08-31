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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("Ajustes TSuki", fontWeight = FontWeight.Bold) },
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
                    exportMsg = if (res.isSuccess) "Exportado NewPipe OK" else "Error export"
                    Toast.makeText(context, exportMsg ?: "", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.importNewPipe(it)
                    exportMsg = if (res.isSuccess) "Importados ${res.getOrNull()} canales" else "Error import"
                    Toast.makeText(context, exportMsg ?: "", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val exportMasterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.exportMaster(it)
                    Toast.makeText(context, if (res.isSuccess) "Backup maestro OK" else "Error backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val importMasterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    val res = backupRepo.importMaster(it)
                    Toast.makeText(context, if (res.isSuccess) "Restaurado OK" else "Error restore", Toast.LENGTH_SHORT).show()
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
                            "Tus Intereses y Canales",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Vuelve a seleccionar tus temas preferidos (Gaming, Tecnología, Anime, etc.) y los canales que quieres seguir en tu feed.",
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
                            Text("Cambiar temas y canales seguidos")
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
                        Text("Idioma y región del contenido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Define en qué idioma se muestran los títulos y descripciones, y de qué país salen las tendencias. Evita que el contenido se traduzca automáticamente a otro idioma.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Idioma", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(contentLanguageOptions, key = { it.tag }) { option ->
                                FilterChip(
                                    selected = contentLanguage == option.tag,
                                    onClick = { scope.launch { prefs.setContentLanguage(option.tag) } },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                        Text("País (tendencias)", style = MaterialTheme.typography.labelLarge)
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
                        Text("Exportar / Importar Suscripciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Guarda tus canales en JSON NewPipe o CSV YouTube para migrar sin cuenta. También backup maestro.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            OutlinedButton(onClick = { importMasterLauncher.launch("application/json") }, modifier = Modifier.weight(1f)) { Text("Restaurar") }
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
                        Text("TSuki Brain (Inteligencia Local)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "TSuki aprende de lo que ves, saltas y buscas. Todo esto se procesa y guarda " +
                        "estrictamente en tu dispositivo. Puedes reiniciarlo si quieres empezar desde cero.",
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
                        Text("Reiniciar TSuki Brain")
                    }
                    if (resetSuccess) {
                        Text(
                            "Brain reiniciado. Ciérralo y vuelve a abrir para ver los cambios.",
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
                title = { Text("¿Reiniciar TSuki Brain?") },
                text = { Text("Perderás todo el entrenamiento local y preferencias aprendidas. No se puede deshacer.") },
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
                        Text("Borrar y reiniciar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
