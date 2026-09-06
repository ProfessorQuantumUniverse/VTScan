// scan/ScanManager.kt
package com.quantum_prof.vtscansuite.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.quantum_prof.vtscansuite.data.model.FileReportResponse
import com.quantum_prof.vtscansuite.data.model.SavedScan
import com.quantum_prof.vtscansuite.data.remote.RateLimitException
import com.quantum_prof.vtscansuite.data.repository.PrefsRepository
import com.quantum_prof.vtscansuite.data.repository.SavedScansRepository
import com.quantum_prof.vtscansuite.domain.repository.ProgressCallback
import com.quantum_prof.vtscansuite.domain.repository.ScanPhase
import com.quantum_prof.vtscansuite.domain.repository.VirusTotalRepository
import com.quantum_prof.vtscansuite.domain.usecase.HashFileUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Zustand eines Scans – wird app-weit geteilt und überlebt die Activity. */
sealed interface ScanState {
    data object Idle : ScanState
    data class Running(
        val phase: ScanPhase,
        val progress: Float?,
        val startedAt: Long,
        val label: String
    ) : ScanState
    data class Success(val report: FileReportResponse, val label: String) : ScanState
    data class Error(val message: String, val label: String, val isRateLimit: Boolean = false) : ScanState
}

/**
 * Führt Scans in einem App-weiten Scope aus (überlebt das Schließen der Activity).
 * Zusammen mit [ScanService] (Foreground-Service) läuft der Scan auch weiter,
 * wenn die App in den Hintergrund geht oder geschlossen wird.
 */
@Singleton
class ScanManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VirusTotalRepository,
    private val hashFileUseCase: HashFileUseCase,
    private val prefs: PrefsRepository,
    private val savedScansRepository: SavedScansRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    @Volatile
    var currentLabel: String = ""
        private set

    private var job: Job? = null

    fun scanUrl(url: String) = start(url.trim()) { key, onProgress ->
        repository.scanUrl(key, url, onProgress)
    }

    fun scanApp(apkFile: File, name: String) = start(name) { key, onProgress ->
        analyzeFile(key, apkFile, onProgress, deleteAfter = false)
    }

    fun scanUri(uri: Uri) {
        val name = queryDisplayName(uri) ?: "file"
        start(name) { key, onProgress ->
            onProgress(ScanPhase.CHECKING, null)
            val temp = copyToCache(uri, name)
            analyzeFile(key, temp, onProgress, deleteAfter = true)
        }
    }

    /**
     * Öffnet einen gespeicherten Scan und lädt seine Informationen frisch nach.
     * Schlägt die Aktualisierung fehl (offline/Rate-Limit), wird der gespeicherte
     * Bericht angezeigt. Kein Foreground-Service – das ist eine schnelle In-App-Abfrage.
     */
    fun openSaved(saved: SavedScan) {
        if (job?.isActive == true) return
        currentLabel = saved.label
        job = scope.launch {
            _state.value = ScanState.Running(ScanPhase.FETCHING, null, System.currentTimeMillis(), saved.label)
            val key = prefs.apiKeyFlow.first()?.takeIf { it.isNotBlank() }
            val fresh = if (key.isNullOrBlank()) null
            else repository.getReport(key, saved.id, saved.isUrl).getOrNull()

            if (fresh != null && fresh.data.attributes.lastAnalysisResults.isNotEmpty()) {
                runCatching { savedScansRepository.upsert(saved.copy(report = fresh)) }
                _state.value = ScanState.Success(fresh, saved.label)
            } else {
                _state.value = ScanState.Success(saved.report, saved.label)
            }
        }
    }

    private suspend fun analyzeFile(
        key: String,
        file: File,
        onProgress: ProgressCallback,
        deleteAfter: Boolean
    ): Result<FileReportResponse> {
        return try {
            onProgress(ScanPhase.CHECKING, null)
            val hash = hashFileUseCase(file)
            val known = repository.getFileReport(key, hash).getOrNull()
            if (known != null && known.data.attributes.lastAnalysisResults.isNotEmpty()) {
                Result.success(known)
            } else {
                repository.uploadFile(key, file, onProgress)
            }
        } finally {
            if (deleteAfter) runCatching { file.delete() }
        }
    }

    private fun start(
        label: String,
        block: suspend (key: String, onProgress: ProgressCallback) -> Result<FileReportResponse>
    ) {
        if (job?.isActive == true) return // immer nur ein Scan gleichzeitig
        currentLabel = label
        job = scope.launch {
            val key = prefs.apiKeyFlow.first()?.takeIf { it.isNotBlank() }
            if (key.isNullOrBlank()) {
                _state.value = ScanState.Error("Your API key is missing or invalid.", label)
                return@launch
            }
            _state.value = ScanState.Running(ScanPhase.CHECKING, null, System.currentTimeMillis(), label)
            startService()

            val onProgress: ProgressCallback = { phase, progress ->
                val started = (_state.value as? ScanState.Running)?.startedAt ?: System.currentTimeMillis()
                _state.value = ScanState.Running(phase, progress, started, label)
            }

            val result = runCatching { block(key, onProgress) }.getOrElse { Result.failure(it) }
            _state.value = result.fold(
                onSuccess = { ScanState.Success(it, label) },
                onFailure = {
                    ScanState.Error(
                        message = it.localizedMessage ?: "Scan failed.",
                        label = label,
                        isRateLimit = it is RateLimitException
                    )
                }
            )
        }
    }

    /** Ergebnis als gesehen markieren (nur wenn gerade kein Scan läuft). */
    fun acknowledge() {
        if (_state.value !is ScanState.Running) _state.value = ScanState.Idle
    }

    private fun startService() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, ScanService::class.java))
        }
    }

    // -------- Datei-Hilfsfunktionen --------
    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) name = cursor.getString(idx)
                    }
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    /**
     * Kopiert die gewählte Datei in ein eigenes Unterverzeichnis des Caches.
     * Der Dateiname stammt aus einem fremden ContentProvider und wird deshalb
     * bereinigt – sonst könnte ein Name wie "../databases/x" ausserhalb des
     * Cache-Verzeichnisses landen.
     */
    private fun copyToCache(uri: Uri, name: String): File {
        val dir = File(context.cacheDir, "scan_uploads").apply { mkdirs() }
        val tempFile = File(dir, sanitizeFileName(name))
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Unable to read the selected file.")
        return tempFile
    }

    /** Entfernt Pfadanteile und Sonderzeichen aus einem vom Nutzer/Provider gelieferten Namen. */
    private fun sanitizeFileName(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trimStart('.')
            .take(120)
        return base.ifBlank { "upload" }
    }
}
