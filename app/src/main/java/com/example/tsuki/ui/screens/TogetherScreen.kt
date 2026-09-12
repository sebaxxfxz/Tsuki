package com.example.tsuki.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tsuki.R
import com.example.tsuki.together.MusicTogetherConnectionMode
import com.example.tsuki.together.MusicTogetherPreferences
import com.example.tsuki.together.MusicTogetherRepository
import com.example.tsuki.together.TogetherParticipant
import com.example.tsuki.together.TogetherRoomSettings
import com.example.tsuki.together.TogetherSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object TogetherDeepLink {
    val pending = MutableStateFlow<String?>(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TogetherScreen(
    onBack: () -> Unit,
    pendingJoinInput: String? = null,
    onConsumePendingJoin: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val repository = remember { MusicTogetherRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val prefs by repository.preferences.collectAsStateWithLifecycle(initialValue = null)
    val session by repository.sessionState.collectAsStateWithLifecycle(initialValue = TogetherSessionState.Idle)
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinMode by remember { mutableStateOf(MusicTogetherConnectionMode.LAN) }
    var joinPrefill by remember { mutableStateOf("") }
    LaunchedEffect(pendingJoinInput) {
        if (!pendingJoinInput.isNullOrBlank()) {
            joinMode = MusicTogetherConnectionMode.LAN
            joinPrefill = pendingJoinInput
            showJoinDialog = true
            onConsumePendingJoin()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_tool_together), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (val s = session) {
                is TogetherSessionState.Hosting -> item {
                    HostingCard(
                        shareText = s.joinLink, localAddressHint = s.localAddressHint, port = s.port, settings = s.settings,
                        participants = s.roomState?.participants ?: emptyList(),
                        onCopy = { clipboard.setText(AnnotatedString(s.joinLink)) },
                        onShare = { shareText(context, context.getString(R.string.tog_join_link, s.joinLink)) },
                        onUpdateSettings = { repository.updateSettings(it) }, onLeave = { repository.leaveSession() }
                    )
                }
                is TogetherSessionState.HostingOnline -> item {
                    OnlineHostingCard(code = s.code, settings = s.settings, participants = s.roomState?.participants ?: emptyList(),
                        onCopy = { clipboard.setText(AnnotatedString(s.code)) }, onShare = { shareText(context, context.getString(R.string.tog_join_code, s.code)) },
                        onUpdateSettings = { repository.updateSettings(it) }, onKick = { repository.kickParticipant(it) }, onBan = { repository.banParticipant(it) },
                        onTransfer = { repository.transferHostOwnership(it) }, onApprove = { id, ok -> repository.approveParticipant(id, ok) }, onLeave = { repository.leaveSession() })
                }
                is TogetherSessionState.Joining -> item { JoiningCard(stringResource(R.string.tog_joining_lan), onLeave = { repository.leaveSession() }) }
                is TogetherSessionState.JoiningOnline -> item { JoiningCard(stringResource(R.string.tog_connecting), onLeave = { repository.leaveSession() }) }
                is TogetherSessionState.Joined -> item { JoinedCard(state = s, onApprove = { id, ok -> repository.approveParticipant(id, ok) }, onLeave = { repository.leaveSession() }) }
                is TogetherSessionState.Error -> item { ErrorCard(s.message) }
                TogetherSessionState.Idle -> item { HeroCard() }
            }
            if (session is TogetherSessionState.Idle || session is TogetherSessionState.Error) {
                item {
                    ModeCard(
                        onStartHost = { mode ->
                            scope.launch {
                                val p = prefs ?: return@launch
                                repository.startSession(mode, p.displayName, p.port, TogetherRoomSettings(p.allowGuestsToAddTracks, p.allowGuestsToControlPlayback, p.requireHostApprovalToJoin))
                            }
                        },
                        onJoin = { mode -> joinMode = mode; joinPrefill = ""; showJoinDialog = true }
                    )
                }
            }
            val cur = prefs
            if (cur != null && session is TogetherSessionState.Idle) {
                item { IdentityCard(cur, onChange = { scope.launch { repository.setDisplayName(it.trim()) } }) }
                item {
                    HostPrefsCard(cur,
                        onPortChange = { raw -> raw.toIntOrNull()?.let { if (it in 1024..65535) scope.launch { repository.setPort(it) } } },
                        onToggleAdd = { v -> scope.launch { repository.setAllowGuestsToAddTracks(v) } },
                        onToggleControl = { v -> scope.launch { repository.setAllowGuestsToControlPlayback(v) } },
                        onToggleApproval = { v -> scope.launch { repository.setRequireHostApprovalToJoin(v) } })
                }
            }
        }
    }
    if (showJoinDialog) {
        var input by remember(showJoinDialog) { mutableStateOf(joinPrefill) }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text(if (joinMode == MusicTogetherConnectionMode.LAN) stringResource(R.string.tog_join_lan) else stringResource(R.string.tog_join_online)) },
            text = {
                Column {
                    Text(if (joinMode == MusicTogetherConnectionMode.LAN) stringResource(R.string.tog_paste_link) else stringResource(R.string.tog_enter_code), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text(if (joinMode == MusicTogetherConnectionMode.LAN) "tsuki://together?host=..." else "ABC123") })
                }
            },
            confirmButton = {
                Button(onClick = { scope.launch { repository.joinSession(joinMode, input.trim(), repository.currentDisplayName()) }; showJoinDialog = false }, enabled = input.isNotBlank()) { Text(stringResource(R.string.tog_join)) }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.common_share)))
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Group, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tog_synced), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.tog_synced_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                Spacer(Modifier.height(10.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.tog_no_internet)) },
                    leadingIcon = { Icon(Icons.Rounded.Wifi, null, Modifier.size(14.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f), labelColor = MaterialTheme.colorScheme.onPrimaryContainer)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp)
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Group, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModeCard(
    onStartHost: (MusicTogetherConnectionMode) -> Unit,
    onJoin: (MusicTogetherConnectionMode) -> Unit
) {
    var selected by remember { mutableStateOf(MusicTogetherConnectionMode.LAN) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.tog_new_session), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tog_choose_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selected == MusicTogetherConnectionMode.LAN,
                    onClick = { selected = MusicTogetherConnectionMode.LAN },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Rounded.Wifi, null, Modifier.size(16.dp)) },
                    label = { Text("LAN") }
                )
                SegmentedButton(
                    selected = false, onClick = {}, enabled = false,
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Rounded.Link, null, Modifier.size(16.dp)) },
                    label = { Text("Online") }
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text("Coming soon", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (selected == MusicTogetherConnectionMode.LAN) onStartHost(selected) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = selected == MusicTogetherConnectionMode.LAN,
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (selected == MusicTogetherConnectionMode.ONLINE) "Coming soon" else stringResource(R.string.tog_host), fontWeight = FontWeight.SemiBold) }
                FilledTonalButton(
                    onClick = { if (selected == MusicTogetherConnectionMode.LAN) onJoin(selected) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = selected == MusicTogetherConnectionMode.LAN,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PersonAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (selected == MusicTogetherConnectionMode.ONLINE) "Coming soon" else stringResource(R.string.tog_join), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.tog_host_sub), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IdentityCard(prefs: MusicTogetherPreferences, onChange: (String) -> Unit) {
    var name by remember(prefs.displayName) { mutableStateOf(prefs.displayName) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase().ifBlank { "T" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tog_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.tog_name_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.tog_name_hint)) },
                trailingIcon = { TextButton(onClick = { onChange(name) }, enabled = name.isNotBlank() && name != prefs.displayName) { Text(stringResource(R.string.common_save)) } },
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun HostPrefsCard(
    prefs: MusicTogetherPreferences,
    onPortChange: (String) -> Unit,
    onToggleAdd: (Boolean) -> Unit,
    onToggleControl: (Boolean) -> Unit,
    onToggleApproval: (Boolean) -> Unit
) {
    var port by remember(prefs.port) { mutableStateOf(prefs.port.toString()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.tog_host_prefs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.tog_host_prefs_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tog_port)) }, supportingText = { Text(stringResource(R.string.tog_port_current, prefs.port)) },
                trailingIcon = {
                    FilledTonalButton(onClick = { onPortChange(port) }, enabled = port.toIntOrNull()?.let { it in 1024..65535 && it != prefs.port } == true, shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.tog_apply)) }
                },
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(6.dp))
            ToggleRow(stringResource(R.string.tog_add_songs), stringResource(R.string.tog_guests_queue), prefs.allowGuestsToAddTracks, onToggleAdd)
            ToggleRow(stringResource(R.string.tog_control), stringResource(R.string.tog_control_sub), prefs.allowGuestsToControlPlayback, onToggleControl)
            ToggleRow(stringResource(R.string.tog_approve), stringResource(R.string.tog_approve_sub), prefs.requireHostApprovalToJoin, onToggleApproval)
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ShareRow(text: String, masked: String?, onCopy: () -> Unit, onShare: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(masked ?: text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)) { Icon(Icons.Rounded.ContentCopy, stringResource(R.string.common_copy), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onShare, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) { Icon(Icons.Rounded.Share, stringResource(R.string.common_share), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable
private fun HostingCard(
    shareText: String,
    localAddressHint: String?,
    port: Int,
    settings: TogetherRoomSettings,
    participants: List<TogetherParticipant>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onUpdateSettings: (TogetherRoomSettings) -> Unit,
    onLeave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tog_room_active), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(localAddressHint?.let { "$it:$port" } ?: stringResource(R.string.tog_waiting), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) { Text(stringResource(R.string.tog_connected_count, participants.size), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer) }
            }
            Spacer(Modifier.height(14.dp))
            ShareRow(shareText, null, onCopy, onShare)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.tog_share_link), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.tog_permissions), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            ToggleRow(stringResource(R.string.tog_add_songs), stringResource(R.string.tog_queue_verb), settings.allowGuestsToAddTracks) { onUpdateSettings(settings.copy(allowGuestsToAddTracks = it)) }
            ToggleRow(stringResource(R.string.tog_control), "Play/pause/skip", settings.allowGuestsToControlPlayback) { onUpdateSettings(settings.copy(allowGuestsToControlPlayback = it)) }
            ToggleRow(stringResource(R.string.tog_approve), stringResource(R.string.tog_manual_approval), settings.requireHostApprovalToJoin) { onUpdateSettings(settings.copy(requireHostApprovalToJoin = it)) }
            if (participants.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.tog_participants), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                participants.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (p.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Text(p.name.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (p.isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(if (p.isHost) stringResource(R.string.tog_role_host) else if (p.isPending) stringResource(R.string.tog_role_pending) else stringResource(R.string.tog_role_guest), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (p.isHost) Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) { Text("HOST", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text(stringResource(R.string.tog_close_room), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun OnlineHostingCard(
    code: String,
    settings: TogetherRoomSettings,
    participants: List<TogetherParticipant>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onUpdateSettings: (TogetherRoomSettings) -> Unit,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
    onTransfer: (String) -> Unit,
    onApprove: (String, Boolean) -> Unit,
    onLeave: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.tog_online_room), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.tog_code), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(code, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified)
                }
            }
            Spacer(Modifier.height(10.dp))
            ShareRow("", code, onCopy, onShare)
            Spacer(Modifier.height(12.dp))
            ToggleRow(stringResource(R.string.tog_add_songs), stringResource(R.string.tog_queue_verb), settings.allowGuestsToAddTracks) { onUpdateSettings(settings.copy(allowGuestsToAddTracks = it)) }
            ToggleRow(stringResource(R.string.tog_control), "Play/pause", settings.allowGuestsToControlPlayback) { onUpdateSettings(settings.copy(allowGuestsToControlPlayback = it)) }
            if (participants.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                participants.forEach { p ->
                    if (p.isPending) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tog_wants_join, p.name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onApprove(p.id, true) }) { Text(stringResource(R.string.tog_accept)) }
                        TextButton(onClick = { onApprove(p.id, false) }) { Text(stringResource(R.string.tog_reject)) }
                    } else if (!p.isHost) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onTransfer(p.id) }) { Text(stringResource(R.string.tog_hand_over)) }
                        TextButton(onClick = { onKick(p.id) }) { Text(stringResource(R.string.tog_kick)) }
                        TextButton(onClick = { onBan(p.id) }) { Text(stringResource(R.string.tog_block)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onLeave, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.tog_close_room)) }
        }
    }
}

@Composable
private fun JoiningCard(message: String, onLeave: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            TextButton(onClick = onLeave) { Text(stringResource(R.string.common_cancel)) }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.tog_connect_fail), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun JoinedCard(state: TogetherSessionState.Joined, onApprove: (String, Boolean) -> Unit, onLeave: () -> Unit) {
    val roleLabel = if (state.role is com.example.tsuki.together.TogetherRole.Host) stringResource(R.string.tog_role_host) else stringResource(R.string.tog_role_guest)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Group, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tog_connected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(roleLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) { Text("${state.roomState.queue.size}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(stringResource(R.string.pld_now_playing), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.roomState.queue.getOrNull(state.roomState.currentIndex)?.title ?: "—", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
                }
            }
            if (state.roomState.participants.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Spacer(Modifier.height(10.dp))
                state.roomState.participants.forEach { p ->
                    if (p.isPending && state.role is com.example.tsuki.together.TogetherRole.Host) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.tog_pending_name, p.name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onApprove(p.id, true) }) { Text(stringResource(R.string.tog_accept)) }
                            TextButton(onClick = { onApprove(p.id, false) }) { Text(stringResource(R.string.tog_reject)) }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                Text(p.name.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.tog_leave), fontWeight = FontWeight.SemiBold) }
        }
    }
}
