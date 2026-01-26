package id.jo.jomanager.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.jo.jomanager.R
import id.jo.jomanager.data.database.AppProfileEntity
import id.jo.jomanager.viewmodel.AppInfo
import id.jo.jomanager.viewmodel.AppProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfilesScreen(
    navController: NavController,
    viewModel: AppProfilesViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isKgslFeatureAvailable by viewModel.isKgslFeatureAvailable.collectAsStateWithLifecycle()
    val isPowersaveAvailable by viewModel.isPowersaveAvailable.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<AppProfileEntity?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = viewModel.hasUsageStatsPermission()
                showPermissionDialog = !hasPermission
                if (hasPermission) {
                    viewModel.toggleService(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.loadInstalledApps()
                    showAddDialog = true
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.app_profiles_add_profile))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(
                    bottom = padding.calculateBottomPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    top = 0.dp // Set top padding to 0.dp
                )
                .fillMaxSize()
        ) {
            if (showPermissionDialog) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                                    Text(
                                                        stringResource(R.string.app_profiles_permission_required),                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.app_profiles_no_profiles))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(profiles) { index, profile ->
                        val shape = when {
                            profiles.size == 1 -> RoundedCornerShape(24.dp) // Single item
                            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp) // First item
                            index == profiles.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp) // Last item
                            else -> RoundedCornerShape(8.dp) // Middle items
                        }
                        AppProfileItem(
                            profile = profile,
                            onEdit = { profileToEdit = it },
                            onDelete = { viewModel.deleteProfile(it) },
                            cardShape = shape
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddDialog = false
                    viewModel.onSearchQueryChanged("") // Reset search when closed
                },
                sheetState = sheetState
            ) {
                AppPickerSheet(
                    filteredApps = filteredApps,
                    isLoading = isLoadingApps,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onDismiss = {
                        showAddDialog = false
                        viewModel.onSearchQueryChanged("") // Reset search when closed
                    },
                    onAppSelected = { appInfo ->
                        viewModel.addProfile(appInfo)
                        showAddDialog = false
                        viewModel.onSearchQueryChanged("") // Reset search
                    }
                )
            }
        }

        if (profileToEdit != null) {
            AppProfileConfigDialog(
                profile = profileToEdit!!,
                isKgslFeatureAvailable = isKgslFeatureAvailable == true,
                isPowersaveAvailable = isPowersaveAvailable,
                onDismiss = { profileToEdit = null },
                onSave = { updatedProfile ->
                    viewModel.updateProfile(updatedProfile)
                    profileToEdit = null
                }
            )
        }
    }
}

@Composable
fun AppProfileItem(
    profile: AppProfileEntity,
    onEdit: (AppProfileEntity) -> Unit,
    onDelete: (AppProfileEntity) -> Unit,
    cardShape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    // Load icon helper
    val icon = remember(profile.packageName) {
        try {
            context.packageManager.getApplicationIcon(profile.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(profile) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (icon != null) {
                Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(48.dp)) // Placeholder
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${profile.performanceMode}${stringResource(R.string.app_profiles_kgsl_prefix)}${if(profile.kgslSkipZeroing) stringResource(R.string.app_profiles_on) else stringResource(R.string.app_profiles_off)}${stringResource(R.string.app_profiles_bypass_prefix)}${if(profile.bypassCharging) stringResource(R.string.app_profiles_on) else stringResource(R.string.app_profiles_off)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = { onDelete(profile) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.app_profiles_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    filteredApps: List<AppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AppRegistration,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = stringResource(R.string.app_profiles_select_app),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.app_profiles_search_apps)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.app_profiles_clear_search))
                    }
                }
            } else null,
            singleLine = true
        )

        // App List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.app_profiles_no_apps_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(filteredApps) { index, app ->
                        val shape = when {
                            filteredApps.size == 1 -> RoundedCornerShape(24.dp) // Single item
                            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp) // First item
                            index == filteredApps.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp) // Last item
                            else -> RoundedCornerShape(8.dp) // Middle items
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = shape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val context = LocalContext.current
                                val icon = remember(app.packageName) {
                                    try {
                                        context.packageManager.getApplicationIcon(app.packageName)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (icon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Close Button
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.app_profiles_close))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppProfileConfigDialog(
    profile: AppProfileEntity,
    isKgslFeatureAvailable: Boolean,
    isPowersaveAvailable: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppProfileEntity) -> Unit
) {
    var performanceMode by remember { mutableStateOf(profile.performanceMode) }
    var kgslSkipZeroing by remember { mutableStateOf(profile.kgslSkipZeroing) }
    var bypassCharging by remember { mutableStateOf(profile.bypassCharging) }
    var isEnabled by remember { mutableStateOf(profile.isEnabled) }

    val options = remember(isPowersaveAvailable) {
        if (isPowersaveAvailable) {
            listOf("Powersave", "Balanced", "Performance")
        } else {
            listOf("Balanced", "Performance")
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(min = 200.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = profile.appName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Content
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Enable/Disable Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.app_profiles_enable_profile),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { isEnabled = it },
                                    thumbContent = if (isEnabled) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    } else {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // Performance Mode
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.app_profiles_performance_mode),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                
                                val selectedText = when (performanceMode) {
                                    "Powersave" -> stringResource(R.string.app_profiles_powersave)
                                    "Balanced" -> stringResource(R.string.app_profiles_balanced)
                                    else -> stringResource(R.string.app_profiles_performance)
                                }
                                
                                AnimatedContent(targetState = selectedText, label = "modeLabel") { text ->
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                options.forEachIndexed { index, option ->
                                    val isSelected = performanceMode == option
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { performanceMode = option },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        enabled = isEnabled,
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = when (option) {
                                                "Powersave" -> Icons.Default.BatterySaver
                                                "Balanced" -> Icons.Default.Balance
                                                else -> Icons.Default.Speed
                                            },
                                            contentDescription = option,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // KGSL and Bypass Charging
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // KGSL
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.app_profiles_kgsl_skip_zeroing),
                                        color = if (isKgslFeatureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Switch(
                                        checked = kgslSkipZeroing && isKgslFeatureAvailable,
                                        onCheckedChange = { kgslSkipZeroing = it },
                                        enabled = isKgslFeatureAvailable && isEnabled,
                                        thumbContent = if (kgslSkipZeroing && isKgslFeatureAvailable) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            }
                                        } else {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            // Bypass Charging
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.app_profiles_bypass_charging))
                                    Switch(
                                        checked = bypassCharging,
                                        onCheckedChange = { bypassCharging = it },
                                        enabled = isEnabled,
                                        thumbContent = if (bypassCharging) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            }
                                        } else {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.app_profiles_cancel))
                        }
                        Button(
                            onClick = {
                                onSave(
                                    profile.copy(
                                        performanceMode = performanceMode,
                                        kgslSkipZeroing = kgslSkipZeroing,
                                        bypassCharging = bypassCharging,
                                        isEnabled = isEnabled
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.app_profiles_save))
                        }
                    }
                }
            }
        }
    }
}