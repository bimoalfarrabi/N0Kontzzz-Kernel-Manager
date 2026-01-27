package id.jo.jomanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues // Added PaddingValues import
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import id.jo.jomanager.R

data class Developer(val name: Int, val role: Int, val telegramUsername: String?, val drawableResId: Int)
data class RepositoryContributor(val name: Int, val url: String, val description: Int, val drawableResId: Int)

// Special recognition for the main developer who led this rebrand
val leadDeveloper = Developer(R.string.Jo, R.string.jo_kernel__developer, "Jo_0012", R.drawable.jo)

val repositoryContributors = listOf(
    RepositoryContributor(R.string.nkm_kernel_manager_repo, "https://github.com/bimoalfarrabi/N0Kontzzz-Kernel-Manager", R.string.original_project_repo, R.drawable.viasco)
)

@Composable
fun AboutCard(
    blur: Boolean,
    modifier: Modifier = Modifier,
    githubLink: String = stringResource(R.string.github_link),
    telegramLink: String = stringResource(R.string.telegram_link),
) {
    var showCreditsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 16.dp, 16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.nkm_png),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(48.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.about),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Description
            Text(
                text = stringResource(id = R.string.desc_about),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            // Action Section
            Column {
                // Social Links Row
                val uriHandler = LocalUriHandler.current
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.follow_us),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    // Telegram Button
                    IconButton(
                        onClick = { uriHandler.openUri(telegramLink) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = stringResource(id = R.string.telegram),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Add spacing between social links and credits badge
                Spacer(modifier = Modifier.height(8.dp))

                // Source Code Button
                val sourceCodeLink = stringResource(R.string.source_code_link)
                OutlinedButton(
                    onClick = { uriHandler.openUri(sourceCodeLink) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.source_code),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                // Add spacing between Source Code and Credits badge
                Spacer(modifier = Modifier.height(8.dp))

                // Credits Badge
                OutlinedButton(
                    onClick = { showCreditsDialog = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.credits),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    if (showCreditsDialog) {
    AnimatedVisibility(
        visible = showCreditsDialog,
        enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        exit = fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    ) {
            AlertDialog(
                onDismissRequest = { showCreditsDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(id = R.string.credits))
                    }
                },
                text = {
                    val scrollState = rememberScrollState()
                    var isScrollbarVisible by remember { mutableStateOf(true) }
                    
                    LaunchedEffect(scrollState) {
                        snapshotFlow { scrollState.value }
                            .collect {
                                isScrollbarVisible = true
                                delay(1000) // Hide scrollbar after 1 second of inactivity
                                isScrollbarVisible = false
                            }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(end = 12.dp), // Add padding to make space for scrollbar
                            verticalArrangement = Arrangement.spacedBy(0.dp) // Remove default spacing since we use custom Spacer
                        ) {
                            // Lead Developer Section
                            Text(
                                text = stringResource(id = R.string.developer),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp)) // Add spacing between title and first card
                            
                            DeveloperCreditItem(developer = leadDeveloper, position = 0, totalItems = 1)
                            
                            // Separator
                            Spacer(modifier = Modifier.height(16.dp)) // Reduce spacing before section title
                            
                            // Repository Contributors Section
                            Spacer(modifier = Modifier.height(16.dp)) // Reduce spacing before section title
                            
                            Text(
                                text = stringResource(id = R.string.repository_contributors),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp)) // Add spacing between title and first card
                            
                            repositoryContributors.forEachIndexed { index, repo ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                RepositoryCreditItem(repository = repo, position = index, totalItems = repositoryContributors.size)
                            }
                            
                            // Add some padding at the end
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Custom scrollbar with fade animation - properly positioned on the right
                        // Using a separate Box to position the entire scrollbar component
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        ) {
                            // Track - always present but only visible when animated container is visible
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(6.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = isScrollbarVisible,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                                ) {
                                    // Track background
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(6.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                            
                            // Thumb - separate element that can be positioned independently
                            val thumbOffset by remember {
                                derivedStateOf {
                                    if (scrollState.maxValue > 0) {
                                        val trackHeight = 400f // Total height of the scrollbar track
                                        val thumbHeight = 30f // Height of the thumb
                                        val availableTrackHeight = trackHeight - thumbHeight
                                        val ratio = scrollState.value.toFloat() / scrollState.maxValue
                                        (availableTrackHeight * ratio).dp
                                    } else 0.dp
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd) // Position at top-right of parent Box
                                    .offset(y = thumbOffset)
                                    .width(6.dp)
                                    .height(30.dp) // Fixed height for thumb
                            ) {
                                AnimatedVisibility(
                                    visible = isScrollbarVisible,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .background(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showCreditsDialog = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
fun DeveloperCreditItem(developer: Developer, position: Int, totalItems: Int) {
    val uriHandler = LocalUriHandler.current
    
    // لينك تليجرام فقط (لو موجود)
    val telegramUrl = developer.telegramUsername?.let { "https://t.me/$it" }
    
    val shape = when (position) {
        0 if position == totalItems - 1 -> RoundedCornerShape(24.dp)
        0 -> RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp)
        totalItems - 1 -> RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                telegramUrl?.let { uriHandler.openUri(it) } // يفتح تليجرام لو موجود
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = developer.drawableResId),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = developer.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = developer.role),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (developer.telegramUsername != null) {
                    Text(
                        text = "t.me/${developer.telegramUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "No Telegram",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RepositoryCreditItem(repository: RepositoryContributor, position: Int, totalItems: Int) {
    val uriHandler = LocalUriHandler.current
    
    // Determine rounded corners based on position
    val shape = when (position) {
        0 if position == totalItems - 1 -> RoundedCornerShape(24.dp) // Only item
        0 -> RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp) // First item: top corners 24dp, bottom 8dp
        totalItems - 1 -> RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp) // Last item: top corners 8dp, bottom 24dp
        else -> RoundedCornerShape(4.dp) // Middle items: all corners 8dp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(repository.url) },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Repository icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Image(
                    painter = painterResource(id = repository.drawableResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = repository.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = repository.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.github),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
