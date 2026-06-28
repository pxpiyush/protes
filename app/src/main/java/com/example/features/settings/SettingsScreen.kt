package com.example.features.settings

import android.content.Context
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.widgets.ProtesLogo
import com.example.features.ProtesViewModel
import com.example.core.model.Prompt
import com.example.core.backup.BackupManager
import com.example.core.security.SecurityManager
import com.example.core.security.EncryptionHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject
import org.json.JSONArray

@Composable
fun SettingsScreen(
    viewModel: ProtesViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val archivedPrompts by viewModel.archivedPrompts.collectAsState()
    val trashPrompts by viewModel.trashPrompts.collectAsState()
    val context = LocalContext.current

    // Dialog Toggle States
    var showArchivedDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var selectedViewPrompt by remember { mutableStateOf<Prompt?>(null) }

    // Phase 9 States
    var storageUsage by remember { mutableStateOf<BackupManager.StorageUsage?>(null) }
    var backupHistory by remember { mutableStateOf<List<BackupManager.BackupFile>>(emptyList()) }
    var autoBackupFreq by remember { mutableStateOf(SecurityManager.getAutoBackupFrequency(context)) }
    var autoBackupRet by remember { mutableStateOf(SecurityManager.getAutoBackupRetention(context)) }
    val coroutineScope = rememberCoroutineScope()

    var showStorageStats by remember { mutableStateOf(false) }
    var showBackupHistory by remember { mutableStateOf(false) }

    // Import/Restore States
    var pendingImportContent by remember { mutableStateOf("") }
    var pendingImportFormat by remember { mutableStateOf("") } // "json_backup", "prompt_pack"
    var importCandidates by remember { mutableStateOf<List<BackupManager.ImportPromptCandidate>>(emptyList()) }
    var selectedHistoryFile by remember { mutableStateOf<BackupManager.BackupFile?>(null) }

    var showMergeReplaceDialog by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var showEncryptBackupChoiceDialog by remember { mutableStateOf(false) }
    var showPasswordDecryptDialog by remember { mutableStateOf(false) }
    var decryptPasswordInput by remember { mutableStateOf("") }
    var encryptPasswordInput by remember { mutableStateOf("") }
    var isBackupEncryptedState by remember { mutableStateOf(false) }

    // PIN Setup States
    var hasPin by remember { mutableStateOf(SecurityManager.hasPin(context)) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var setupPinStep by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var setupPinInput1 by remember { mutableStateOf("") }
    var setupPinInput2 by remember { mutableStateOf("") }
    var showPinClearDialog by remember { mutableStateOf(false) }
    var clearPinInput by remember { mutableStateOf("") }

    // Locked Actions toggles
    var isLockOpenApp by remember { mutableStateOf(SecurityManager.isLockOpenAppEnabled(context)) }
    var isLockHiddenCols by remember { mutableStateOf(SecurityManager.isLockHiddenCollectionsEnabled(context)) }
    var isLockExportLib by remember { mutableStateOf(SecurityManager.isLockExportLibraryEnabled(context)) }
    var isLockRestoreBk by remember { mutableStateOf(SecurityManager.isLockRestoreBackupEnabled(context)) }

    var showHiddenColsConfigDialog by remember { mutableStateOf(false) }
    val allCollections by viewModel.collections.collectAsState()

    // Load stats
    LaunchedEffect(Unit) {
        storageUsage = viewModel.calculateStorageUsage(context)
        backupHistory = BackupManager.getBackupHistory(context)
    }

    val refreshStats = {
        coroutineScope.launch {
            storageUsage = viewModel.calculateStorageUsage(context)
            backupHistory = BackupManager.getBackupHistory(context)
        }
    }

    // System Exporters and Importers via SAF
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val rawJson = viewModel.performBackupJson(context)
                val finalData = if (isBackupEncryptedState && encryptPasswordInput.isNotEmpty()) {
                    EncryptionHelper.encryptBackup(rawJson, encryptPasswordInput.toCharArray())
                } else {
                    rawJson.toByteArray(Charsets.UTF_8)
                }

                if (finalData != null) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(finalData)
                        }
                        Toast.makeText(context, "Backup exported successfully.", Toast.LENGTH_SHORT).show()
                        refreshStats()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Encryption failed.", Toast.LENGTH_SHORT).show()
                }
                encryptPasswordInput = ""
                isBackupEncryptedState = false
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputBytes = context.contentResolver.openInputStream(uri)?.use { ins ->
                        ins.readBytes()
                    } ?: byteArrayOf()

                    val textContent = try {
                        String(inputBytes, Charsets.UTF_8)
                    } catch (e: Exception) {
                        ""
                    }

                    if (textContent.trim().startsWith("{")) {
                        // JSON layout
                        val jsonObj = JSONObject(textContent)
                        if (jsonObj.optBoolean("isPromptPack", false)) {
                            val candidates = BackupManager.parsePromptPack(textContent)
                            if (candidates != null) {
                                val allPromptsList = viewModel.prompts.value + viewModel.archivedPrompts.value
                                importCandidates = BackupManager.analyzeDuplicates(candidates, allPromptsList)
                                showDuplicatesDialog = true
                            } else {
                                Toast.makeText(context, "Invalid Prompt Pack.", Toast.LENGTH_SHORT).show()
                            }
                        } else if (jsonObj.has("prompts") && jsonObj.has("schemaVersion")) {
                            pendingImportContent = textContent
                            pendingImportFormat = "json_backup"
                            showMergeReplaceDialog = true
                        } else {
                            Toast.makeText(context, "Unsupported JSON structure.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Verify if it is an encrypted backup
                        if (inputBytes.size > 20 && inputBytes[0].toInt() == 0x01 && inputBytes[1].toInt() == 0x02) {
                            pendingImportContent = ""
                            context.filesDir.resolve("temp_dec.enc").writeBytes(inputBytes)
                            showPasswordDecryptDialog = true
                        } else {
                            val path = uri.path ?: ""
                            if (path.endsWith(".csv", ignoreCase = true)) {
                                val candidates = BackupManager.parseCsvPrompts(textContent)
                                val allPromptsList = viewModel.prompts.value + viewModel.archivedPrompts.value
                                importCandidates = BackupManager.analyzeDuplicates(candidates, allPromptsList)
                                showDuplicatesDialog = true
                            } else {
                                val candidates = BackupManager.parseMarkdownPrompts(textContent)
                                val allPromptsList = viewModel.prompts.value + viewModel.archivedPrompts.value
                                importCandidates = BackupManager.analyzeDuplicates(candidates, allPromptsList)
                                showDuplicatesDialog = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure your secure local-first prompt vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // SECTION: APPEARANCE
        SettingsSectionHeader("APPEARANCE")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    viewModel.updateThemeMode(if (preferences.themeMode == "dark") "light" else "dark")
                }
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.DarkMode,
                contentDescription = "Theme",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (preferences.themeMode == "dark") "ON" else "OFF",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: WORKSPACE & VAULT
        SettingsSectionHeader("WORKSPACE & VAULT")
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showArchivedDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = "Archived Vault",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Archived Prompts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${archivedPrompts.size} master templates archived.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showTrashDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Trash Bin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Trash Bin", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${trashPrompts.size} soft-deleted prompts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: SECURITY
        SettingsSectionHeader("SECURITY")
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (hasPin) {
                                showPinClearDialog = true
                            } else {
                                setupPinStep = 1
                                setupPinInput1 = ""
                                setupPinInput2 = ""
                                showSetupPinDialog = true
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "PIN Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (hasPin) "Disable App Lock PIN" else "Setup App Lock PIN", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (hasPin) "PIN Protection is ACTIVE. Tap to remove secure lock." else "Secure your master library with a 4-digit master PIN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasPin) {
                            viewModel.updateBiometric(!preferences.isBiometricEnabled)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Fingerprint, "Biometrics", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Lock", style = MaterialTheme.typography.titleMedium, color = if (hasPin) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                        Text("Require fingerprint or face recognition to unlock.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        text = if (preferences.isBiometricEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (preferences.isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Require authentication for specific actions:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = hasPin) { val nv = !isLockOpenApp; SecurityManager.setLockOpenAppEnabled(context, nv); isLockOpenApp = nv }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Open Application:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(text = if (isLockOpenApp) "ON" else "OFF", style = MaterialTheme.typography.labelLarge, color = if (isLockOpenApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }

                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = hasPin) { val nv = !isLockHiddenCols; SecurityManager.setLockHiddenCollectionsEnabled(context, nv); isLockHiddenCols = nv }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Open Hidden Collections:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(text = if (isLockHiddenCols) "ON" else "OFF", style = MaterialTheme.typography.labelLarge, color = if (isLockHiddenCols) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }

                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = hasPin) { val nv = !isLockExportLib; SecurityManager.setLockExportLibraryEnabled(context, nv); isLockExportLib = nv }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Export Vault / Library:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(text = if (isLockExportLib) "ON" else "OFF", style = MaterialTheme.typography.labelLarge, color = if (isLockExportLib) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }

                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = hasPin) { val nv = !isLockRestoreBk; SecurityManager.setLockRestoreBackupEnabled(context, nv); isLockRestoreBk = nv }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Restore Backups:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(text = if (isLockRestoreBk) "ON" else "OFF", style = MaterialTheme.typography.labelLarge, color = if (isLockRestoreBk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showHiddenColsConfigDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.FolderSpecial, "Hidden Folders", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hidden Collections Vault", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Toggle visibility of collections in your workspace library.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: BACKUP & RESTORE
        SettingsSectionHeader("BACKUP & RESTORE")
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (isLockExportLib && SecurityManager.hasPin(context)) {
                                Toast.makeText(context, "Verification required.", Toast.LENGTH_SHORT).show()
                                showPinClearDialog = true
                            } else {
                                showEncryptBackupChoiceDialog = true
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CloudUpload, "Backup", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Vault Backup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Save all prompts, models, canvas layouts securely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (isLockRestoreBk && SecurityManager.hasPin(context)) {
                                Toast.makeText(context, "Verification required.", Toast.LENGTH_SHORT).show()
                            } else {
                                importFileLauncher.launch(arrayOf("*/*"))
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BackupTable, "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import & Restore Files", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Import MD, CSV, TXT, Prompt Packs, or restore JSON vault.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Sync, "Auto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Backups", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Periodically create safe offline local copies.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        var expandedFreq by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { expandedFreq = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Freq: ${autoBackupFreq.uppercase()}")
                            }
                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                                    listOf("none", "daily", "weekly", "monthly").forEach { f ->
                                        DropdownMenuItem(text = { Text(f.uppercase()) }, onClick = { SecurityManager.setAutoBackupFrequency(context, f); autoBackupFreq = f; expandedFreq = false })
                                    }
                                }
                            }
                        }

                        var expandedRet by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { expandedRet = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (autoBackupRet == 0) "UNLIMITED" else "KEEP $autoBackupRet")
                            }
                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenu(expanded = expandedRet, onDismissRequest = { expandedRet = false }) {
                                    listOf(5, 10, 20, 0).forEach { lim ->
                                        DropdownMenuItem(text = { Text(if (lim == 0) "UNLIMITED" else "KEEP $lim") }, onClick = { SecurityManager.setAutoBackupRetention(context, lim); autoBackupRet = lim; expandedRet = false })
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.runAutoBackup(context, autoBackupRet)
                                Toast.makeText(context, "Local backup created inside history list.", Toast.LENGTH_SHORT).show()
                                refreshStats()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Backup Now to Local History")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showBackupHistory = !showBackupHistory }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, "History", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local Backup History", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("${backupHistory.size} historical snapshots stored.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(if (showBackupHistory) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.outline)
                    }
                    AnimatedVisibility(visible = showBackupHistory) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            if (backupHistory.isEmpty()) {
                                Text("No local backups found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                backupHistory.forEach { bk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedHistoryFile = bk }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (bk.isEncrypted) {
                                                    Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(bk.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(bk.lastModified)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text("${bk.size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: ABOUT
        SettingsSectionHeader("ABOUT PROTES")
        Spacer(modifier = Modifier.height(8.dp))
        FlippableAboutCard()
        Spacer(modifier = Modifier.height(28.dp))

    }

    // ARCHIVED PROMPTS DIALOG
    if (showArchivedDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showArchivedDialog = false },
            title = { Text("Archived Vault") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Archived prompts do not appear on your Home grid, keeping your visual workspace clutter-free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (archivedPrompts.isEmpty()) {
                        Text(
                            text = "Archive is empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    } else {
                        archivedPrompts.forEach { prompt ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedViewPrompt = prompt }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = prompt.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = prompt.description.ifEmpty { "No description provided." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.unarchivePrompt(prompt)
                                            Toast.makeText(context, "Restored \"${prompt.title}\" to Home.", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Restore", style = MaterialTheme.typography.labelMedium)
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.permanentDeletePrompt(prompt.id)
                                            Toast.makeText(context, "Deleted permanently.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Delete Permanently", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchivedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // TRASH BIN DIALOG
    if (showTrashDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showTrashDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trash Bin")
                    if (trashPrompts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                trashPrompts.forEach { viewModel.permanentDeletePrompt(it.id) }
                                Toast.makeText(context, "Emptied all items from Trash.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Empty Trash")
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Soft-deleted prompts reside here safely until you choose to empty them or restore them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (trashPrompts.isEmpty()) {
                        Text(
                            text = "Trash Bin is completely empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    } else {
                        trashPrompts.forEach { prompt ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedViewPrompt = prompt }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = prompt.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = prompt.description.ifEmpty { "No description provided." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.restorePrompt(prompt)
                                            Toast.makeText(context, "Restored \"${prompt.title}\" to active vault.", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Restore", style = MaterialTheme.typography.labelMedium)
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.permanentDeletePrompt(prompt.id)
                                            Toast.makeText(context, "Purged permanently.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Purge", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // VIEW SELECTED PROMPT DRAFT DIALOG
    val promptToView = selectedViewPrompt
    if (promptToView != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { selectedViewPrompt = null },
            title = { Text(promptToView.title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = promptToView.description.ifEmpty { "No description." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = promptToView.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedViewPrompt = null }) {
                    Text("Back")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // PHASE 9 SECURITY & SYSTEM DIALOGS
    // -------------------------------------------------------------

    // 1. Password Encryption Option for Backup
    if (showEncryptBackupChoiceDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showEncryptBackupChoiceDialog = false },
            title = { Text("Backup Security Option") },
            text = {
                Column {
                    Text(
                        text = "Would you like to encrypt this backup file with an AES password? Encrypted backups protect your sensitive prompts, but forgotten passwords can never be recovered.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isBackupEncryptedState) {
                        OutlinedTextField(
                            value = encryptPasswordInput,
                            onValueChange = { encryptPasswordInput = it },
                            label = { Text("Backup Password") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (isBackupEncryptedState) {
                    Button(
                        onClick = {
                            if (encryptPasswordInput.isNotBlank()) {
                                showEncryptBackupChoiceDialog = false
                                exportBackupLauncher.launch("protes_backup_${System.currentTimeMillis()}.json.enc")
                            }
                        },
                        enabled = encryptPasswordInput.isNotBlank()
                    ) {
                        Text("Export Encrypted")
                    }
                } else {
                    Button(onClick = { isBackupEncryptedState = true }) {
                        Text("Add Password")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncryptBackupChoiceDialog = false
                    exportBackupLauncher.launch("protes_backup_${System.currentTimeMillis()}.json")
                }) {
                    Text("Export Unencrypted")
                }
            }
        )
    }

    // 2. Password Decryption Prompt during Restore
    if (showPasswordDecryptDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showPasswordDecryptDialog = false },
            title = { Text("Encrypted Backup Detected") },
            text = {
                Column {
                    Text("Please enter the password used to encrypt this backup file:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = decryptPasswordInput,
                        onValueChange = { decryptPasswordInput = it },
                        label = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val encFile = context.filesDir.resolve("temp_dec.enc")
                        if (encFile.exists()) {
                            val encBytes = encFile.readBytes()
                            val decStr = EncryptionHelper.decryptBackup(encBytes, decryptPasswordInput.toCharArray())
                            if (decStr != null) {
                                pendingImportContent = decStr
                                pendingImportFormat = "json_backup"
                                showPasswordDecryptDialog = false
                                showMergeReplaceDialog = true
                            } else {
                                Toast.makeText(context, "Incorrect Password or Decryption failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Decrypt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDecryptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Database Restore Strategy Selection (Merge / Replace)
    if (showMergeReplaceDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showMergeReplaceDialog = false },
            title = { Text("Restore Master Vault") },
            text = {
                Text("Select your backup restoration philosophy:\n\n" +
                     "• REPLACE: Destructive. Erases all current local prompts and sets up identical snapshots.\n" +
                     "• MERGE: Safe. Only inserts elements not already present, preserving your recent work.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.performRestoreReplace(pendingImportContent)
                            Toast.makeText(context, "Full master vault restored successfully.", Toast.LENGTH_LONG).show()
                            showMergeReplaceDialog = false
                            refreshStats()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Replace All (Overwrite)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        viewModel.performRestoreMerge(pendingImportContent)
                        Toast.makeText(context, "Backup elements merged successfully.", Toast.LENGTH_LONG).show()
                        showMergeReplaceDialog = false
                        refreshStats()
                    }
                }) {
                    Text("Merge (Keep Existing)")
                }
            }
        )
    }

    // 4. Import Prompt Pack & Duplicates Resolution Manager
    if (showDuplicatesDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showDuplicatesDialog = false },
            title = { Text("Resolve Import Conflicts") },
            text = {
                Column {
                    val dupsCount = importCandidates.count { it.duplicateType != BackupManager.DuplicateType.NONE }
                    Text(
                        text = "Importing ${importCandidates.size} elements. Detected $dupsCount conflicts with existing local prompts.\n\n" +
                               "Choose duplicate handling strategy:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf(
                        "skip" to "Skip Exact/Near duplicates",
                        "replace" to "Replace older local prompts",
                        "keep_both" to "Keep both (create copy snapshots)",
                        "merge" to "Merge body contents together"
                    ).forEach { (strategy, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveImportedPrompts(importCandidates, strategy)
                                    showDuplicatesDialog = false
                                    Toast.makeText(context, "Import complete.", Toast.LENGTH_SHORT).show()
                                    refreshStats()
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDuplicatesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Setup PIN Dialog
    if (showSetupPinDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showSetupPinDialog = false },
            title = { Text("Secure App PIN Setup") },
            text = {
                Column {
                    Text(
                        text = if (setupPinStep == 1) "Enter a new 4-digit security passcode:" else "Confirm 4-digit security passcode:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = if (setupPinStep == 1) setupPinInput1 else setupPinInput2,
                        onValueChange = { input ->
                            val sanitized = input.filter { it.isDigit() }.take(4)
                            if (setupPinStep == 1) {
                                setupPinInput1 = sanitized
                                if (sanitized.length == 4) {
                                    setupPinStep = 2
                                }
                            } else {
                                setupPinInput2 = sanitized
                                if (sanitized.length == 4) {
                                    if (setupPinInput1 == setupPinInput2) {
                                        SecurityManager.setPin(context, setupPinInput2)
                                        hasPin = true
                                        showSetupPinDialog = false
                                        Toast.makeText(context, "PIN code configured successfully.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Passcodes do not match. Restarting.", Toast.LENGTH_LONG).show()
                                        setupPinStep = 1
                                        setupPinInput1 = ""
                                        setupPinInput2 = ""
                                    }
                                }
                            }
                        },
                        label = { Text("4-digit PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {}
        )
    }

    // 6. Disable PIN Dialog
    if (showPinClearDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showPinClearDialog = false },
            title = { Text("Authentication Required") },
            text = {
                Column {
                    Text("Enter your current 4-digit security PIN to proceed:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = clearPinInput,
                        onValueChange = { input ->
                            val sanitized = input.filter { it.isDigit() }.take(4)
                            clearPinInput = sanitized
                            if (sanitized.length == 4) {
                                if (SecurityManager.verifyPin(context, sanitized)) {
                                    SecurityManager.clearPin(context)
                                    hasPin = false
                                    isLockOpenApp = false
                                    isLockHiddenCols = false
                                    isLockExportLib = false
                                    isLockRestoreBk = false
                                    showPinClearDialog = false
                                    Toast.makeText(context, "PIN code deactivated.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Incorrect passcode.", Toast.LENGTH_SHORT).show()
                                    clearPinInput = ""
                                }
                            }
                        },
                        label = { Text("Current PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {}
        )
    }

    // 7. Hidden Collections Selector Dialog
    if (showHiddenColsConfigDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { showHiddenColsConfigDialog = false },
            title = { Text("Hidden Collections Vault") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Mark collections as hidden. Hidden collections are locked and invisible on your primary Workspace screen unless authenticated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (allCollections.isEmpty()) {
                        Text("No collections available.")
                    } else {
                        allCollections.forEach { col ->
                            var isColHidden by remember { mutableStateOf(SecurityManager.isCollectionHidden(context, col.id)) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(col.name, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = isColHidden,
                                    onCheckedChange = { checked ->
                                        SecurityManager.setCollectionHidden(context, col.id, checked)
                                        isColHidden = checked
                                        Toast.makeText(context, if (checked) "${col.name} hidden" else "${col.name} visible", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenColsConfigDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // 8. Selected History Snapshot Management Modal
    val activeHistoryFile = selectedHistoryFile
    if (activeHistoryFile != null) {
        var showRenameField by remember { mutableStateOf(false) }
        var renameInput by remember { mutableStateOf(activeHistoryFile.name) }
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            onDismissRequest = { selectedHistoryFile = null },
            title = { Text(activeHistoryFile.name) },
            text = {
                Column {
                    Text("Size: ${activeHistoryFile.size / 1024} KB")
                    Text("Modified: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(activeHistoryFile.lastModified)))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showRenameField) {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("Backup Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Button(
                            onClick = {
                                val content = activeHistoryFile.file.readText()
                                if (activeHistoryFile.isEncrypted) {
                                    context.filesDir.resolve("temp_dec.enc").writeBytes(activeHistoryFile.file.readBytes())
                                    selectedHistoryFile = null
                                    showPasswordDecryptDialog = true
                                } else {
                                    pendingImportContent = content
                                    pendingImportFormat = "json_backup"
                                    selectedHistoryFile = null
                                    showMergeReplaceDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore This Snapshot")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showRenameField = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rename Backup Snapshot")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                activeHistoryFile.file.delete()
                                selectedHistoryFile = null
                                Toast.makeText(context, "Backup deleted.", Toast.LENGTH_SHORT).show()
                                refreshStats()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Snapshot permanently")
                        }
                    }
                }
            },
            confirmButton = {
                if (showRenameField) {
                    Button(onClick = {
                        if (renameInput.isNotBlank()) {
                            val dest = activeHistoryFile.file.parentFile?.resolve(renameInput)
                            if (dest != null && activeHistoryFile.file.renameTo(dest)) {
                                Toast.makeText(context, "Backup renamed successfully.", Toast.LENGTH_SHORT).show()
                                selectedHistoryFile = null
                                refreshStats()
                            }
                        }
                    }) {
                        Text("Save")
                    }
                } else {
                    TextButton(onClick = { selectedHistoryFile = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun FlippableAboutCard() {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flip"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                flipped = !flipped
            }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (rotation <= 90f) {
            // Front Side
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_protes_logo),
                    contentDescription = "protes Logo",
                    modifier = Modifier.size(160.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "VERSION 1.0.0 (STABLE BASE)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "protes is a secure, local-first repository built for those who rely on high-fidelity prompts. Keep all your master prompts safe, organized, and easily accessible without needing constant internet access. protes ensures your valuable templates are never lost.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            // Back Side
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer { rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Made at FuseLabs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developer: Piyush Kushwaha",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "\"If it can be free why to pay and infact what to pay for.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val uri = android.net.Uri.parse("https://instagram.com/pxpiyush")
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Outlined.Link, contentDescription = "Instagram")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("@pxpiyush")
                }
            }
        }
    }
}

