package id.jo.jomanager.ui.screens

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.jo.jomanager.ui.viewmodel.SettingsViewModel
import id.jo.jomanager.ui.theme.ThemeMode
import id.jo.jomanager.R
import id.jo.jomanager.utils.LocaleHelper
import id.jo.jomanager.utils.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import id.jo.jomanager.ui.dialog.BackupRestoreDialog
import androidx.core.net.toUri

private data class PendingBackupOptions(
    val tuning: Boolean,
    val network: Boolean,
    val battery: Boolean,
    val other: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationIconDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    // State to hold options selected in the dialog before file picker is launched
    var pendingOptions by remember { mutableStateOf<PendingBackupOptions?>(null) }

    val currentThemeMode by viewModel.currentThemeMode.collectAsState()
    val notificationIconStyle by viewModel.notificationIconStyle.collectAsState()
    val isBatteryMonitorEnabled by viewModel.isBatteryMonitorEnabled.collectAsState()
    val backupPreview by viewModel.backupPreview.collectAsState()
    val selectedRestoreUri by viewModel.selectedRestoreUri.collectAsState()
    val context = LocalContext.current
    
    var themeRefreshKey by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(currentThemeMode) {
        themeRefreshKey++
    }

    LaunchedEffect(Unit) {
        viewModel.backupRestoreEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val backupFileName = stringResource(R.string.backup_file_name)
    val permissionStorageRequired = stringResource(R.string.permission_storage_required)

    // File Picker Launchers
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingOptions != null) {
            val ops = pendingOptions!!
            viewModel.backupSettings(uri, ops.tuning, ops.network, ops.battery, ops.other)
        }
        pendingOptions = null
    }

    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadBackupPreview(uri)
            showBackupDialog = true // Re-open dialog to show preview
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showBackupDialog = true
        } else {
            Toast.makeText(context, permissionStorageRequired, Toast.LENGTH_SHORT).show()
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                showBackupDialog = true
            } else {
                Toast.makeText(context, permissionStorageRequired, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkStoragePermissionAndShowDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                showBackupDialog = true
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            }
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                showBackupDialog = true
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.language),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )
            
            var showLanguageDialog by remember { mutableStateOf(false) }
            val currentLocaleTag by remember(themeRefreshKey) { mutableStateOf(LocaleHelper.getCurrentLocaleTag(context)) }

            SettingItemCard(
                headlineText = stringResource(R.string.language),
                supportingText = LocaleHelper.getLocaleDisplayName(context, currentLocaleTag),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = getRoundedCornerShape(0, 1),
                onClick = { 
                    if (LocaleHelper.useSystemLanguageSettings) {
                        LocaleHelper.launchSystemLanguageSettings(context)
                    } else {
                        showLanguageDialog = true 
                    }
                }
            )

            if (showLanguageDialog) {
                LanguageSelectionDialog(
                    currentLocale = currentLocaleTag,
                    onLocaleSelected = { localeTag ->
                        LocaleHelper.setLocale(context, localeTag)
                        showLanguageDialog = false
                        val activity = context as? Activity
                        activity?.apply {
                            finish()
                            val intent = Intent(this, this::class.java)
                            intent.putExtra("navigateToSettings", true)
                            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
                            startActivity(intent, options.toBundle())
                        }
                    },
                    onDismiss = { showLanguageDialog = false }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(id = R.string.battery_monitor),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )

            SettingItemCard(
                headlineText = stringResource(R.string.notification_icon),
                supportingText = if (isBatteryMonitorEnabled) {
                    when (notificationIconStyle) {
                        PreferenceManager.ICON_STYLE_BATTERY_PERCENT -> stringResource(R.string.icon_battery_percent)
                        PreferenceManager.ICON_STYLE_APP_LOGO -> stringResource(R.string.icon_app_logo)
                        PreferenceManager.ICON_STYLE_TRANSPARENT -> stringResource(R.string.icon_transparent)
                        else -> stringResource(R.string.icon_app_logo)
                    }
                } else {
                    stringResource(R.string.requires_battery_monitor)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = getRoundedCornerShape(0, 1),
                enabled = isBatteryMonitorEnabled,
                onClick = { showNotificationIconDialog = true }
            )

            if (showNotificationIconDialog) {
                NotificationIconSelectionDialog(
                    currentStyle = notificationIconStyle,
                    onStyleSelected = { style ->
                        viewModel.setNotificationIconStyle(style)
                        showNotificationIconDialog = false
                    },
                    onDismiss = { showNotificationIconDialog = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.theme_and_display),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )
            
            SettingItemCard(
                headlineText = stringResource(R.string.theme),
                supportingText = when (currentThemeMode) {
                    ThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Contrast,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = getRoundedCornerShape(0, 2),
                onClick = { showThemeDialog = true }
            )

            val isDarkTheme = when (currentThemeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }
            val isAmoledMode by viewModel.isAmoledMode.collectAsState()

            SettingItemCard(
                headlineText = stringResource(id = R.string.amoled_mode),
                supportingText = stringResource(id = R.string.amoled_mode_desc),
                icon = {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isAmoledMode,
                        onCheckedChange = { viewModel.setAmoledMode(it) },
                        enabled = isDarkTheme,
                        thumbContent = if (isAmoledMode) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        }
                    )
                },
                shape = getRoundedCornerShape(1, 2),
                onClick = { 
                    if (isDarkTheme) {
                        viewModel.setAmoledMode(!isAmoledMode) 
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.backup_restore_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )

            SettingItemCard(
                headlineText = stringResource(R.string.backup_restore_title),
                supportingText = stringResource(R.string.backup_restore_desc),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = getRoundedCornerShape(0, 1),
                onClick = { checkStoragePermissionAndShowDialog() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.system_info),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            )

            SettingItemCard(
                headlineText = stringResource(R.string.permission_manager_title),
                supportingText = stringResource(R.string.permission_manager_desc),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = getRoundedCornerShape(0, 1),
                onClick = { navController.navigate("permission_manager") }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val pInfo = remember(context) {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: Exception) {
                    null
                }
            }

            val versionInfo = if (pInfo != null) {
                val versionName = pInfo.versionName
                val versionCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
                stringResource(R.string.version_format, versionName ?: "", versionCode)
            } else {
                stringResource(R.string.version_na)
            }

            Text(
                text = versionInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = currentThemeMode,
            onThemeSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            onDismiss = { 
                showBackupDialog = false
                viewModel.clearBackupPreview()
            },
            onBackup = { tuning, network, battery, other ->
                pendingOptions = PendingBackupOptions(tuning, network, battery, other)
                showBackupDialog = false
                backupLauncher.launch(backupFileName)
            },
            onSelectFile = {
                showBackupDialog = false
                restoreFilePickerLauncher.launch(arrayOf("application/json"))
            },
            onRestore = { tuning, network, battery, other ->
                selectedRestoreUri?.let { uri ->
                    viewModel.restoreSettings(uri, tuning, network, battery, other)
                }
                showBackupDialog = false
            },
            preview = backupPreview
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationIconSelectionDialog(
    currentStyle: Int,
    onStyleSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notification),
                                contentDescription = stringResource(id = R.string.notification_icon),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.select_notification_icon),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val styles = listOf(
                            PreferenceManager.ICON_STYLE_BATTERY_PERCENT,
                            PreferenceManager.ICON_STYLE_APP_LOGO,
                            PreferenceManager.ICON_STYLE_TRANSPARENT
                        )
                        styles.forEachIndexed { index, style ->
                            val isSelected = style == currentStyle
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getDialogListItemShape(index, styles.size),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                onClick = { onStyleSelected(style) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = when (style) {
                                            PreferenceManager.ICON_STYLE_BATTERY_PERCENT -> stringResource(R.string.icon_battery_percent)
                                            PreferenceManager.ICON_STYLE_APP_LOGO -> stringResource(R.string.icon_app_logo)
                                            PreferenceManager.ICON_STYLE_TRANSPARENT -> stringResource(R.string.icon_transparent)
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionDialog(
    currentLocale: String,
    onLocaleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val languageOptions = remember {
        listOf("system", "en", "in")
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(id = R.string.language),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.select_language),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        languageOptions.forEachIndexed { index, localeTag ->
                            val isSelected = localeTag == currentLocale
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getDialogListItemShape(index, languageOptions.size),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                onClick = { onLocaleSelected(localeTag) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = LocaleHelper.getLocaleDisplayName(context, localeTag),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                 }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionDialog(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contrast,
                                contentDescription = stringResource(id = R.string.theme),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.select_theme),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val themeModes = ThemeMode.entries
                        themeModes.forEachIndexed { index, themeMode ->
                            val isSelected = themeMode == currentThemeMode
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getDialogListItemShape(index, themeModes.size),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                onClick = { onThemeSelected(themeMode) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = when (themeMode) {
                                            ThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system)
                                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                        },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItemCard(
    headlineText: String,
    supportingText: String,
    icon: @Composable () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingContent: @Composable () -> Unit = {},
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(if (enabled) Modifier else Modifier.alpha(0.38f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with themed background
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    icon()
                }
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (trailingContent != {}) {
                trailingContent()
            }
        }
    }
}

private fun getRoundedCornerShape(index: Int, totalItems: Int): RoundedCornerShape {
    return when (totalItems) {
        1 -> RoundedCornerShape(24.dp) // If only one card, all corners 12dp
        2 -> {
            when (index) {
                0 -> RoundedCornerShape( // First card: 12dp top, 4dp bottom
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
                1 -> RoundedCornerShape( // Second card: 4dp top, 12dp bottom
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
                else -> RoundedCornerShape(24.dp) // Default case
            }
        }
        else -> {
            // For groups with more than 2 items
            when (index) {
                0 -> RoundedCornerShape( // First card: 12dp top, 4dp bottom
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
                totalItems - 1 -> RoundedCornerShape( // Last card: 4dp top, 12dp bottom
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
                else -> RoundedCornerShape(8.dp) // Middle cards: 4dp all sides
            }
        }
    }
}

private fun getDialogListItemShape(index: Int, totalItems: Int): RoundedCornerShape {
    return when {
        totalItems == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }
}
