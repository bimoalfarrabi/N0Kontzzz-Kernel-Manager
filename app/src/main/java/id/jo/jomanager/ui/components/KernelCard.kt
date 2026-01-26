package id.jo.jomanager.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import id.jo.jomanager.data.model.KernelDetailInfo
import id.jo.jomanager.data.model.KernelInfo
import id.jo.jomanager.ui.dialog.KernelDetailDialog
import id.jo.jomanager.R

@Composable
fun KernelCard(
    k: KernelInfo,
    modifier: Modifier = Modifier
) {
                Card(
                modifier = modifier,
                shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
            ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Section with MD3 styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.kernel),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.kernel),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.system_information),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick info grid with MD3 cards
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Process kernel version to extract clean version info
                val shortenedVersion = shortenKernelVersion(k.version)

                // State untuk menyimpan detail info yang sedang ditampilkan
                var detailInfo by remember { mutableStateOf<KernelDetailInfo?>(null) }
                
                // Tampilkan dialog jika ada detail info
                detailInfo?.let { info ->
                    KernelDetailDialog(
                        detailInfo = info,
                        onDismiss = { detailInfo = null }
                    )
                }

                // Single card: Kernel Version (full width)
                val kernelVersionTitle = stringResource(id = R.string.kernel_version)
                CompactInfoCardWithCustomShape(
                    label = stringResource(R.string.version),
                    value = shortenedVersion,
                    icon = Icons.Filled.Memory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp, 12.dp, 4.dp, 4.dp), // top left, top right, bottom right, bottom left
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = kernelVersionTitle,
                            value = k.version,
                            icon = Icons.Filled.Memory
                        )
                    }
                )

                // Single card: GKI Type (full width)
                val kernelTypeString = stringResource(id = getKernelTypeResByVersion(k.version))
                val kernelTypeTitle = stringResource(id = R.string.kernel_type)
                CompactInfoCardWithCustomShape(
                    label = kernelTypeTitle,
                    value = kernelTypeString,
                    icon = Icons.Filled.Computer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = kernelTypeTitle,
                            value = kernelTypeString,
                            icon = Icons.Filled.Computer
                        )
                    }
                )

                // Single card: Build Fingerprint (full width)
                val buildFingerprintTitle = stringResource(id = R.string.build_fingerprint)
                CompactInfoCardWithCustomShape(
                    label = stringResource(id = R.string.build),
                    value = k.fingerprint.substringAfterLast("/"),
                    icon = Icons.Outlined.Fingerprint,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = buildFingerprintTitle,
                            value = k.fingerprint,
                            icon = Icons.Outlined.Fingerprint
                        )
                    }
                )

                // WireGuard Version
                val wireguardTitle = stringResource(id = R.string.wireguard_version)
                val wireguardValue = k.wireguardVersion ?: stringResource(id = R.string.common_na)
                CompactInfoCardWithCustomShape(
                    label = wireguardTitle,
                    value = wireguardValue,
                    icon = Icons.Filled.Shield, // Reusing Shield icon which fits security/vpn theme
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = wireguardTitle,
                            value = wireguardValue,
                            icon = Icons.Filled.Shield
                        )
                    }
                )

                // Single card: I/O Scheduler (full width)
                val ioSchedulerTitle = stringResource(id = R.string.io_scheduler)
                CompactInfoCardWithCustomShape(
                    label = stringResource(R.string.sched),
                    value = k.scheduler,
                    icon = Icons.Filled.Settings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = ioSchedulerTitle,
                            value = k.scheduler,
                            icon = Icons.Filled.Settings
                        )
                    }
                )

                // Row: ABI and Architecture with 2dp rounded corners on all sides
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // ABI Card (left card) - 8dp on all sides
                    val abiTitle = stringResource(id = R.string.abi)
                    CompactInfoCardWithCustomShape(
                        label = abiTitle,
                        value = k.abi,
                        icon = Icons.Filled.Computer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        onCardClick = {
                            detailInfo = KernelDetailInfo(
                                title = abiTitle,
                                value = k.abi,
                                icon = Icons.Filled.Computer
                            )
                        }
                    )
                    // Architecture Card (right card) - 8dp on all sides
                    val architectureTitle = stringResource(id = R.string.architecture)
                    CompactInfoCardWithCustomShape(
                        label = architectureTitle,
                        value = k.architecture,
                        icon = Icons.Filled.Memory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        onCardClick = {
                            detailInfo = KernelDetailInfo(
                                title = architectureTitle,
                                value = k.architecture,
                                icon = Icons.Filled.Memory
                            )
                        }
                    )
                }

                // Row: SELinux and KernelSU (highlighted with colors) with custom rounded corners
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // SELinux Card (left card) - 8dp top left/right, 8dp bottom left, 24dp bottom right
                    val selinuxTitle = stringResource(id = R.string.selinux)
                    CompactInfoCardWithCustomShape(
                        label = selinuxTitle,
                        value = k.selinuxStatus,
                        icon = Icons.Filled.Shield,
                        valueColor = getSelinuxColor(k.selinuxStatus),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp, 4.dp, 4.dp, 12.dp), // top left, top right, bottom right, bottom left
                        onCardClick = {
                            detailInfo = KernelDetailInfo(
                                title = selinuxTitle,
                                value = k.selinuxStatus,
                                icon = Icons.Filled.Shield
                            )
                        }
                    )
                    // KernelSU Card (right card) - 8dp top left/right, 24dp bottom left, 8dp bottom right
                    val kernelsuTitle = stringResource(id = R.string.kernelsu)
                    CompactInfoCardWithCustomShape(
                        label = kernelsuTitle,
                        value = when {
                            k.kernelSuStatus.contains("Version", ignoreCase = true) -> "✓ " + k.kernelSuStatus.substringAfter("Version ").take(8)
                            k.kernelSuStatus.contains("Active", ignoreCase = true) -> {
                                val inside = k.kernelSuStatus.substringAfter("(", "").substringBefore(")")
                                if (inside.isNotBlank()) "✓ $inside" else stringResource(id = R.string.ksu_active)
                            }
                            k.kernelSuStatus.contains("Detected", ignoreCase = true) -> stringResource(id = R.string.ksu_detected)
                            else -> stringResource(id = R.string.ksu_not_found)
                        },
                        icon = Icons.Filled.AdminPanelSettings,
                        valueColor = getKernelSuColor(k.kernelSuStatus),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp, 4.dp, 12.dp, 4.dp), // top left, top right, bottom right, bottom left
                        onCardClick = {
                            detailInfo = KernelDetailInfo(
                                title = kernelsuTitle,
                                value = k.kernelSuStatus,
                                icon = Icons.Filled.AdminPanelSettings
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with MD3 styling using dynamic colors based on label
            val iconColor = when {
                label.contains("version", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("type", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("abi", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("arch", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("selinux", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("kernelsu", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            
            Surface(
                modifier = Modifier.size(42.dp),
                color = iconColor,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (iconColor == MaterialTheme.colorScheme.errorContainer)
                            MaterialTheme.colorScheme.onErrorContainer
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Jika value tidak kosong, tampilkan label dan value terpisah
                // Jika value kosong, anggap label sudah berisi format lengkap
                if (value.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // Untuk kasus format string seperti "Version: 4.19.0"
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoCardWithCustomShape(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    valueColor: Color? = null,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with MD3 styling using dynamic colors based on label
            val iconColor = when {
                label.contains("version", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("type", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("abi", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("arch", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("selinux", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                label.contains("kernelsu", ignoreCase = true) -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            
            Surface(
                modifier = Modifier.size(42.dp),
                color = iconColor,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (iconColor == MaterialTheme.colorScheme.errorContainer)
                            MaterialTheme.colorScheme.onErrorContainer
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Jika value tidak kosong, tampilkan label dan value terpisah
                // Jika value kosong, anggap label sudah berisi format lengkap
                if (value.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // Untuk kasus format string seperti "Version: 4.19.0"
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
private fun getSelinuxColor(status: String): Color {
    return when (status.lowercase()) {
        "enforcing" -> MaterialTheme.colorScheme.primary // Green tone from dynamic colors
        "permissive" -> MaterialTheme.colorScheme.tertiary // Orange tone from dynamic colors
        "disabled" -> MaterialTheme.colorScheme.error // Red tone from dynamic colors
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun getKernelSuColor(status: String): Color {
    return when {
        status.contains("Version", ignoreCase = true) -> MaterialTheme.colorScheme.primary // Green tone from dynamic colors
        status.contains("Active", ignoreCase = true) -> MaterialTheme.colorScheme.primary // Green tone from dynamic colors
        status.contains("Detected", ignoreCase = true) -> MaterialTheme.colorScheme.secondary // Blue tone from dynamic colors
        status.contains("Not Detected", ignoreCase = true) -> MaterialTheme.colorScheme.outline // Gray tone from dynamic colors
        else -> MaterialTheme.colorScheme.onSurface
    }
}

// Utility function to shorten kernel version format
private fun shortenKernelVersion(version: String): String {
    // Special case for E404R kernel which has empty kernel name
    if (version.contains("4.19.404R", ignoreCase = true) && 
        version.contains("vyn@zorin", ignoreCase = true)) {
        return "4.19.404R"
    }

    // Special case for perf+ kernel by rohmanurip
    if (version.contains("perf+", ignoreCase = true) && 
        version.contains("rohmanurip@Github", ignoreCase = true)) {
         val versionRegex = """Linux version ([\d.]+)""".toRegex()
         val match = versionRegex.find(version)
         val ver = match?.groupValues?.get(1) ?: ""
         return if (ver.isNotEmpty()) "$ver-perf+" else "perf+"
    }

    // Extract version number and kernel name
    val versionRegex = """Linux version ([\d.]+)-([^ ]+)""".toRegex()
    val matchResult = versionRegex.find(version)
    
    return if (matchResult != null) {
        val versionNumber = matchResult.groupValues[1]
        val kernelName = matchResult.groupValues[2]
        "$versionNumber-$kernelName"
    } else {
        // Fallback: try to extract version info before hash or parentheses
        val hashIndex = version.indexOf(" #")
        val parenIndex = version.indexOf(" (")
        val endIndex = when {
            hashIndex != -1 && parenIndex != -1 -> minOf(hashIndex, parenIndex)
            hashIndex != -1 -> hashIndex
            parenIndex != -1 -> parenIndex
            else -> version.length
        }
        version.take(endIndex).trim()
    }
}

// Utility function to determine kernel type based on version
private fun getKernelTypeResByVersion(version: String): Int {
    // Debug logging
    Log.d("KernelCard", "getKernelTypeByVersion called with version: '$version'")
    
    // Extract version number
    val versionRegex = """Linux version ([\d.]+)""".toRegex()
    val matchResult = versionRegex.find(version)
    
    if (matchResult != null) {
        val versionNumber = matchResult.groupValues[1]
        Log.d("KernelCard", "Extracted version number: '$versionNumber'")
        
        // Clean the version number to remove any non-printable characters and normalize dots
        val cleanVersionNumber = versionNumber.trim()
            .replace(Regex("""[^\d.]"""), "") // Hanya biarkan angka dan titik
            .replace("..", ".") // Ganti double titik dengan satu titik (cara manual)
            .replace(Regex("""\.{2,}"""), ".") // Ganti multiple titik dengan satu titik
            .replace(Regex("""[^\x00-\x7F]"""), "") // Hapus karakter non-ASCII
            .trim('.', '.')
        
        // Jika ada multiple titik, ambil hanya bagian sebelum dan setelah titik pertama
        val finalVersionNumber = if (cleanVersionNumber.count { it == '.' } > 1) {
            val firstDotIndex = cleanVersionNumber.indexOf('.')
            val secondDotIndex = cleanVersionNumber.indexOf('.', firstDotIndex + 1)
            if (secondDotIndex != -1) {
                cleanVersionNumber.take(secondDotIndex)
            } else {
                cleanVersionNumber
            }
        } else {
            cleanVersionNumber
        }
        
        // Debug: Print each character and its code
        Log.d("KernelCard", "Final cleaned version number: '$finalVersionNumber'")
        finalVersionNumber.forEachIndexed { index, char ->
            Log.d("KernelCard", "  Character $index: '$char' (code: ${char.code})")
        }
        
        // Parse version as string parts for more accurate comparison
        val versionParts = finalVersionNumber.split(".").map { it.toIntOrNull() ?: 0 }
        Log.d("KernelCard", "Version parts: $versionParts")
        
        val result = when {
            versionParts.size >= 2 && (versionParts[0] < 4 || (versionParts[0] == 4 && versionParts[1] < 19)) -> {
                Log.d("KernelCard", "Returning Legacy")
                R.string.legacy
            }
            versionParts.size >= 2 && (versionParts[0] > 4 || (versionParts[0] == 4 && versionParts[1] >= 19)) && 
            (versionParts[0] < 5 || (versionParts[0] == 5 && versionParts[1] <= 4)) -> {
                Log.d("KernelCard", "Returning GKI1 (4.19-5.4)")
                R.string.gki1
            }
            versionParts.size >= 2 && versionParts[0] == 5 && versionParts[1] > 4 && versionParts[1] < 10 -> {
                Log.d("KernelCard", "Returning GKI1 (5.4-5.10)")
                R.string.gki1
            }
            versionParts.size >= 2 && (versionParts[0] > 5 || (versionParts[0] == 5 && versionParts[1] >= 10)) -> {
                Log.d("KernelCard", "Returning GKI2")
                R.string.gki2
            }
            else -> {
                Log.d("KernelCard", "Returning Unknown (else case)")
                R.string.common_unknown_value
            }
        }
        
        Log.d("KernelCard", "Final result: $result")
        return result
    }
    
    Log.d("KernelCard", "No match found with primary regex")
    
    // Fallback: if we can't parse with the regex, try to extract version in a simpler way
    val simpleVersionRegex = """(\d+\.\d+)""".toRegex()
    val simpleMatch = simpleVersionRegex.find(version)
    
    if (simpleMatch != null) {
        val versionNumber = simpleMatch.groupValues[1]
        Log.d("KernelCard", "Fallback extracted version number: '$versionNumber'")
        
        // Clean the version number to remove any non-printable characters and normalize dots
        val cleanVersionNumber = versionNumber.trim()
            .replace(Regex("""[^\d.]"""), "") // Hanya biarkan angka dan titik
            .replace("..", ".") // Ganti double titik dengan satu titik (cara manual)
            .replace(Regex("""\.{2,}"""), ".") // Ganti multiple titik dengan satu titik
            .replace(Regex("""[^\x00-\x7F]"""), "") // Hapus karakter non-ASCII
            .trim('.', '.')
        
        // Jika ada multiple titik, ambil hanya bagian sebelum dan setelah titik pertama
        val finalVersionNumber = if (cleanVersionNumber.count { it == '.' } > 1) {
            val firstDotIndex = cleanVersionNumber.indexOf('.')
            val secondDotIndex = cleanVersionNumber.indexOf('.', firstDotIndex + 1)
            if (secondDotIndex != -1) {
                cleanVersionNumber.take(secondDotIndex)
            } else {
                cleanVersionNumber
            }
        } else {
            cleanVersionNumber
        }
        
        // Debug: Print each character and its code
        Log.d("KernelCard", "Fallback final cleaned version number: '$finalVersionNumber'")
        finalVersionNumber.forEachIndexed { index, char ->
            Log.d("KernelCard", "  Character $index: '$char' (code: ${char.code})")
        }
        
        // Parse version as string parts for more accurate comparison
        val versionParts = finalVersionNumber.split(".").map { it.toIntOrNull() ?: 0 }
        Log.d("KernelCard", "Fallback version parts: $versionParts")
        
        val result = when {
            versionParts.size >= 2 && (versionParts[0] < 4 || (versionParts[0] == 4 && versionParts[1] < 19)) -> {
                Log.d("KernelCard", "Fallback returning Legacy")
                R.string.legacy
            }
            versionParts.size >= 2 && (versionParts[0] > 4 || (versionParts[0] == 4 && versionParts[1] >= 19)) && 
            (versionParts[0] < 5 || (versionParts[0] == 5 && versionParts[1] <= 4)) -> {
                Log.d("KernelCard", "Fallback returning GKI1 (4.19-5.4)")
                R.string.gki1
            }
            versionParts.size >= 2 && versionParts[0] == 5 && versionParts[1] > 4 && versionParts[1] < 10 -> {
                Log.d("KernelCard", "Fallback returning GKI1 (5.4-5.10)")
                R.string.gki1
            }
            versionParts.size >= 2 && (versionParts[0] > 5 || (versionParts[0] == 5 && versionParts[1] >= 10)) -> {
                Log.d("KernelCard", "Fallback returning GKI2")
                R.string.gki2
            }
            else -> {
                Log.d("KernelCard", "Fallback returning Unknown (else case)")
                R.string.common_unknown_value
            }
        }
        
        Log.d("KernelCard", "Fallback final result: $result")
        return result
    }
    
    Log.d("KernelCard", "No match found with fallback regex, returning Unknown")
    return R.string.common_unknown_value
}