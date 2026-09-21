package com.example

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

  private var mediaPlayer: MediaPlayer? = null
  private lateinit var audioManager: AudioManager

  // Observable state for Compose UI
  private val isPlayingState = mutableStateOf(false)
  private val isPausedState = mutableStateOf(false)
  private val currentVolumeState = mutableIntStateOf(0)
  private val maxVolumeState = mutableIntStateOf(15)
  private val statusMessageState = mutableStateOf("Menyiapkan audio...")
  private val playbackProgressState = mutableFloatStateOf(0f)

  companion object {
    private const val TAG = "AudioPrank"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    maxVolumeState.intValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    currentVolumeState.intValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    // Set volume ke 100% dan langsung putar audio secara otomatis saat Activity pertama kali dimuat
    setMaxVolumeAndPlay()

    setContent {
      MyApplicationTheme {
        AudioPrankScreen(
          isPlaying = isPlayingState.value,
          isPaused = isPausedState.value,
          currentVolume = currentVolumeState.intValue,
          maxVolume = maxVolumeState.intValue,
          statusMessage = statusMessageState.value,
          playbackProgress = playbackProgressState.floatValue,
          onPlayAgainClick = { setMaxVolumeAndPlay() },
          onPauseResumeClick = { togglePauseResume() },
          onStopClick = { stopAudio() },
          onSetMaxVolumeClick = { setVolumeToMaxOnly() },
          onVolumeChange = { newVolume -> setCustomVolume(newVolume) }
        )
      }
    }
  }

  /**
   * 1. Mengatur Stream Volume Media (STREAM_MUSIC) ke tingkat maksimum (100%).
   * 2. Langsung memutar audio bawaan dari res/raw/custom_sound.mp3 menggunakan MediaPlayer.
   */
  private fun setMaxVolumeAndPlay() {
    try {
      val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
      maxVolumeState.intValue = maxVol
      // Set volume stream media ke maksimum (100%) dengan feedback UI sistem
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
      currentVolumeState.intValue = maxVol

      // Bersihkan media player sebelumnya jika ada
      releaseMediaPlayer()

      // Baca file audio dari res/raw/prankk.mp3 (atau fallback custom_sound jika ada)
      val audioResId = R.raw.prankk
      mediaPlayer = MediaPlayer.create(this, audioResId)?.apply {
        setOnCompletionListener {
          isPlayingState.value = false
          isPausedState.value = false
          playbackProgressState.floatValue = 1f
          statusMessageState.value = "Audio Selesai Diputar"
          Log.d(TAG, "Audio playback completed.")
        }
        setOnErrorListener { _, what, extra ->
          Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
          statusMessageState.value = "Error memutar audio (Kode: $what)"
          isPlayingState.value = false
          isPausedState.value = false
          true
        }
        start()
      }

      if (mediaPlayer != null) {
        isPlayingState.value = true
        isPausedState.value = false
        playbackProgressState.floatValue = 0f
        statusMessageState.value = "Volume 100% - Memutar Prankk.mp3"
        Log.d(TAG, "Audio started at maximum volume ($maxVol).")
      } else {
        statusMessageState.value = "File res/raw/prankk.mp3 tidak ditemukan"
        Log.e(TAG, "Failed to create MediaPlayer from R.raw.prankk")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception during setMaxVolumeAndPlay", e)
      statusMessageState.value = "Gagal memutar audio: ${e.localizedMessage}"
    }
  }

  private fun togglePauseResume() {
    try {
      mediaPlayer?.let { player ->
        if (player.isPlaying) {
          player.pause()
          isPlayingState.value = false
          isPausedState.value = true
          statusMessageState.value = "Audio Dijeda (Paused)"
        } else if (isPausedState.value) {
          player.start()
          isPlayingState.value = true
          isPausedState.value = false
          statusMessageState.value = "Melanjutkan Audio..."
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception in togglePauseResume", e)
    }
  }

  private fun stopAudio() {
    try {
      mediaPlayer?.let { player ->
        if (player.isPlaying || isPausedState.value) {
          player.stop()
        }
      }
      releaseMediaPlayer()
      isPlayingState.value = false
      isPausedState.value = false
      playbackProgressState.floatValue = 0f
      statusMessageState.value = "Audio Berhenti (Stopped)"
    } catch (e: Exception) {
      Log.e(TAG, "Exception in stopAudio", e)
    }
  }

  private fun setVolumeToMaxOnly() {
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
    currentVolumeState.intValue = maxVol
  }

  private fun setCustomVolume(level: Int) {
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, AudioManager.FLAG_SHOW_UI)
    currentVolumeState.intValue = level
  }

  private fun releaseMediaPlayer() {
    try {
      mediaPlayer?.let { player ->
        player.stop()
        player.release()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Exception releasing MediaPlayer: ${e.message}")
    } finally {
      mediaPlayer = null
    }
  }

  /**
   * Manajemen Lifecycle:
   * Memastikan MediaPlayer di-release saat Activity masuk ke background (onStop)
   * atau ditutup (onDestroy) agar tidak bocor di memory dan audio tidak berjalan liar.
   */
  override fun onStop() {
    super.onStop()
    Log.d(TAG, "onStop: Menghentikan audio dan membersihkan memori")
    stopAudio()
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, "onDestroy: Memastikan MediaPlayer dibersihkan total")
    releaseMediaPlayer()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPrankScreen(
  isPlaying: Boolean,
  isPaused: Boolean,
  currentVolume: Int,
  maxVolume: Int,
  statusMessage: String,
  playbackProgress: Float,
  onPlayAgainClick: () -> Unit,
  onPauseResumeClick: () -> Unit,
  onStopClick: () -> Unit,
  onSetMaxVolumeClick: () -> Unit,
  onVolumeChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val volumePercent = if (maxVolume > 0) ((currentVolume.toFloat() / maxVolume) * 100).toInt() else 0

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "Audio Prank Controller",
            fontWeight = FontWeight.Bold
          )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // Volume Status Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (volumePercent >= 100) {
            MaterialTheme.colorScheme.errorContainer
          } else {
            MaterialTheme.colorScheme.secondaryContainer
          }
        )
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Volume Icon",
                modifier = Modifier.size(28.dp),
                tint = if (volumePercent >= 100) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                }
              )
              Text(
                text = "STREAM_MUSIC",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (volumePercent >= 100) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.primary
              }
            ) {
              Text(
                text = "$volumePercent% (MAX)",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onError,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
          }

          LinearProgressIndicator(
            progress = { if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 1f },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp)),
            color = if (volumePercent >= 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Level: $currentVolume / $maxVolume",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
              onClick = onSetMaxVolumeClick,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .height(36.dp)
                .testTag("btn_set_max_volume"),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
              )
            ) {
              Text("Set Max 100%", fontSize = 12.sp)
            }
          }
        }
      }

      // Playback Status & Equalizer Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Animated Wave Equalizer
          AnimatedEqualizer(isPlaying = isPlaying)

          Text(
            text = statusMessage,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
          ) {
            Text(
              text = "Target Audio: res/raw/prankk.mp3",
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // Primary Action: Play Again
      Button(
        onClick = onPlayAgainClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("btn_play_again"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(
          imageVector = Icons.Default.Replay,
          contentDescription = "Play Again Icon",
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Play Again (Volume 100%)",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Secondary Controls Row: Pause / Resume & Stop
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        FilledTonalButton(
          onClick = onPauseResumeClick,
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("btn_pause_resume"),
          shape = RoundedCornerShape(14.dp),
          enabled = isPlaying || isPaused
        ) {
          Icon(
            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = if (isPaused) "Resume Icon" else "Pause Icon"
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = if (isPaused) "Resume" else "Pause")
        }

        OutlinedButton(
          onClick = onStopClick,
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("btn_stop"),
          shape = RoundedCornerShape(14.dp),
          enabled = isPlaying || isPaused
        ) {
          Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Stop Icon"
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Stop")
        }
      }

      // Information Card on where to put the custom audio file
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info Icon",
            tint = MaterialTheme.colorScheme.primary
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Lokasi File Audio di Android Studio:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "app/src/main/res/raw/custom_sound.mp3",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "• Letakkan file MP3 Anda di folder res/raw/.\n• Buat folder 'raw' jika belum ada di dalam 'res'.\n• Gunakan huruf kecil tanpa spasi (misal: custom_sound.mp3).\n• MediaPlayer otomatis di-release pada onStop() & onDestroy().",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

/**
 * Animated equalizer visualizer bars for playing audio
 */
@Composable
fun AnimatedEqualizer(
  isPlaying: Boolean,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "equalizer")

  val height1 by transition.animateFloat(
    initialValue = 14f,
    targetValue = if (isPlaying) 52f else 14f,
    animationSpec = infiniteRepeatable(
      animation = tween(400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h1"
  )
  val height2 by transition.animateFloat(
    initialValue = 24f,
    targetValue = if (isPlaying) 68f else 18f,
    animationSpec = infiniteRepeatable(
      animation = tween(280),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h2"
  )
  val height3 by transition.animateFloat(
    initialValue = 12f,
    targetValue = if (isPlaying) 60f else 16f,
    animationSpec = infiniteRepeatable(
      animation = tween(350),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h3"
  )
  val height4 by transition.animateFloat(
    initialValue = 18f,
    targetValue = if (isPlaying) 48f else 14f,
    animationSpec = infiniteRepeatable(
      animation = tween(420),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h4"
  )
  val height5 by transition.animateFloat(
    initialValue = 10f,
    targetValue = if (isPlaying) 58f else 12f,
    animationSpec = infiniteRepeatable(
      animation = tween(310),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h5"
  )

  val heights = listOf(height1, height2, height3, height4, height5)

  Row(
    modifier = modifier.height(72.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.Bottom
  ) {
    heights.forEach { h ->
      Box(
        modifier = Modifier
          .width(10.dp)
          .height(h.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(
            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
          )
      )
    }
  }
}

/**
 * Backward-compatible Greeting Composable for tests
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun AudioPrankScreenPreview() {
  MyApplicationTheme {
    AudioPrankScreen(
      isPlaying = true,
      isPaused = false,
      currentVolume = 15,
      maxVolume = 15,
      statusMessage = "Volume 100% - Memutar custom_sound.mp3",
      playbackProgress = 0.5f,
      onPlayAgainClick = {},
      onPauseResumeClick = {},
      onStopClick = {},
      onSetMaxVolumeClick = {},
      onVolumeChange = {}
    )
  }
}
