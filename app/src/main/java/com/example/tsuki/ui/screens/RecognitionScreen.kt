package com.example.tsuki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.shazam.MusicRecognizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayResult: (String, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var resultTitle by remember { mutableStateOf<String?>(null) }
    var resultArtist by remember { mutableStateOf<String?>(null) }
    var resultCover by remember { mutableStateOf<String?>(null) }

    var pendingPermission by remember { mutableStateOf(false) }

    fun startListening() {
        if (isListening) return
        isListening = true
        errorText = null
        resultTitle = null
        resultArtist = null
        resultCover = null
        progress = 0f
        scope.launch {
            val outcome = MusicRecognizer.recognizeFromMic { p -> progress = p / 100f }
            isListening = false
            outcome.fold(
                onSuccess = { res ->
                    if (res.isMatch) {
                        resultTitle = res.title
                        resultArtist = res.artist
                        resultCover = res.coverArtUrl
                    } else {
                        errorText = "No se reconoció ninguna canción. Inténtalo de nuevo."
                    }
                },
                onFailure = { e ->
                    errorText = e.message ?: "Error al acceder al micrófono"
                }
            )
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else errorText = "Permiso de micrófono denegado"
    }

    androidx.compose.runtime.LaunchedEffect(pendingPermission) {
        if (pendingPermission) {
            pendingPermission = false
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reconocer música", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(140.dp),
                        strokeWidth = 5.dp
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(110.dp)
                        .clickable(enabled = !isListening) {
                            val perm = android.Manifest.permission.RECORD_AUDIO
                            if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                scope.launch { startListening() }
                            } else {
                                pendingPermission = true
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = when {
                    isListening -> "Escuchando… ${ (progress * 100).toInt() }%"
                    resultTitle != null -> "Canción reconocida"
                    else -> "Toca el botón y acerca el teléfono a la música"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            if (resultTitle != null) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        AsyncImage(
                            model = resultCover,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(resultTitle ?: "", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                        Text(resultArtist ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = {
                            val t = resultTitle.orEmpty()
                            val a = resultArtist.orEmpty()
                            onPlayResult(t, a)
                        }) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Buscar y reproducir")
                        }
                    }
                }
            }

            if (errorText != null) {
                Text(errorText ?: "", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                val perm = android.Manifest.permission.RECORD_AUDIO
                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    scope.launch { startListening() }
                } else {
                    pendingPermission = true
                }
            }, enabled = !isListening) {
                Text(if (isListening) "Escuchando…" else "Reconocer")
            }
        }
    }
}
