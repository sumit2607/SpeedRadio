package com.speedradio.app.viewmodel

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedradio.app.data.AudioRepository
import com.speedradio.app.domain.AudioPost
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class RecordUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val lastSavedTitle: String? = null,
    val error: String? = null
)

private const val MAX_RECORD_SECONDS = 30

@HiltViewModel
class RecordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var timerJob: Job? = null

    fun startRecording() {
        if (_uiState.value.isRecording) return

        val file = File(context.filesDir, "audio_${System.currentTimeMillis()}.m4a")
        currentFile = file

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setMaxDuration(MAX_RECORD_SECONDS * 1000)
            setOutputFile(file.absolutePath)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecording()
                }
            }
            try {
                prepare()
                start()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed: ${e.message}")
                release()
                recorder = null
                return@apply
            }
        }

        _uiState.value = RecordUiState(isRecording = true)
        startTimer()
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) return
        timerJob?.cancel()

        val elapsed = _uiState.value.elapsedSeconds
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            currentFile?.delete()
            _uiState.value = RecordUiState(error = "Recording failed: ${e.message}")
            recorder = null
            currentFile = null
            return
        }
        recorder = null

        val file = currentFile ?: return
        val durationMs = elapsed * 1000L
        val title = "Clip · ${formatDuration(elapsed)}"
        val post = AudioPost(
            id = UUID.randomUUID().toString(),
            title = title,
            filePath = file.absolutePath,
            durationMs = durationMs
        )
        
        // Persist to local storage database
        viewModelScope.launch {
            repository.addPost(post)
            _uiState.value = RecordUiState(lastSavedTitle = title)
            currentFile = null
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.elapsedSeconds < MAX_RECORD_SECONDS) {
                delay(1_000)
                val next = _uiState.value.elapsedSeconds + 1
                _uiState.value = _uiState.value.copy(elapsedSeconds = next)
                if (next >= MAX_RECORD_SECONDS) {
                    stopRecording()
                    break
                }
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        recorder?.apply { stop(); release() }
        recorder = null
    }
}
