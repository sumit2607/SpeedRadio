package com.speedradio.app.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.speedradio.app.domain.AudioPost
import com.speedradio.app.player.PlaybackState
import com.speedradio.app.ui.feed.WaveformAnimation
import com.speedradio.app.viewmodel.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerScreen(
    initialPostId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Discovering...", color = Color.White)
        }
        return
    }

    val initialIndex = posts.indexOfFirst { it.id == initialPostId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { posts.size })

    var lastSeenIndex by remember { mutableStateOf(initialIndex) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            if (posts.isNotEmpty() && index < posts.size) {
                // Only vibrate if this is an actual manual scrolling swipe, not the first load
                if (index != lastSeenIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastSeenIndex = index
                }
                viewModel.playPost(posts[index].id)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val isCurrentPage = playbackState.currentPostId == posts[page].id
            PlayerItem(
                post = posts[page],
                playbackState = if (isCurrentPage) playbackState else PlaybackState(),
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) }
            )
        }

        IconButton(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNavigateBack() 
            },
            modifier = Modifier
                .padding(top = 48.dp, start = 12.dp)
                .align(Alignment.TopStart)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PlayerItem(
    post: AudioPost,
    playbackState: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val isPlaying = playbackState.isPlaying
    val imageUrl = "https://picsum.photos/seed/${post.id.hashCode()}/800/1200"

    // Breathing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onTogglePlay)
    ) {
        // Immersive Backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .scale(1.25f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Glowing Image Card
            Box(contentAlignment = Alignment.Center) {
                if (isPlaying) {
                     Box(
                        modifier = Modifier
                            .size(310.dp)
                            .scale(glowScale)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    )
                }
                
                Card(
                    modifier = Modifier.size(300.dp).aspectRatio(1f),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = post.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "SpeedRadio Original · ${formatDate(post.createdAt)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Modern Progress Section
            PremiumProgressControl(
                playbackState = playbackState,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek
            )
        }
        
        // Visual status feedback
        AnimatedVisibility(
            visible = !isPlaying,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumProgressControl(
    playbackState: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val duration = playbackState.durationMs
    val position = playbackState.positionMs

    var draggingPosition by remember { mutableStateOf<Float?>(null) }
    val currentDisplayPosition = draggingPosition ?: position.toFloat()

    val percentage = if (duration > 0) {
        ((currentDisplayPosition / duration.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Text(
                text = "Live Playback Status",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f)
            )
            
            // Percentage animated state
             Text(
                text = "$percentage%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.5.sp
            )
        }

        Slider(
            value = if (duration > 0) currentDisplayPosition.coerceIn(0f, duration.toFloat()) else 0f,
            onValueChange = { draggingPosition = it },
            onValueChangeFinished = {
                draggingPosition?.let { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeek(it.toLong()) 
                }
                draggingPosition = null
            },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentDisplayPosition.toLong()),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = formatTime(duration),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center primary control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePlay() 
                },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
