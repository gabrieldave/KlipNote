package com.blackeye.klipnote

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build
import com.blackeye.klipnote.ui.KlipNeutrals
import com.blackeye.klipnote.ui.KlipPalette
import com.blackeye.klipnote.ui.KlipTheme
import com.blackeye.klipnote.ui.KlipThemeId
import com.blackeye.klipnote.ui.ThemePrefs
private val Bg get() = KlipNeutrals.Bg
private val Card get() = KlipNeutrals.Card
private val Accent get() = KlipPalette.accent
private val OnAccent get() = KlipPalette.onAccent
private val OnBg get() = KlipNeutrals.OnBg
private val Muted get() = KlipNeutrals.Muted
private val Amber get() = KlipNeutrals.Amber

private enum class MainTab { Capturar, Guiones, Motores }

sealed class UiPhase {
    data object Idle : UiPhase()
    data class Downloading(val label: String) : UiPhase()
    data object Extracting : UiPhase()
    data class Transcribing(val motorHint: String = "") : UiPhase()
    data class Done(val meta: TranscriptionResult) : UiPhase()
    data class Error(
        val message: String,
        val isManualTest: Boolean = false
    ) : UiPhase()
}

data class PreFlightState(
    val uri: Uri,
    val fileName: String,
    val durationSeconds: Double,
    val estimate: BillingManager.CostEstimate,
    val hardStopMessage: String?,
    val needsGiantConfirm: Boolean,
    val estimatedUsdDisplay: Double,
    val routeHint: String
)

class MainActivity : AppCompatActivity() {

    private var pendingShareUri by mutableStateOf<Uri?>(null)
    private var pendingShareText by mutableStateOf<String?>(null)
    private var shareNonce by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KlipNoteNotify.ensureChannels(this)
        KlipNoteNotify.clearOrphanProgressIfNeeded(this)
        TranscriptHistoryStore.migrateFromBillingIfNeeded(this)
        handleShareIntent(intent)
        setContent {
            val appContext = this@MainActivity
            var themeId by remember {
                mutableStateOf(ThemePrefs.getThemeId(appContext))
            }
            var showOnboarding by remember {
                mutableStateOf(
                    !ApiKeyStore.hasAnyKey(appContext) && !OnboardingPrefs.isDone(appContext)
                )
            }
            KlipTheme(themeId = themeId) {
                // Asistente = pantalla independiente (no Scaffold / tabs).
                if (showOnboarding) {
                    OnboardingWizard(
                        onFinished = { showOnboarding = false },
                        onSavedKey = { /* KlipNoteApp recarga llaves al montar */ }
                    )
                } else {
                    KlipNoteApp(
                        shareUri = pendingShareUri,
                        shareText = pendingShareText,
                        shareNonce = shareNonce,
                        themeId = themeId,
                        onThemeIdChange = { id ->
                            themeId = id
                            ThemePrefs.setThemeId(appContext, id)
                        },
                        onShowOnboarding = { showOnboarding = true },
                        onShareConsumed = {
                            pendingShareUri = null
                            pendingShareText = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(KlipNoteNotify.EXTRA_OPEN_RESULT, false)) {
            // Abrir en Capturar; el JobHub ya habrá rellenado el resultado si aplica.
            return
        }
        if (intent.action != Intent.ACTION_SEND) return
        val uri = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
        if (uri != null) {
            pendingShareUri = uri
            shareNonce += 1
            return
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            pendingShareText = text
            shareNonce += 1
        }
    }
}

@Suppress("DEPRECATION")
private fun Intent.getParcelableExtraCompat(key: String): Uri? {
    return if (android.os.Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(key, Uri::class.java)
    } else {
        getParcelableExtra(key) as? Uri
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KlipNoteApp(
    shareUri: Uri?,
    shareText: String?,
    shareNonce: Int,
    themeId: KlipThemeId,
    onThemeIdChange: (KlipThemeId) -> Unit,
    onShowOnboarding: () -> Unit,
    onShareConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val sealReady by SealDownloader.ready.collectAsState()

    var selectedTab by remember { mutableStateOf(MainTab.Capturar) }
    var showMotorSheet by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keysFocusProvider by remember { mutableStateOf<SttProvider?>(null) }
    // showOnboarding vive en setContent (pantalla independiente).
    var languageMode by remember { mutableStateOf(BillingManager.getLanguageMode(context)) }
    var forcedLanguage by remember { mutableStateOf(BillingManager.getForcedLanguage(context)) }
    var diarize by remember { mutableStateOf(BillingManager.getDiarizeEnabled(context)) }
    var phase by remember { mutableStateOf<UiPhase>(UiPhase.Idle) }
    var linkInput by remember { mutableStateOf("") }
    var linkMode by remember { mutableStateOf(SealDownloader.LinkMode.TEXT_ONLY) }
    var downloadKind by remember { mutableStateOf(SealDownloader.DownloadKind.AUDIO_M4A) }
    var updatingYtDlp by remember { mutableStateOf(false) }
    /** Sitio cuya hoja de login está abierta (null = cerrada). */
    var cookieSheetSite by remember { mutableStateOf<CookieSite?>(null) }
    /** Sitio del último enlace que falló por login (para el botón del error en Capturar). */
    var cookieSiteHint by remember { mutableStateOf(CookieSite.INSTAGRAM) }
    var cookiesRevision by remember { mutableStateOf(0) }
    var transcriptText by remember { mutableStateOf("") }
    var lastResultMeta by remember { mutableStateOf<TranscriptionResult?>(null) }
    var copyFeedback by remember { mutableStateOf(false) }
    var lastSourceLabel by remember { mutableStateOf("") }

    var creditExpanded by remember { mutableStateOf(true) }
    var creditRefreshing by remember { mutableStateOf(false) }
    var verifyingOffers by remember { mutableStateOf(false) }
    var balanceSnapshot by remember {
        mutableStateOf<DeepgramBillingClient.BalanceSnapshot?>(null)
    }
    var providerStatuses by remember {
        mutableStateOf(OfferHealthMonitor.seedFromStore(context))
    }
    var keyMasks by remember {
        mutableStateOf(readKeyMasks(context))
    }
    var routingSelection by remember { mutableStateOf<RoutingSelection>(RoutingSelection.Auto) }
    var routeChain by remember {
        mutableStateOf(SttRouter.summarizeActiveRoute(context, RoutingSelection.Auto).chainLabel)
    }
    var autoPrimary by remember {
        mutableStateOf(SttRouter.summarizeActiveRoute(context, RoutingSelection.Auto).primaryProvider)
    }
    var appSpent by remember { mutableStateOf(BillingManager.getAppSpent(context)) }
    var localUsage by remember { mutableStateOf(BillingManager.getLocalUsage(context)) }
    var includeDiarizeCost by remember {
        mutableStateOf(BillingManager.getIncludeDiarizeCost(context))
    }
    var autoTranscribeOnShare by remember {
        mutableStateOf(BillingManager.getAutoTranscribeOnShare(context))
    }
    var engineEnabled by remember {
        mutableStateOf(SttProvider.entries.associateWith { BillingManager.isEngineEnabled(context, it) })
    }
    var allowPaidFallback by remember {
        mutableStateOf(BillingManager.getAllowPaidFallback(context))
    }
    var initialCredit by remember { mutableStateOf(BillingManager.getInitialCredit(context)) }
    var appCap by remember { mutableStateOf(BillingManager.getAppCap(context)) }
    var minReserve by remember { mutableStateOf(BillingManager.getMinReserve(context)) }
    var groqRemaining by remember { mutableStateOf(BillingManager.getGroqRemainingSeconds(context)) }
    var groqLimit by remember { mutableStateOf(BillingManager.getGroqLimitSeconds(context)) }
    var groqUsedToday by remember { mutableStateOf(BillingManager.getGroqUsedTodaySeconds(context)) }
    var groqHasLive by remember { mutableStateOf(BillingManager.hasGroqLiveQuota(context)) }
    var inworldRemaining by remember {
        mutableStateOf(BillingManager.getInworldRemainingSeconds(context))
    }
    var assemblyRemaining by remember {
        mutableStateOf(BillingManager.getAssemblyRemainingSeconds(context))
    }

    var preFlight by remember { mutableStateOf<PreFlightState?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showCostSettings by remember { mutableStateOf(false) }
    var giantConfirm by remember { mutableStateOf<PreFlightState?>(null) }
    var historyEntries by remember { mutableStateOf(BillingManager.loadHistory(context)) }
    var transcriptEntries by remember { mutableStateOf(TranscriptHistoryStore.load(context)) }
    var guionSearch by remember { mutableStateOf("") }
    var expandedGuionId by remember { mutableStateOf<String?>(null) }
    var lastSavedMedia by remember { mutableStateOf<SavedMediaInfo?>(null) }
    var notifyWhenDone by remember { mutableStateOf(KlipNoteNotify.isNotifyEnabled(context)) }
    var showNotifPermissionRationale by remember { mutableStateOf(false) }
    var pendingLinkAfterNotif by remember { mutableStateOf<String?>(null) }

    fun launchLinkJob(url: String) {
        phase = UiPhase.Downloading(context.getString(R.string.status_downloading_percent, 0))
        lastSavedMedia = null
        KlipNoteWorkService.startLinkJob(
            context = context,
            url = url,
            mode = linkMode,
            kind = downloadKind,
            languageMode = languageMode,
            forcedLanguage = forcedLanguage,
            diarize = diarize,
            routingSelection = routingSelection
        )
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        KlipNoteNotify.markAskedNotificationPermission(context)
        if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_notifications_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
        val pending = pendingLinkAfterNotif
        pendingLinkAfterNotif = null
        if (pending != null) launchLinkJob(pending)
    }

    fun syncKeysUi() {
        keyMasks = readKeyMasks(context)
        engineEnabled = SttProvider.entries.associateWith { BillingManager.isEngineEnabled(context, it) }
        providerStatuses = OfferHealthMonitor.reconcileWithStore(context, providerStatuses)
        val summary = SttRouter.summarizeActiveRoute(context, routingSelection)
        routeChain = summary.chainLabel
        autoPrimary = when (routingSelection) {
            is RoutingSelection.Auto -> summary.primaryProvider
            is RoutingSelection.Manual -> summary.primaryProvider
        }
    }

    fun setRouting(selection: RoutingSelection) {
        routingSelection = selection
        val summary = SttRouter.summarizeActiveRoute(context, selection)
        routeChain = summary.chainLabel
        autoPrimary = summary.primaryProvider
        val err = phase
        if (err is UiPhase.Error && err.isManualTest && selection is RoutingSelection.Auto) {
            phase = UiPhase.Idle
        }
    }

    fun refreshDashboard() {
        scope.launch {
            creditRefreshing = true
            syncKeysUi()
            withContext(Dispatchers.IO) {
                RemoteRouterConfig.refreshQuietly(context)
            }
            val key = ApiKeyStore.getDeepgram(context)
            val snap = withContext(Dispatchers.IO) {
                DeepgramBillingClient.fetchOfficialBalance(context, key)
            }
            balanceSnapshot = snap
            appSpent = BillingManager.getAppSpent(context)
            localUsage = BillingManager.getLocalUsage(context)
            groqRemaining = BillingManager.getGroqRemainingSeconds(context)
            groqLimit = BillingManager.getGroqLimitSeconds(context)
            groqUsedToday = BillingManager.getGroqUsedTodaySeconds(context)
            groqHasLive = BillingManager.hasGroqLiveQuota(context)
            inworldRemaining = BillingManager.getInworldRemainingSeconds(context)
            assemblyRemaining = BillingManager.getAssemblyRemainingSeconds(context)
            syncKeysUi()
            historyEntries = BillingManager.loadHistory(context)
            allowPaidFallback = BillingManager.getAllowPaidFallback(context)
            creditRefreshing = false
        }
    }

    fun verifyOffers(showSnackbar: Boolean = true) {
        scope.launch {
            verifyingOffers = true
            syncKeysUi()
            val report = withContext(Dispatchers.IO) {
                OfferHealthMonitor.verifyAll(context)
            }
            providerStatuses = report.statuses
            keyMasks = readKeyMasks(context)
            groqRemaining = BillingManager.getGroqRemainingSeconds(context)
            groqLimit = BillingManager.getGroqLimitSeconds(context)
            groqUsedToday = BillingManager.getGroqUsedTodaySeconds(context)
            groqHasLive = BillingManager.hasGroqLiveQuota(context)
            inworldRemaining = BillingManager.getInworldRemainingSeconds(context)
            assemblyRemaining = BillingManager.getAssemblyRemainingSeconds(context)
            syncKeysUi()
            verifyingOffers = false
            if (showSnackbar) {
                snackbar.showSnackbar(context.getString(R.string.snackbar_offers_verified))
            }
        }
    }

    fun onKeysChanged() {
        syncKeysUi()
        verifyOffers(showSnackbar = false)
    }

    fun notifyCopied() {
        copyFeedback = true
        scope.launch {
            snackbar.showSnackbar(context.getString(R.string.copied))
            copyFeedback = false
        }
    }

    fun buildPreFlight(
        uri: Uri,
        mode: LanguageMode = languageMode,
        diarizeOn: Boolean = diarize,
        includeDiarize: Boolean = includeDiarizeCost,
        officialBalance: Double? = balanceSnapshot?.balanceUsd
    ): PreFlightState {
        val probe = BillingManager.probeMedia(context, uri)
        val route = SttRouter.summarizeActiveRoute(context, routingSelection)
        val estimate = BillingManager.estimate(
            durationSeconds = probe.durationSeconds,
            mode = mode,
            diarizeEnabled = diarizeOn,
            includeDiarizeInEstimate = includeDiarize
        )
        val displayUsd = if (route.hasFreePath) 0.0 else estimate.estimatedUsd
        val stop = BillingManager.checkHardStop(
            context = context,
            estimatedUsd = displayUsd,
            officialBalance = officialBalance,
            hasFreePath = route.hasFreePath
        )
        val hardMsg = when (stop) {
            is BillingManager.HardStop.ReserveBreach ->
                context.getString(R.string.hard_stop_reserve, BillingManager.formatUsd(stop.reserve, 2))
            is BillingManager.HardStop.AppCapBreach ->
                context.getString(R.string.hard_stop_cap)
            is BillingManager.HardStop.StrictZeroMode -> stop.message
            BillingManager.HardStop.Ok -> null
        }
        return PreFlightState(
            uri = uri,
            fileName = probe.displayName,
            durationSeconds = probe.durationSeconds,
            estimate = estimate,
            hardStopMessage = hardMsg,
            needsGiantConfirm = !route.hasFreePath && BillingManager.isGiantFile(displayUsd),
            estimatedUsdDisplay = displayUsd,
            routeHint = route.chainLabel
        )
    }

    /** Respaldo UI: no duplica si el servicio ya guardó con el mismo [historyId] o texto. */
    fun persistGuion(
        source: String,
        result: TranscriptionResult,
        historyId: String? = null
    ) {
        if (result.transcript.isBlank()) return
        val linkUrl = SealDownloader.extractFirstUrl(source)
        transcriptEntries = if (!historyId.isNullOrBlank()) {
            TranscriptHistoryStore.upsertById(
                context = context,
                id = historyId,
                sourceTitleOrUrl = source,
                providerUsed = result.providerLabel,
                durationSec = result.durationSeconds ?: 0.0,
                text = result.transcript,
                sourceUrl = linkUrl
            )
        } else {
            TranscriptHistoryStore.append(
                context = context,
                sourceTitleOrUrl = source,
                providerUsed = result.providerLabel,
                durationSec = result.durationSeconds ?: 0.0,
                text = result.transcript,
                sourceUrl = linkUrl
            )
        }
    }

    fun startTranscription(uri: Uri, fileName: String) {
        val selectionSnap = routingSelection
        lastSourceLabel = fileName
        scope.launch {
            runTranscription(
                context = context,
                uri = uri,
                fileName = fileName,
                mode = languageMode,
                forcedLanguageCode = forcedLanguage,
                diarize = diarize,
                routingSelection = selectionSnap,
                // Galería / compartir: comprimir copia STT para videos largos con solo Groq.
                compressForStt = true,
                savedInDownloads = false,
                onPhase = { phase = it },
                onText = { transcriptText = it },
                onMeta = { meta, historyId ->
                    lastResultMeta = meta
                    persistGuion(fileName, meta, historyId)
                },
                onBalance = { balanceSnapshot = it },
                onAppSpent = {
                    appSpent = it
                    localUsage = BillingManager.getLocalUsage(context)
                    groqRemaining = BillingManager.getGroqRemainingSeconds(context)
                    groqLimit = BillingManager.getGroqLimitSeconds(context)
                    groqUsedToday = BillingManager.getGroqUsedTodaySeconds(context)
                    groqHasLive = BillingManager.hasGroqLiveQuota(context)
                    inworldRemaining = BillingManager.getInworldRemainingSeconds(context)
                    assemblyRemaining = BillingManager.getAssemblyRemainingSeconds(context)
                    syncKeysUi()
                },
                onHistoryChanged = { historyEntries = it },
                snackbar = snackbar
            )
        }
    }

    fun startFromLink(raw: String, auto: Boolean = false) {
        val url = SealDownloader.extractFirstUrl(raw)
        if (url.isNullOrBlank()) {
            if (!auto) {
                Toast.makeText(context, context.getString(R.string.toast_invalid_url), Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (KlipNoteJobHub.isBusy()) {
            Toast.makeText(context, context.getString(R.string.toast_job_already_running), Toast.LENGTH_SHORT).show()
            return
        }
        linkInput = url
        lastSourceLabel = url
        val needsStt = linkMode != SealDownloader.LinkMode.DOWNLOAD_ONLY
        if (needsStt && !ApiKeyStore.hasAnyKey(context)) {
            showKeyDialog = true
            selectedTab = MainTab.Motores
            Toast.makeText(context, context.getString(R.string.error_no_key), Toast.LENGTH_LONG).show()
            return
        }
        if (sealReady != SealDownloader.ReadyState.READY) {
            Toast.makeText(
                context,
                if (sealReady == SealDownloader.ReadyState.ERROR) {
                    context.getString(
                        R.string.toast_ytdlp_error,
                        SealDownloader.readyError.value ?: context.getString(R.string.error_generic)
                    )
                } else {
                    context.getString(R.string.toast_ytdlp_init)
                },
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Android 13+: pedir notificaciones la 1ª vez (si el usuario quiere avisos).
        if (Build.VERSION.SDK_INT >= 33 &&
            KlipNoteNotify.isNotifyEnabled(context) &&
            !KlipNoteNotify.canPostNotifications(context) &&
            !KlipNoteNotify.hasAskedNotificationPermission(context)
        ) {
            pendingLinkAfterNotif = url
            showNotifPermissionRationale = true
            return
        }
        launchLinkJob(url)
    }

    fun cancelWork() {
        KlipNoteWorkService.cancel(context)
        SealDownloader.cancelActive()
        if (phase is UiPhase.Downloading ||
            phase is UiPhase.Extracting ||
            phase is UiPhase.Transcribing
        ) {
            phase = UiPhase.Idle
        }
    }

    fun confirmPreFlight(pf: PreFlightState) {
        if (pf.needsGiantConfirm) {
            giantConfirm = pf
            return
        }
        preFlight = null
        startTranscription(pf.uri, pf.fileName)
    }

    val jobState by KlipNoteJobHub.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME ->
                    KlipNoteJobHub.appInForeground = true
                Lifecycle.Event.ON_STOP ->
                    KlipNoteJobHub.appInForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        KlipNoteJobHub.appInForeground =
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(jobState) {
        when (val s = jobState) {
            is KlipNoteJobHub.JobState.Running -> {
                val label = s.label
                phase = when {
                    label.contains("Descarg", ignoreCase = true) -> UiPhase.Downloading(label)
                    label.contains("Extra", ignoreCase = true) ||
                        label.contains("Preparando", ignoreCase = true) ||
                        label.contains("Preparing", ignoreCase = true) -> UiPhase.Extracting
                    else -> UiPhase.Transcribing(label)
                }
            }
            is KlipNoteJobHub.JobState.Success -> {
                transcriptText = s.transcript
                lastResultMeta = s.meta
                lastSavedMedia = s.savedMedia
                lastSourceLabel = s.sourceLabel.ifBlank { lastSourceLabel }
                // Respaldo: el servicio ya debió guardar; upsertById evita duplicados.
                s.meta?.let { persistGuion(lastSourceLabel, it, s.historyId) }
                transcriptEntries = TranscriptHistoryStore.load(context)
                historyEntries = BillingManager.loadHistory(context)
                appSpent = BillingManager.getAppSpent(context)
                localUsage = BillingManager.getLocalUsage(context)
                syncKeysUi()
                phase = if (s.meta != null) {
                    UiPhase.Done(s.meta)
                } else {
                    UiPhase.Idle
                }
                selectedTab = MainTab.Capturar
                if (KlipNoteJobHub.appInForeground) {
                    vibrateShort(context)
                    snackbar.showSnackbar(s.toastMessage)
                }
                KlipNoteJobHub.consumeTerminal()
            }
            is KlipNoteJobHub.JobState.Failed -> {
                CookieStore.siteForUrl(linkInput)?.let { cookieSiteHint = it }
                phase = UiPhase.Error(s.message)
                historyEntries = BillingManager.loadHistory(context)
                if (KlipNoteJobHub.appInForeground) vibrateShort(context)
                KlipNoteJobHub.consumeTerminal()
            }
            KlipNoteJobHub.JobState.Cancelled -> {
                phase = UiPhase.Idle
                snackbar.showSnackbar(context.getString(R.string.cancel))
                KlipNoteJobHub.consumeTerminal()
            }
            KlipNoteJobHub.JobState.Idle -> Unit
        }
    }

    val keysRevision by ApiKeyStore.revision.collectAsState()
    LaunchedEffect(keysRevision) {
        syncKeysUi()
        if (keysRevision == 0L) return@LaunchedEffect
        providerStatuses = OfferHealthMonitor.reconcileWithStore(context, providerStatuses)
    }

    LaunchedEffect(Unit) {
        syncKeysUi()
        refreshDashboard()
        if (ApiKeyStore.hasAnyKey(context)) {
            if (OfferHealthMonitor.shouldAutoCheck(context)) {
                verifyOffers(showSnackbar = false)
            }
        }
    }

    LaunchedEffect(shareNonce, shareUri, shareText) {
        if (shareText != null) {
            val text = shareText
            onShareConsumed()
            selectedTab = MainTab.Capturar
            val url = SealDownloader.extractFirstUrl(text)
            if (url != null) {
                linkInput = url
                if (autoTranscribeOnShare) {
                    var waits = 0
                    while (
                        SealDownloader.ready.value == SealDownloader.ReadyState.IDLE && waits < 25
                    ) {
                        kotlinx.coroutines.delay(200)
                        waits++
                    }
                    startFromLink(url, auto = true)
                }
            } else {
                linkInput = text
            }
            return@LaunchedEffect
        }
        val uri = shareUri ?: return@LaunchedEffect
        onShareConsumed()
        selectedTab = MainTab.Capturar
        if (!ApiKeyStore.hasAnyKey(context)) {
            showKeyDialog = true
            selectedTab = MainTab.Motores
            Toast.makeText(
                context,
                context.getString(R.string.error_no_key),
                Toast.LENGTH_LONG
            ).show()
            return@LaunchedEffect
        }
        val modeSnap = languageMode
        val diarizeSnap = diarize
        val includeSnap = includeDiarizeCost
        val balSnap = balanceSnapshot?.balanceUsd
        val autoSnap = autoTranscribeOnShare
        val pf = withContext(Dispatchers.IO) {
            buildPreFlight(uri, modeSnap, diarizeSnap, includeSnap, balSnap)
        }
        if (autoSnap && pf.hardStopMessage == null && !pf.needsGiantConfirm) {
            startTranscription(pf.uri, pf.fileName)
        } else {
            preFlight = pf
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val modeSnap = languageMode
            val diarizeSnap = diarize
            val includeSnap = includeDiarizeCost
            val balSnap = balanceSnapshot?.balanceUsd
            scope.launch {
                val pf = withContext(Dispatchers.IO) {
                    buildPreFlight(uri, modeSnap, diarizeSnap, includeSnap, balSnap)
                }
                preFlight = pf
            }
        }
    }

    val busy = phase is UiPhase.Downloading ||
        phase is UiPhase.Extracting ||
        phase is UiPhase.Transcribing ||
        KlipNoteJobHub.isBusy()

    val hasAnyKey = ApiKeyStore.hasAnyKey(context)
    val pillManual = routingSelection is RoutingSelection.Manual
    val pillText = activeMotorPillText(context, routingSelection, autoPrimary, hasKeys = hasAnyKey)

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            when (selectedTab) {
                MainTab.Capturar -> {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                color = OnBg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        actions = {
                            ActiveMotorPill(
                                label = pillText,
                                isManual = pillManual,
                                onClick = { showMotorSheet = true }
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Bg,
                            titleContentColor = OnBg
                        )
                    )
                }
                MainTab.Guiones -> {
                    TopAppBar(
                        title = {
                            Text(stringResource(R.string.tab_scripts), fontWeight = FontWeight.Bold, color = OnBg)
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Bg,
                            titleContentColor = OnBg
                        )
                    )
                }
                MainTab.Motores -> {
                    TopAppBar(
                        title = {
                            Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.Bold, color = OnBg)
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Bg,
                            titleContentColor = OnBg
                        )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Card, contentColor = OnBg) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Capturar,
                    onClick = { selectedTab = MainTab.Capturar },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_capture)) },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Guiones,
                    onClick = {
                        transcriptEntries = TranscriptHistoryStore.load(context)
                        selectedTab = MainTab.Guiones
                    },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_scripts)) },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Motores,
                    onClick = { selectedTab = MainTab.Motores },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    colors = navColors()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            when (selectedTab) {
                MainTab.Capturar -> {
                    CapturarTab(
                        linkInput = linkInput,
                        onLinkChange = { raw ->
                            linkInput = SealDownloader.extractFirstUrl(raw)
                                ?: SealDownloader.cleanMediaUrl(raw)
                        },
                        linkMode = linkMode,
                        onModeChange = { linkMode = it },
                        downloadKind = downloadKind,
                        onDownloadKindChange = { downloadKind = it },
                        sealReady = sealReady,
                        busy = busy,
                        phase = phase,
                        transcriptText = transcriptText,
                        sourceTitleOrUrl = lastSourceLabel.ifBlank { linkInput },
                        lastResultMeta = lastResultMeta,
                        copyFeedback = copyFeedback,
                        preFlight = preFlight,
                        forcedLanguage = forcedLanguage,
                        autoTranscribeOnShare = autoTranscribeOnShare,
                        onPasteLink = {
                            val cm = context.getSystemService(ClipboardManager::class.java)
                            val pasted = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)
                                ?.toString()?.trim().orEmpty()
                            if (pasted.isNotBlank()) {
                                linkInput = SealDownloader.extractFirstUrl(pasted)
                                    ?: SealDownloader.cleanMediaUrl(pasted)
                            }
                        },
                        onClearLink = { linkInput = "" },
                        onRun = { startFromLink(linkInput) },
                        onCancel = { cancelWork() },
                        onPickFile = {
                            if (!ApiKeyStore.hasAnyKey(context) &&
                                linkMode != SealDownloader.LinkMode.DOWNLOAD_ONLY
                            ) {
                                showKeyDialog = true
                                selectedTab = MainTab.Motores
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_no_key),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                picker.launch(arrayOf("audio/*", "video/*"))
                            }
                        },
                        onConnectCookies = { cookieSheetSite = cookieSiteHint },
                        onConfirmPreFlight = { confirmPreFlight(it) },
                        onDismissPreFlight = { preFlight = null },
                        onAutoTranscribeChange = { enabled ->
                            preFlight?.let { pf ->
                                if (enabled && pf.hardStopMessage != null) return@CapturarTab
                            }
                            autoTranscribeOnShare = enabled
                            BillingManager.setAutoTranscribeOnShare(context, enabled)
                        },
                        onCopy = {
                            copyToClipboard(context, transcriptText)
                            notifyCopied()
                        },
                        onClearResult = {
                            transcriptText = ""
                            lastResultMeta = null
                            lastSavedMedia = null
                            phase = UiPhase.Idle
                            copyFeedback = false
                        },
                        onBackToAuto = { setRouting(RoutingSelection.Auto) },
                        onTextChange = { transcriptText = it },
                        hasKeys = ApiKeyStore.hasAnyKey(context),
                        servicesSummary = buildServicesSummary(
                            context = context,
                            keyMasks = keyMasks,
                            groqRemainingSeconds = groqRemaining,
                            autoPrimary = autoPrimary
                        ),
                        onOpenOnboarding = onShowOnboarding,
                        onOpenKeys = {
                            keysFocusProvider = null
                            showKeyDialog = true
                        },
                        savedMedia = lastSavedMedia,
                        onOpenSavedMedia = { SavedMediaActions.openWithPlayer(context, it) },
                        onShareSavedMedia = { SavedMediaActions.share(context, it) },
                        onOpenDownloads = { SavedMediaActions.openDownloadsFolder(context) }
                    )
                }
                MainTab.Guiones -> {
                    GuionesTab(
                        entries = transcriptEntries,
                        search = guionSearch,
                        onSearchChange = { guionSearch = it },
                        expandedId = expandedGuionId,
                        onExpandToggle = { id ->
                            expandedGuionId = if (expandedGuionId == id) null else id
                        },
                        onCopy = { text ->
                            copyToClipboard(context, text)
                            notifyCopied()
                        },
                        onOpenLink = { url ->
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.capture_no_openable_link),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDelete = { id ->
                            transcriptEntries = TranscriptHistoryStore.delete(context, id)
                            historyEntries = BillingManager.loadHistory(context)
                            if (expandedGuionId == id) expandedGuionId = null
                        },
                        onClearAll = {
                            transcriptEntries = TranscriptHistoryStore.clear(context)
                            historyEntries = BillingManager.loadHistory(context)
                            expandedGuionId = null
                        },
                        onEntriesChanged = { transcriptEntries = it },
                        onGoToCapturar = { selectedTab = MainTab.Capturar }
                    )
                }
                MainTab.Motores -> {
                    MotoresTab(
                        routingSelection = routingSelection,
                        keyMasks = keyMasks,
                        onSelectAuto = { setRouting(RoutingSelection.Auto) },
                        onSelectManual = { setRouting(RoutingSelection.Manual(it)) },
                        onOpenKeys = {
                            keysFocusProvider = null
                            showKeyDialog = true
                        },
                        onConnectProvider = { provider ->
                            keysFocusProvider = provider
                            showKeyDialog = true
                        },
                        creditState = CreditUiState(
                            expanded = creditExpanded,
                            refreshing = creditRefreshing,
                            snapshot = balanceSnapshot,
                            appSpent = appSpent,
                            initialCredit = initialCredit,
                            includeDiarizeCost = includeDiarizeCost,
                            languageMode = languageMode,
                            localUsage = localUsage,
                            routeChain = routeChain,
                            providerStatuses = providerStatuses,
                            keyMasks = keyMasks,
                            allowPaidFallback = allowPaidFallback,
                            verifyingOffers = verifyingOffers,
                            groqRemainingSeconds = groqRemaining,
                            groqLimitSeconds = groqLimit,
                            groqUsedTodaySeconds = groqUsedToday,
                            groqHasLiveQuota = groqHasLive,
                            inworldRemainingSeconds = inworldRemaining,
                            inworldUsedMinutesEditable = String.format(
                                "%.1f",
                                BillingManager.getInworldUsedSeconds(context) / 60.0
                            ),
                            assemblyRemainingSeconds = assemblyRemaining,
                            assemblyRemainingUsdEditable = String.format(
                                java.util.Locale.US,
                                "%.2f",
                                BillingManager.getAssemblyRemainingUsdEstimate(context)
                            ),
                            engineEnabled = engineEnabled,
                            routingSelection = routingSelection
                        ),
                        onToggleExpand = { creditExpanded = !creditExpanded },
                        onRefresh = { refreshDashboard() },
                        onOpenHistory = {
                            historyEntries = BillingManager.loadHistory(context)
                            showHistory = true
                        },
                        onOpenCostSettings = { showCostSettings = true },
                        onVerifyOffers = { verifyOffers(showSnackbar = true) },
                        onAllowPaidChange = { enabled ->
                            allowPaidFallback = enabled
                            BillingManager.setAllowPaidFallback(context, enabled)
                            syncKeysUi()
                        },
                        onInworldUsedMinutesSave = { minutes ->
                            BillingManager.setInworldUsedSeconds(context, minutes * 60.0)
                            inworldRemaining = BillingManager.getInworldRemainingSeconds(context)
                        },
                        onAssemblyRemainingUsdSave = { usd ->
                            BillingManager.setAssemblyRemainingUsd(context, usd)
                            assemblyRemaining = BillingManager.getAssemblyRemainingSeconds(context)
                        },
                        onEngineEnabledChange = { provider, enabled ->
                            BillingManager.setEngineEnabled(context, provider, enabled)
                            if (provider == SttProvider.PAID_FALLBACK) {
                                allowPaidFallback = enabled
                            }
                            syncKeysUi()
                        },
                        onProbeProvider = { setRouting(RoutingSelection.Manual(it)) },
                        languageMode = languageMode,
                        onLanguageModeChange = {
                            languageMode = it
                            BillingManager.setLanguageMode(context, it)
                        },
                        forcedLanguage = forcedLanguage,
                        onForcedLanguageChange = {
                            forcedLanguage = it
                            BillingManager.setForcedLanguage(context, it)
                            if (languageMode != LanguageMode.FIXED) {
                                languageMode = LanguageMode.FIXED
                                BillingManager.setLanguageMode(context, LanguageMode.FIXED)
                            }
                        },
                        diarize = diarize,
                        onDiarizeChange = {
                            diarize = it
                            BillingManager.setDiarizeEnabled(context, it)
                        },
                        sealReady = sealReady,
                        updatingYtDlp = updatingYtDlp,
                        onUpdateYtDlp = {
                            if (updatingYtDlp) return@MotoresTab
                            updatingYtDlp = true
                            scope.launch {
                                val msg = runCatching {
                                    SealDownloader.updateYtDlpNightly(context)
                                }.getOrElse { e ->
                                    e.message ?: context.getString(R.string.toast_update_failed)
                                }
                                updatingYtDlp = false
                                snackbar.showSnackbar(context.getString(R.string.toast_ytdlp_status, msg))
                            }
                        },
                        cookiesEnabled = CookieStore.isEnabled(context),
                        cookiesRevision = cookiesRevision,
                        onConnectCookies = { site -> cookieSheetSite = site },
                        onToggleCookies = { enabled ->
                            CookieStore.setEnabled(context, enabled)
                            cookiesRevision += 1
                        },
                        onClearCookies = { site ->
                            CookieStore.clear(context, site)
                            cookiesRevision += 1
                            scope.launch {
                                snackbar.showSnackbar(
                                    context.getString(R.string.toast_account_disconnected, site.label)
                                )
                            }
                        },
                        onShowTutorial = onShowOnboarding,
                        themeId = themeId,
                        onThemeIdChange = onThemeIdChange,
                        notifyWhenDone = notifyWhenDone,
                        onNotifyWhenDoneChange = { enabled ->
                            notifyWhenDone = enabled
                            KlipNoteNotify.setNotifyEnabled(context, enabled)
                        }
                    )
                }
            }
        }
    }


    cookieSheetSite?.let { sheetSite ->
        SiteCookiesSheet(
            site = sheetSite,
            onDismiss = { cookieSheetSite = null },
            onSaved = { count ->
                cookiesRevision += 1
                cookieSheetSite = null
                scope.launch {
                    snackbar.showSnackbar(context.getString(R.string.toast_cookies_saved, count))
                }
            }
        )
    }

    if (showMotorSheet) {
        MotorPickerSheet(
            selection = routingSelection,
            keyMasks = keyMasks,
            onDismiss = { showMotorSheet = false },
            onSelectAuto = {
                setRouting(RoutingSelection.Auto)
                showMotorSheet = false
            },
            onSelectManual = { provider ->
                if (keyMasks[provider].isNullOrBlank()) {
                    showMotorSheet = false
                    showKeyDialog = true
                    selectedTab = MainTab.Motores
                } else {
                    setRouting(RoutingSelection.Manual(provider))
                    showMotorSheet = false
                }
            }
        )
    }

    if (showKeyDialog) {
        MultiProviderKeysDialog(
            onDismiss = {
                showKeyDialog = false
                keysFocusProvider = null
            },
            onChanged = {
                if (ApiKeyStore.getDeepgram(context).isBlank()) {
                    balanceSnapshot = null
                }
                onKeysChanged()
            },
            focusProvider = keysFocusProvider
        )
    }

    if (showCostSettings) {
        CostSettingsDialog(
            initialCredit = initialCredit,
            appCap = appCap,
            minReserve = minReserve,
            includeDiarize = includeDiarizeCost,
            onDismiss = { showCostSettings = false },
            onSave = { credit, cap, reserve, include ->
                BillingManager.setInitialCredit(context, credit)
                BillingManager.setAppCap(context, cap)
                BillingManager.setMinReserve(context, reserve)
                BillingManager.setIncludeDiarizeCost(context, include)
                initialCredit = credit
                appCap = cap
                minReserve = reserve
                includeDiarizeCost = include
                showCostSettings = false
            }
        )
    }

    if (showHistory) {
        CostHistorySheet(
            entries = historyEntries,
            onDismiss = { showHistory = false },
            onOpen = { entry ->
                transcriptText = entry.transcript
                lastSavedMedia = SavedMediaActions.fromHistory(
                    entry.downloadUri,
                    entry.downloadDisplayName,
                    entry.downloadIsVideo
                )
                phase = UiPhase.Idle
                selectedTab = MainTab.Capturar
                showHistory = false
            },
            onDelete = { entry ->
                BillingManager.deleteHistory(context, entry.id)
                historyEntries = BillingManager.loadHistory(context)
            },
            onCopyTranscript = { text ->
                copyToClipboard(context, text)
                notifyCopied()
            },
            onOpenSavedMedia = { SavedMediaActions.openWithPlayer(context, it) },
            onShareSavedMedia = { SavedMediaActions.share(context, it) },
            onOpenDownloads = { SavedMediaActions.openDownloadsFolder(context) }
        )
    }

    if (showNotifPermissionRationale) {
        AlertDialog(
            onDismissRequest = {
                showNotifPermissionRationale = false
                KlipNoteNotify.markAskedNotificationPermission(context)
                val pending = pendingLinkAfterNotif
                pendingLinkAfterNotif = null
                if (pending != null) launchLinkJob(pending)
            },
            containerColor = Card,
            titleContentColor = OnBg,
            textContentColor = OnBg,
            title = { Text(stringResource(R.string.finish_notifications_title)) },
            text = {
                Text(stringResource(R.string.notif_permission_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotifPermissionRationale = false
                        if (Build.VERSION.SDK_INT >= 33) {
                            notifPermissionLauncher.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            val pending = pendingLinkAfterNotif
                            pendingLinkAfterNotif = null
                            if (pending != null) launchLinkJob(pending)
                        }
                    }
                ) { Text(stringResource(R.string.allow), color = Accent) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotifPermissionRationale = false
                        KlipNoteNotify.markAskedNotificationPermission(context)
                        val pending = pendingLinkAfterNotif
                        pendingLinkAfterNotif = null
                        if (pending != null) launchLinkJob(pending)
                    }
                ) { Text(stringResource(R.string.not_now), color = Muted) }
            }
        )
    }

    giantConfirm?.let { pf ->
        AlertDialog(
            onDismissRequest = { giantConfirm = null },
            containerColor = Card,
            titleContentColor = OnBg,
            textContentColor = OnBg,
            title = { Text(stringResource(R.string.high_cost_file)) },
            text = {
                Text(
                    stringResource(
                        R.string.confirm_expensive_body,
                        BillingManager.formatUsd(pf.estimatedUsdDisplay, 4),
                        BillingManager.formatUsd(BillingManager.GIANT_FILE_THRESHOLD_USD, 2)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        giantConfirm = null
                        preFlight = null
                        startTranscription(pf.uri, pf.fileName)
                    }
                ) {
                    Text(stringResource(R.string.yes_transcribe), color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { giantConfirm = null }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            }
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Accent,
    selectedTextColor = Accent,
    indicatorColor = Accent.copy(alpha = 0.18f),
    unselectedIconColor = Muted,
    unselectedTextColor = Muted
)

@Composable
private fun ActiveMotorPill(
    label: String,
    isManual: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isManual) Amber else Accent
    Text(
        text = label,
        color = tint,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(end = 8.dp)
            .widthIn(max = 168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotorPickerSheet(
    selection: RoutingSelection,
    keyMasks: Map<SttProvider, String?>,
    onDismiss: () -> Unit,
    onSelectAuto: () -> Unit,
    onSelectManual: (SttProvider) -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.motor_picker_title),
                color = OnBg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                stringResource(R.string.capture_engine_sheet_hint),
                color = Muted,
                fontSize = 13.sp
            )
            FilterChip(
                selected = selection is RoutingSelection.Auto,
                onClick = onSelectAuto,
                label = { Text(stringResource(R.string.automatic)) },
                modifier = Modifier.fillMaxWidth(),
                colors = chipColors()
            )
            SttProvider.entries.forEach { provider ->
                val selected = selection is RoutingSelection.Manual &&
                    selection.provider == provider
                FilterChip(
                    selected = selected,
                    onClick = { onSelectManual(provider) },
                    label = {
                        Text(
                            buildString {
                                append(provider.chipLabel(context))
                                if (keyMasks[provider].isNullOrBlank()) {
                                    append(" · ")
                                    append(context.getString(R.string.block_no_key))
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = chipColors()
                )
            }
        }
    }
}

@Composable
private fun CapturarTab(
    linkInput: String,
    onLinkChange: (String) -> Unit,
    linkMode: SealDownloader.LinkMode,
    onModeChange: (SealDownloader.LinkMode) -> Unit,
    downloadKind: SealDownloader.DownloadKind,
    onDownloadKindChange: (SealDownloader.DownloadKind) -> Unit,
    sealReady: SealDownloader.ReadyState,
    busy: Boolean,
    phase: UiPhase,
    transcriptText: String,
    sourceTitleOrUrl: String,
    lastResultMeta: TranscriptionResult?,
    copyFeedback: Boolean,
    preFlight: PreFlightState?,
    forcedLanguage: String,
    autoTranscribeOnShare: Boolean,
    onPasteLink: () -> Unit,
    onClearLink: () -> Unit,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onPickFile: () -> Unit,
    onConnectCookies: () -> Unit,
    onConfirmPreFlight: (PreFlightState) -> Unit,
    onDismissPreFlight: () -> Unit,
    onAutoTranscribeChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onClearResult: () -> Unit,
    onBackToAuto: () -> Unit,
    onTextChange: (String) -> Unit,
    hasKeys: Boolean = false,
    servicesSummary: String = "",
    onOpenOnboarding: () -> Unit = {},
    onOpenKeys: () -> Unit = {},
    savedMedia: SavedMediaInfo?,
    onOpenSavedMedia: (SavedMediaInfo) -> Unit,
    onShareSavedMedia: (SavedMediaInfo) -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LinkHeroCard(
            linkInput = linkInput,
            onLinkChange = onLinkChange,
            linkMode = linkMode,
            onModeChange = onModeChange,
            downloadKind = downloadKind,
            onDownloadKindChange = onDownloadKindChange,
            sealReady = sealReady,
            busy = busy,
            onPasteLink = onPasteLink,
            onClearLink = onClearLink,
            onRun = onRun,
            onPickFile = onPickFile
        )

        when (phase) {
            is UiPhase.Downloading,
            UiPhase.Extracting,
            is UiPhase.Transcribing -> {
                ProgressCard(phase = phase, onCancel = onCancel)
            }
            is UiPhase.Error -> {
                ErrorCard(
                    message = phase.message,
                    isManual = phase.isManualTest,
                    showConnectCookies = SealDownloader.looksLikeLoginRequired(phase.message),
                    onConnectCookies = onConnectCookies,
                    onBackToAuto = onBackToAuto
                )
            }
            else -> Unit
        }

        preFlight?.let { pf ->
            PreFlightCard(
                fileName = pf.fileName,
                durationLabel = BillingManager.formatDuration(pf.durationSeconds),
                modeLabel = pf.estimate.mode.shortUiLabel(context, forcedLanguage) +
                    if (pf.estimate.includeDiarize) " + separar voces" else "",
                estimatedUsd = pf.estimatedUsdDisplay,
                hardStopMessage = pf.hardStopMessage,
                autoTranscribe = autoTranscribeOnShare,
                autoTranscribeEnabled = pf.hardStopMessage == null,
                routeHint = pf.routeHint,
                onAutoTranscribeChange = onAutoTranscribeChange,
                onConfirm = { onConfirmPreFlight(pf) },
                onDismiss = onDismissPreFlight,
                busy = busy
            )
        }

        if (savedMedia != null) {
            SavedMediaBanner(
                media = savedMedia,
                onOpen = { onOpenSavedMedia(savedMedia) },
                onOpenDownloads = onOpenDownloads,
                onShareFile = { onShareSavedMedia(savedMedia) }
            )
        }

        if (transcriptText.isNotBlank() || phase is UiPhase.Done) {
            ResultWorkspaceCard(
                text = transcriptText,
                sourceTitleOrUrl = sourceTitleOrUrl,
                metaLabel = lastResultMeta?.resultBadge(copiedHint = copyFeedback),
                onTextChange = onTextChange,
                onCopy = onCopy,
                onClear = onClearResult
            )
        } else if (phase is UiPhase.Idle && preFlight == null) {
            val openAction = {
                if (!hasKeys) onOpenOnboarding() else onOpenKeys()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .background(Card)
                    .clickable(onClick = openAction)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasKeys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.capture_step1_title),
                                color = OnBg,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                stringResource(R.string.capture_step1_body),
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Button(
                        onClick = openAction,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = OnAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.start), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        servicesSummary.ifBlank { stringResource(R.string.services_ready_fallback) },
                        color = OnBg,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(stringResource(R.string.capture_ready_paste), color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SavedMediaBanner(
    media: SavedMediaInfo,
    onOpen: () -> Unit,
    onOpenDownloads: () -> Unit,
    onShareFile: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.notify_download_saved, media.displayName),
            color = Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(media.openLabel(context), color = Accent, fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onOpenDownloads,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.view_in_downloads), color = Accent, fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onShareFile,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.action_share),
                    color = Accent,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LinkHeroCard(
    linkInput: String,
    onLinkChange: (String) -> Unit,
    linkMode: SealDownloader.LinkMode,
    onModeChange: (SealDownloader.LinkMode) -> Unit,
    downloadKind: SealDownloader.DownloadKind,
    onDownloadKindChange: (SealDownloader.DownloadKind) -> Unit,
    sealReady: SealDownloader.ReadyState,
    busy: Boolean,
    onPasteLink: () -> Unit,
    onClearLink: () -> Unit,
    onRun: () -> Unit,
    onPickFile: () -> Unit
) {
    val context = LocalContext.current
    val primaryLabel = when (linkMode) {
        SealDownloader.LinkMode.TEXT_ONLY -> stringResource(R.string.extract_transcript)
        SealDownloader.LinkMode.AUDIO_AND_TEXT -> stringResource(R.string.capture_mode_audio_script)
        SealDownloader.LinkMode.VIDEO_AND_TEXT -> stringResource(R.string.capture_mode_video_script)
        SealDownloader.LinkMode.DOWNLOAD_ONLY -> stringResource(R.string.capture_download_file)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = linkInput,
            onValueChange = { onLinkChange(it.replace("\n", "")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            maxLines = 1,
            placeholder = {
                Text(
                    stringResource(R.string.capture_paste_link_hint),
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingIcon = {
                if (linkInput.isNotBlank()) {
                    IconButton(onClick = onClearLink, enabled = !busy) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear), tint = Muted)
                    }
                } else {
                    IconButton(onClick = onPasteLink, enabled = !busy) {
                        Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.cd_paste), tint = Accent)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = KlipNeutrals.FieldBorder,
                focusedTextColor = OnBg,
                unfocusedTextColor = OnBg,
                cursorColor = Accent
            )
        )

        // 2x2 — todos los modos visibles (sin LazyRow que recorta Solo Bajar)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip(
                    selected = linkMode == SealDownloader.LinkMode.TEXT_ONLY,
                    label = stringResource(R.string.capture_chip_text_only),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(SealDownloader.LinkMode.TEXT_ONLY) }
                )
                ModeChip(
                    selected = linkMode == SealDownloader.LinkMode.AUDIO_AND_TEXT,
                    label = stringResource(R.string.capture_chip_audio_text),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(SealDownloader.LinkMode.AUDIO_AND_TEXT) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip(
                    selected = linkMode == SealDownloader.LinkMode.VIDEO_AND_TEXT,
                    label = stringResource(R.string.capture_chip_video_text),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(SealDownloader.LinkMode.VIDEO_AND_TEXT) }
                )
                ModeChip(
                    selected = linkMode == SealDownloader.LinkMode.DOWNLOAD_ONLY,
                    label = stringResource(R.string.capture_chip_download_only),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(SealDownloader.LinkMode.DOWNLOAD_ONLY) }
                )
            }
        }

        Text(
            when (linkMode) {
                SealDownloader.LinkMode.TEXT_ONLY ->
                    stringResource(R.string.capture_hint_text_only)
                SealDownloader.LinkMode.AUDIO_AND_TEXT ->
                    stringResource(R.string.capture_hint_audio_text)
                SealDownloader.LinkMode.VIDEO_AND_TEXT ->
                    stringResource(R.string.capture_hint_video_text)
                SealDownloader.LinkMode.DOWNLOAD_ONLY ->
                    stringResource(R.string.capture_hint_download_only)
            },
            color = Muted,
            fontSize = 12.sp
        )

        if (linkMode == SealDownloader.LinkMode.DOWNLOAD_ONLY) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip(
                    selected = downloadKind == SealDownloader.DownloadKind.AUDIO_M4A,
                    label = stringResource(R.string.capture_audio_m4a),
                    enabled = !busy,
                    onClick = { onDownloadKindChange(SealDownloader.DownloadKind.AUDIO_M4A) }
                )
                ModeChip(
                    selected = downloadKind == SealDownloader.DownloadKind.VIDEO_MP4,
                    label = stringResource(R.string.capture_video_mp4),
                    enabled = !busy,
                    onClick = { onDownloadKindChange(SealDownloader.DownloadKind.VIDEO_MP4) }
                )
            }
        }

        val readyHint = when (sealReady) {
            SealDownloader.ReadyState.READY -> null
            SealDownloader.ReadyState.IDLE -> stringResource(R.string.preparing_download_engine)
            SealDownloader.ReadyState.ERROR -> stringResource(R.string.download_engine_not_ready)
        }
        if (readyHint != null) {
            Text(readyHint, color = Muted, fontSize = 12.sp)
        }

        Button(
            onClick = onRun,
            enabled = !busy && linkInput.isNotBlank() &&
                sealReady == SealDownloader.ReadyState.READY,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = OnAccent,
                disabledContainerColor = Accent.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(primaryLabel, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onPickFile,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Accent)
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.capture_pick_file),
                color = Accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProgressCard(phase: UiPhase, onCancel: () -> Unit) {
    val (label, progress) = when (phase) {
        is UiPhase.Downloading -> {
            val pct = Regex("""(\d+)%""").find(phase.label)?.groupValues?.getOrNull(1)
                ?.toFloatOrNull()?.div(100f)
            phase.label to pct
        }
        UiPhase.Extracting -> stringResource(R.string.status_extracting_audio) to null
        is UiPhase.Transcribing ->
            (phase.motorHint.ifBlank { stringResource(R.string.transcribing) }) to null
        else -> return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Accent,
                strokeWidth = 2.5.dp
            )
            Text(label, color = OnBg, modifier = Modifier.weight(1f))
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Accent,
                trackColor = KlipNeutrals.ChipBg
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Accent,
                trackColor = KlipNeutrals.ChipBg
            )
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.cancel), color = KlipNeutrals.Red)
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    isManual: Boolean,
    showConnectCookies: Boolean = false,
    onConnectCookies: () -> Unit = {},
    onBackToAuto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(message, color = KlipNeutrals.Red)
        if (showConnectCookies) {
            Text(
                stringResource(R.string.cookie_connect_privacy),
                color = Muted,
                fontSize = 12.sp
            )
            Button(
                onClick = onConnectCookies,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = OnAccent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.connect_my_account), fontWeight = FontWeight.SemiBold)
            }
        }
        if (isManual) {
            TextButton(onClick = onBackToAuto) {
                Text(stringResource(R.string.back_to_auto), color = Accent)
            }
        }
    }
}

@Composable
private fun TranscriptActionBar(
    text: String,
    sourceTitleOrUrl: String,
    modifier: Modifier = Modifier,
    onCopied: (() -> Unit)? = null
) {
    if (text.isBlank()) return
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TranscriptActionButton(
                label = stringResource(R.string.action_copy),
                icon = Icons.Default.ContentCopy,
                onClick = {
                    if (onCopied != null) {
                        onCopied.invoke()
                    } else {
                        copyToClipboard(context, text)
                        Toast.makeText(
                            context,
                            context.getString(R.string.copied_to_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            TranscriptActionButton(
                label = stringResource(R.string.save_txt),
                icon = Icons.Default.SaveAlt,
                onClick = {
                    val uri = saveTranscriptTxt(context, sourceTitleOrUrl, text)
                    Toast.makeText(
                        context,
                        context.getString(
                            if (uri != null) R.string.save_txt_ok else R.string.save_txt_failed
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.weight(1f)
            )
            TranscriptActionButton(
                label = stringResource(R.string.share_txt),
                icon = Icons.Default.Share,
                onClick = {
                    val uri = saveOrShareTranscriptTxt(
                        context = context,
                        title = sourceTitleOrUrl,
                        transcript = text,
                        share = true
                    )
                    if (uri == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.save_txt_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedButton(
            onClick = { sendTranscriptWithAiPack(context, sourceTitleOrUrl, text) },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Accent.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Accent.copy(alpha = 0.12f),
                contentColor = Accent
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = stringResource(R.string.send_with_ai),
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TranscriptActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, KlipNeutrals.FieldBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = KlipNeutrals.ChipBg,
            contentColor = Accent
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = label,
            color = Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResultWorkspaceCard(
    text: String,
    sourceTitleOrUrl: String,
    metaLabel: String?,
    onTextChange: (String) -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metaLabel?.let {
            Text(
                it,
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (text.isNotBlank()) {
            TranscriptActionBar(
                text = text,
                sourceTitleOrUrl = sourceTitleOrUrl,
                onCopied = onCopy
            )
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    stringResource(R.string.action_clear),
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AiStudyActionsSection(
                transcript = text,
                sourceTitleOrUrl = sourceTitleOrUrl
            )
        }
        SelectionContainer {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = OnBg,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 22.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = KlipNeutrals.FieldBorder,
                    focusedContainerColor = KlipNeutrals.ChipBg,
                    unfocusedContainerColor = KlipNeutrals.ChipBg,
                    cursorColor = Accent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AiStudyActionsSection(
    transcript: String,
    sourceTitleOrUrl: String,
    modifier: Modifier = Modifier
) {
    if (transcript.isBlank()) return

    val context = LocalContext.current
    var lastCopiedId by remember { mutableStateOf<String?>(null) }
    var showCreativeActions by remember { mutableStateOf(false) }

    LaunchedEffect(lastCopiedId) {
        val id = lastCopiedId ?: return@LaunchedEffect
        delay(2_500)
        if (lastCopiedId == id) lastCopiedId = null
    }

    fun copyMode(mode: AiStudyMode) {
        val prompt = AiStudyPrompts.build(
            context = context,
            mode = mode,
            sourceTitleOrUrl = sourceTitleOrUrl
        )
        copyToClipboard(context, prompt)
        lastCopiedId = mode.name
        Toast.makeText(
            context,
            context.getString(R.string.ai_prompt_copied_attach_txt),
            Toast.LENGTH_LONG
        ).show()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.send_to_ai))
            )
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.ai_share_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KlipNeutrals.ChipBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.study_with_ai),
            color = OnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )

        Text(
            stringResource(R.string.ai_button_hint),
            color = Muted,
            fontSize = 12.sp
        )

        // Six text-first actions stay visible; creative actions are progressively disclosed.
        Text(
            stringResource(R.string.ai_group_analyze),
            color = OnBg,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AiStudyMode.entries.filter { !it.isCreative() }.forEach { mode ->
                val justCopied = lastCopiedId == mode.name
                AssistChip(
                    onClick = { copyMode(mode) },
                    label = {
                        Text(
                            if (justCopied) stringResource(R.string.copied_to_clipboard)
                            else mode.chipLabel(context),
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (justCopied) Accent.copy(alpha = 0.28f)
                        else KlipNeutrals.Card,
                        labelColor = if (justCopied) Accent else OnBg
                    )
                )
            }
        }

        val creativeModes = AiStudyMode.entries.filter { it.isCreative() }
        OutlinedButton(
            onClick = { showCreativeActions = !showCreativeActions },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBg)
        ) {
            Text(
                "${stringResource(R.string.ai_group_create)} (${creativeModes.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            Icon(
                imageVector = if (showCreativeActions) Icons.Default.ExpandLess
                else Icons.Default.ExpandMore,
                contentDescription = if (showCreativeActions)
                    stringResource(R.string.ai_hide_creative)
                else stringResource(R.string.ai_show_creative),
                tint = OnBg
            )
        }

        if (showCreativeActions) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                creativeModes.forEach { mode ->
                    val justCopied = lastCopiedId == mode.name
                    AssistChip(
                        onClick = { copyMode(mode) },
                        label = {
                            Text(
                                if (justCopied) stringResource(R.string.copied_to_clipboard)
                                else mode.chipLabel(context),
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (justCopied) Accent.copy(alpha = 0.28f)
                            else KlipNeutrals.Card,
                            labelColor = if (justCopied) Accent else OnBg
                        )
                    )
                }
            }
        }

        lastCopiedId?.let { id ->
            val mode = runCatching { AiStudyMode.valueOf(id) }.getOrNull()
            val label = mode?.modeName(context) ?: stringResource(R.string.prompt_label)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.copied_to_clipboard_named, label),
                    color = Accent,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                if (mode != null) {
                    TextButton(onClick = { copyMode(mode) }) {
                        Text(stringResource(R.string.send_to_ai), color = Accent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuionesTab(
    entries: List<TranscriptHistoryStore.Entry>,
    search: String,
    onSearchChange: (String) -> Unit,
    expandedId: String?,
    onExpandToggle: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onEntriesChanged: (List<TranscriptHistoryStore.Entry>) -> Unit = {},
    onGoToCapturar: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filtered = remember(entries, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) entries
        else entries.filter {
            it.text.lowercase().contains(q) ||
                it.sourceTitleOrUrl.lowercase().contains(q) ||
                it.providerUsed.lowercase().contains(q)
        }
    }
    val fmt = remember {
        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = TranscriptBackup.exportJson(entries)
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: throw java.io.IOException("No se pudo abrir destino")
                    out.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                        it.flush()
                    }
                }.isSuccess
            }
            Toast.makeText(
                context,
                context.getString(
                    if (ok) R.string.scripts_export_ok else R.string.scripts_export_failed
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        TranscriptBackup.readLimited(input)
                    } ?: return@runCatching -1
                    TranscriptBackup.importInto(context, bytes)
                }.getOrDefault(-1)
            }
            if (added < 0) {
                Toast.makeText(
                    context,
                    context.getString(R.string.scripts_import_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                onEntriesChanged(TranscriptHistoryStore.load(context))
                Toast.makeText(
                    context,
                    if (added == 0) {
                        context.getString(R.string.scripts_import_none)
                    } else {
                        context.getString(R.string.scripts_import_ok, added)
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Card,
            titleContentColor = OnBg,
            textContentColor = OnBg,
            title = { Text(stringResource(R.string.clear_all_history_title)) },
            text = { Text(stringResource(R.string.scripts_clear_confirm_body), color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text(stringResource(R.string.delete), color = KlipNeutrals.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            }
        )
    }

    if (entries.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_more),
                            tint = Muted
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.scripts_export), color = OnBg) },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch(TranscriptBackup.FILE_NAME)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.scripts_import), color = OnBg) },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Accent.copy(alpha = 0.85f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.scripts_empty),
                    color = OnBg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onGoToCapturar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = OnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.go_to_capture), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.scripts_search_hint), color = Muted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = KlipNeutrals.FieldBorder,
                focusedTextColor = OnBg,
                unfocusedTextColor = OnBg,
                cursorColor = Accent
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.scripts_count, filtered.size),
                color = Muted,
                fontSize = 12.sp
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more),
                        tint = Muted
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scripts_export), color = OnBg) },
                        onClick = {
                            menuOpen = false
                            exportLauncher.launch(TranscriptBackup.FILE_NAME)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scripts_import), color = OnBg) },
                        onClick = {
                            menuOpen = false
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clear_history), color = KlipNeutrals.Red) },
                        onClick = {
                            menuOpen = false
                            confirmClear = true
                        }
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.scripts_search_empty), color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { entry ->
                    val expanded = expandedId == entry.id
                    val origin = sourceKindLabel(context, entry.sourceTitleOrUrl)
                    val dur = if (entry.durationSec > 0) {
                        BillingManager.formatDuration(entry.durationSec)
                    } else "—"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Card)
                            .clickable { onExpandToggle(entry.id) }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            entry.sourceTitleOrUrl,
                            color = OnBg,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$origin · $dur · ${fmt.format(Date(entry.timestampMs))} · ${entry.providerUsed}",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        Text(
                            entry.text,
                            color = OnBg.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        TranscriptActionBar(
                            text = entry.text,
                            sourceTitleOrUrl = entry.sourceTitleOrUrl,
                            onCopied = { onCopy(entry.text) }
                        )
                        if (expanded) {
                            val openableUrl = entry.sourceUrl?.let {
                                SealDownloader.extractFirstUrl(it)
                            } ?: SealDownloader.extractFirstUrl(entry.sourceTitleOrUrl)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (openableUrl != null) {
                                    TextButton(onClick = { onOpenLink(openableUrl) }) {
                                        Text(
                                            stringResource(R.string.open_link),
                                            color = Accent,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                TextButton(onClick = { onDelete(entry.id) }) {
                                    Text(
                                        stringResource(R.string.delete),
                                        color = KlipNeutrals.Red,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            if (entry.text.isNotBlank()) {
                                AiStudyActionsSection(
                                    transcript = entry.text,
                                    sourceTitleOrUrl = entry.sourceTitleOrUrl
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
private fun MotoresTab(
    routingSelection: RoutingSelection,
    keyMasks: Map<SttProvider, String?>,
    onSelectAuto: () -> Unit,
    onSelectManual: (SttProvider) -> Unit,
    onOpenKeys: () -> Unit,
    onConnectProvider: (SttProvider) -> Unit = { onOpenKeys() },
    creditState: CreditUiState,
    onToggleExpand: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCostSettings: () -> Unit,
    onVerifyOffers: () -> Unit,
    onAllowPaidChange: (Boolean) -> Unit,
    onInworldUsedMinutesSave: (Double) -> Unit,
    onAssemblyRemainingUsdSave: (Double) -> Unit,
    onEngineEnabledChange: (SttProvider, Boolean) -> Unit,
    onProbeProvider: (SttProvider) -> Unit,
    languageMode: LanguageMode,
    onLanguageModeChange: (LanguageMode) -> Unit,
    forcedLanguage: String,
    onForcedLanguageChange: (String) -> Unit,
    diarize: Boolean,
    onDiarizeChange: (Boolean) -> Unit,
    sealReady: SealDownloader.ReadyState,
    updatingYtDlp: Boolean,
    onUpdateYtDlp: () -> Unit,
    cookiesEnabled: Boolean,
    cookiesRevision: Int,
    onConnectCookies: (CookieSite) -> Unit,
    onToggleCookies: (Boolean) -> Unit,
    onClearCookies: (CookieSite) -> Unit,
    onShowTutorial: () -> Unit = {},
    themeId: KlipThemeId = KlipThemeId.GREEN,
    onThemeIdChange: (KlipThemeId) -> Unit = {},
    notifyWhenDone: Boolean = true,
    onNotifyWhenDoneChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var advancedOpen by remember { mutableStateOf(false) }
    var cookiesOpen by remember { mutableStateOf(false) }
    val connected = remember(cookiesRevision) { CookieStore.connectedSites(context).toSet() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FreeServicesPanel(
            state = creditState,
            onRefresh = onRefresh,
            onVerifyOffers = onVerifyOffers,
            onInworldUsedMinutesSave = onInworldUsedMinutesSave,
            onAssemblyRemainingUsdSave = onAssemblyRemainingUsdSave,
            onOpenKeys = onOpenKeys,
            onConnectProvider = onConnectProvider,
            onProbeProvider = onProbeProvider,
            onEngineEnabledChange = onEngineEnabledChange
        )

        AppLanguageSection()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(stringResource(R.string.lang_detection_label), helpRes = HelpCopy.LANGUAGE)
            ModeChip(
                selected = languageMode == LanguageMode.AUTO,
                label = stringResource(R.string.mode_auto),
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLanguageModeChange(LanguageMode.AUTO) }
            )
            ModeChip(
                selected = languageMode == LanguageMode.MULTI,
                label = stringResource(R.string.mode_multi),
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLanguageModeChange(LanguageMode.MULTI) }
            )
            Text(stringResource(R.string.always_on_label), color = Muted, fontSize = 12.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COMMON_STT_LANGUAGES.filter { it.code != "multi" }.forEach { opt ->
                    ModeChip(
                        selected = languageMode == LanguageMode.FIXED &&
                            forcedLanguage.equals(opt.code, ignoreCase = true),
                        label = opt.label,
                        enabled = true,
                        onClick = { onForcedLanguageChange(opt.code) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { cookiesOpen = !cookiesOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader(
                        stringResource(R.string.videos_need_sign_in),
                        helpRes = HelpCopy.COOKIES
                    )
                    Text(
                        stringResource(HelpCopy.COOKIES),
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = if (cookiesOpen) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (cookiesOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (cookiesOpen) R.string.cd_collapse else R.string.cd_expand
                    ),
                    tint = Muted
                )
            }
            if (cookiesOpen) {
                CookieSite.entries.forEach { site ->
                    val isConnected = site in connected
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${site.label} · ${
                                if (isConnected) {
                                    stringResource(R.string.connected_status)
                                } else {
                                    stringResource(R.string.not_connected_status)
                                }
                            }",
                            color = if (isConnected) Accent else OnBg,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isConnected) {
                            TextButton(onClick = { onClearCookies(site) }) {
                                Text(stringResource(R.string.disconnect), color = KlipNeutrals.Red, fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = { onConnectCookies(site) }) {
                                Text(stringResource(R.string.connect), color = Accent, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (connected.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.use_accounts_downloads), color = OnBg, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = cookiesEnabled,
                            onCheckedChange = onToggleCookies,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Accent
                            )
                        )
                    }
                }
            }
        }

        AppearanceSection(
            themeId = themeId,
            onThemeIdChange = onThemeIdChange
        )

        CheckForUpdatesSection()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { advancedOpen = !advancedOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.advanced_section), color = OnBg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Icon(
                    if (advancedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (advancedOpen) R.string.cd_collapse else R.string.cd_expand
                    ),
                    tint = Muted
                )
            }
            if (advancedOpen) {
                SectionHeader(stringResource(R.string.automatic), helpRes = HelpCopy.AUTO)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.notify_when_done), color = OnBg, fontSize = 13.sp)
                        Text(
                            stringResource(R.string.notify_when_done_hint),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = notifyWhenDone,
                        onCheckedChange = onNotifyWhenDoneChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.zero_mode_label),
                        color = OnBg,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = !creditState.allowPaidFallback,
                        onCheckedChange = { strict -> onAllowPaidChange(!strict) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent
                        )
                    )
                }
                if (creditState.allowPaidFallback) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.use_paid_service), color = OnBg, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = creditState.engineEnabled[SttProvider.PAID_FALLBACK] ?: true,
                            onCheckedChange = { onEngineEnabledChange(SttProvider.PAID_FALLBACK, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Amber
                            )
                        )
                    }
                }

                Text(stringResource(R.string.test_service_manual), color = OnBg, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                RoutingModeChips(
                    selection = routingSelection,
                    keyMasks = keyMasks,
                    onSelectAuto = onSelectAuto,
                    onSelectManual = onSelectManual,
                    onOpenKeys = onOpenKeys
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.diarize), color = OnBg, fontSize = 13.sp)
                        Text(stringResource(HelpCopy.DIARIZE), color = Muted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = diarize,
                        onCheckedChange = onDiarizeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent
                        )
                    )
                }

                TextButton(onClick = onOpenCostSettings) {
                    Text(stringResource(R.string.deepgram_cost_settings), color = Accent)
                }

                SectionHeader(stringResource(R.string.download_engine), helpRes = HelpCopy.YTDLP)
                Text(stringResource(R.string.updates_itself), color = Muted, fontSize = 12.sp)
                Text(
                    when (sealReady) {
                        SealDownloader.ReadyState.READY -> stringResource(R.string.ready)
                        SealDownloader.ReadyState.IDLE -> stringResource(R.string.initializing)
                        SealDownloader.ReadyState.ERROR -> stringResource(R.string.ytdlp_error_retry)
                    },
                    color = Muted,
                    fontSize = 12.sp
                )
                OutlinedButton(
                    onClick = onUpdateYtDlp,
                    enabled = !updatingYtDlp && sealReady == SealDownloader.ReadyState.READY,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (updatingYtDlp) {
                            stringResource(R.string.updating)
                        } else {
                            stringResource(R.string.update_now)
                        },
                        color = Accent
                    )
                }

                TextButton(onClick = onOpenHistory) {
                    Text(stringResource(R.string.recording_history), color = Accent)
                }
                TextButton(onClick = onShowTutorial) {
                    Text(stringResource(R.string.view_tutorial), color = Accent)
                }
                Text(
                    stringResource(
                        R.string.version_format,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CheckForUpdatesSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<UpdateChecker.Result?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .clickable(enabled = !checking) {
                checking = true
                scope.launch {
                    val result = UpdateChecker.checkLatest(BuildConfig.VERSION_NAME)
                    checking = false
                    dialog = result
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            if (checking) {
                stringResource(R.string.check_for_updates_checking)
            } else {
                stringResource(R.string.check_for_updates)
            },
            color = OnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        Text(
            stringResource(R.string.check_for_updates_subtitle, BuildConfig.VERSION_NAME),
            color = Muted,
            fontSize = 12.sp
        )
    }

    when (val state = dialog) {
        is UpdateChecker.Result.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Card,
                titleContentColor = OnBg,
                textContentColor = OnBg,
                title = { Text(stringResource(R.string.update_available_title), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        stringResource(R.string.update_available_body, state.versionName),
                        color = Muted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog = null
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(state.downloadUrl))
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.download_apk), color = Accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(R.string.action_cancel), color = Muted)
                    }
                }
            )
        }
        UpdateChecker.Result.UpToDate -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Card,
                titleContentColor = OnBg,
                textContentColor = OnBg,
                title = { Text(stringResource(R.string.check_for_updates), fontWeight = FontWeight.Bold) },
                text = {
                    Text(stringResource(R.string.update_up_to_date), color = Muted, fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(R.string.help_got_it), color = Accent)
                    }
                }
            )
        }
        UpdateChecker.Result.Failed -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Card,
                titleContentColor = OnBg,
                textContentColor = OnBg,
                title = { Text(stringResource(R.string.check_for_updates), fontWeight = FontWeight.Bold) },
                text = {
                    Text(stringResource(R.string.update_check_failed), color = Muted, fontSize = 14.sp)
                },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(R.string.help_got_it), color = Accent)
                    }
                }
            )
        }
        null -> Unit
    }
}

@Composable
private fun AppLanguageSection() {
    val context = LocalContext.current
    val saved = remember { mutableStateOf(AppLocales.getSavedTag(context)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            stringResource(R.string.app_language_section),
            helpRes = R.string.help_app_language
        )
        ModeChip(
            selected = saved.value.isEmpty(),
            label = stringResource(R.string.app_language_system),
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                saved.value = ""
                AppLocales.setAndApply(context, "")
            }
        )
        ModeChip(
            selected = saved.value == "en",
            label = stringResource(R.string.app_language_en),
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                saved.value = "en"
                AppLocales.setAndApply(context, "en")
            }
        )
        ModeChip(
            selected = saved.value == "es",
            label = stringResource(R.string.app_language_es),
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                saved.value = "es"
                AppLocales.setAndApply(context, "es")
            }
        )
    }
}

@Composable
private fun AppearanceSection(
    themeId: KlipThemeId,
    onThemeIdChange: (KlipThemeId) -> Unit
) {
    val context = LocalContext.current
    val options = remember { KlipThemeId.selectable(context) }
    val shortLabel: (KlipThemeId) -> String = { id -> id.shortLabel(context) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.appearance),
            help = stringResource(R.string.appearance_help)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            options.forEach { id ->
                val swatch = when (id) {
                    KlipThemeId.GREEN -> Color(0xFF10B981)
                    KlipThemeId.BLUE -> Color(0xFF3B82F6)
                    KlipThemeId.PURPLE -> Color(0xFF8B5CF6)
                    KlipThemeId.ORANGE -> Color(0xFFF59E0B)
                    KlipThemeId.PINK -> Color(0xFFEC4899)
                    KlipThemeId.DYNAMIC -> Color(0xFF94A3B8)
                }
                val selected = themeId == id
                val dynamicBrush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF10B981),
                        Color(0xFF3B82F6),
                        Color(0xFF8B5CF6),
                        Color(0xFFEC4899),
                        Color(0xFFF59E0B),
                        Color(0xFF10B981)
                    )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (selected) 40.dp else 34.dp)
                            .clip(CircleShape)
                            .then(
                                if (id == KlipThemeId.DYNAMIC) {
                                    Modifier.background(dynamicBrush)
                                } else {
                                    Modifier.background(swatch)
                                }
                            )
                            .then(
                                if (selected) Modifier.border(2.dp, OnBg, CircleShape)
                                else Modifier
                            )
                            .semantics {
                                contentDescription = context.getString(R.string.theme_label, id.shortLabel(context))
                            }
                            .clickable { onThemeIdChange(id) },
                        contentAlignment = Alignment.Center
                    ) {}
                    Text(
                        shortLabel(id),
                        color = if (selected) Accent else Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ModeChip(
    selected: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = chipColors()
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = KlipNeutrals.ChipBg,
    labelColor = Muted,
    selectedContainerColor = Accent.copy(alpha = 0.2f),
    selectedLabelColor = Accent
)


private fun buildServicesSummary(
    context: android.content.Context,
    keyMasks: Map<SttProvider, String?>,
    groqRemainingSeconds: Double,
    autoPrimary: SttProvider?
): String {
    val ready = listOf(
        SttProvider.GROQ_FREE,
        SttProvider.INWORLD,
        SttProvider.ASSEMBLY_AI,
        SttProvider.DEEPGRAM
    ).count { !keyMasks[it].isNullOrBlank() }
    val mins = (groqRemainingSeconds / 60.0).coerceAtLeast(0.0)
    val groqName = context.getString(R.string.provider_groq_short)
    val primary = autoPrimary?.shortName(context) ?: groqName
    val readyLabel = if (ready == 1) {
        context.getString(R.string.services_ready_one)
    } else {
        context.getString(R.string.services_ready_many, ready)
    }
    return if (primary == groqName || !keyMasks[SttProvider.GROQ_FREE].isNullOrBlank()) {
        context.getString(R.string.groq_min_available, mins, readyLabel)
    } else {
        context.getString(R.string.provider_ready_summary, primary, readyLabel)
    }
}

private fun readKeyMasks(context: android.content.Context): Map<SttProvider, String?> = mapOf(
    SttProvider.GROQ_FREE to ApiKeyStore.maskDisplay(context, ApiKeyStore.ProviderSlot.GROQ),
    SttProvider.INWORLD to ApiKeyStore.maskDisplay(context, ApiKeyStore.ProviderSlot.INWORLD),
    SttProvider.ASSEMBLY_AI to ApiKeyStore.maskDisplay(context, ApiKeyStore.ProviderSlot.ASSEMBLY_AI),
    SttProvider.DEEPGRAM to ApiKeyStore.maskDisplay(context, ApiKeyStore.ProviderSlot.DEEPGRAM),
    SttProvider.PAID_FALLBACK to ApiKeyStore.maskDisplay(context, ApiKeyStore.ProviderSlot.PAID_FALLBACK)
)

private suspend fun runTranscription(
    context: android.content.Context,
    uri: Uri,
    fileName: String,
    mode: LanguageMode,
    forcedLanguageCode: String? = null,
    diarize: Boolean,
    routingSelection: RoutingSelection,
    compressForStt: Boolean = false,
    savedInDownloads: Boolean = false,
    onPhase: (UiPhase) -> Unit,
    onText: (String) -> Unit,
    onMeta: (TranscriptionResult, historyId: String?) -> Unit,
    onBalance: (DeepgramBillingClient.BalanceSnapshot) -> Unit,
    onAppSpent: (Double) -> Unit,
    onHistoryChanged: (List<BillingManager.HistoryEntry>) -> Unit,
    snackbar: SnackbarHostState
) {
    if (!ApiKeyStore.hasAnyKey(context)) {
        onPhase(UiPhase.Error(context.getString(R.string.error_no_key)))
        return
    }
    if (routingSelection is RoutingSelection.Manual &&
        !SttRouter.isConfigured(context, routingSelection.provider)
    ) {
        onPhase(
            UiPhase.Error(
                message = context.getString(
                    R.string.manual_test_error,
                    routingSelection.provider.displayName(context),
                    context.getString(R.string.block_no_key)
                ),
                isManualTest = true
            )
        )
        return
    }

    var extracted: AudioExtractor.ExtractedAudio? = null
    var sttCompact: File? = null
    var balanceBefore: Double? = null
    try {
        val dgKey = ApiKeyStore.getDeepgram(context)
        if (dgKey.isNotBlank()) {
            val beforeSnap = withContext(Dispatchers.IO) {
                DeepgramBillingClient.fetchOfficialBalance(context, dgKey)
            }
            onBalance(beforeSnap)
            balanceBefore = beforeSnap.balanceUsd
        }

        onPhase(UiPhase.Extracting)
        extracted = withContext(Dispatchers.IO) {
            AudioExtractor.prepareAudio(context, uri)
        }

        val audioForStt = if (compressForStt) {
            onPhase(
                UiPhase.Transcribing(context.getString(R.string.status_preparing_light_audio))
            )
            withContext(Dispatchers.IO) {
                SealDownloader.compressAudioForStt(context, extracted!!.file).also { sttCompact = it }
            }
        } else {
            extracted.file
        }
        val mimeForStt = if (sttCompact != null) "audio/mp4" else extracted.mimeType

        val motorHint = when (routingSelection) {
            is RoutingSelection.Auto -> context.getString(R.string.status_transcribing_automatic)
            is RoutingSelection.Manual -> context.getString(
                R.string.status_transcribing_with,
                routingSelection.provider.displayName(context)
            )
        }
        onPhase(UiPhase.Transcribing(motorHint))
        val result = withContext(Dispatchers.IO) {
            SttRouter.transcribe(
                context = context,
                audioFile = audioForStt,
                mimeType = mimeForStt,
                mode = mode,
                diarize = diarize,
                deepgramBalanceUsd = balanceBefore,
                selection = routingSelection,
                forcedLanguageCode = forcedLanguageCode,
                savedInDownloads = savedInDownloads
            )
        }

        val duration = result.durationSeconds ?: 0.0
        val cost = result.costUsd
        if (cost > 0.0) {
            BillingManager.addAppSpent(context, cost)
        }
        BillingManager.recordLocalUsage(context, duration)
        onAppSpent(BillingManager.getAppSpent(context))

        var afterBalance: Double? = null
        if (result.provider == SttProvider.DEEPGRAM && dgKey.isNotBlank()) {
            val afterSnap = withContext(Dispatchers.IO) {
                DeepgramBillingClient.fetchOfficialBalance(context, dgKey)
            }
            onBalance(afterSnap)
            afterBalance = afterSnap.balanceUsd
        }

        val persistedAudio = extracted?.let { audioFile ->
            runCatching {
                val dest = File(
                    context.filesDir,
                    "recordings/rec_${System.currentTimeMillis()}_${audioFile.file.name}"
                )
                dest.parentFile?.mkdirs()
                audioFile.file.copyTo(dest, overwrite = true)
                dest.absolutePath
            }.getOrNull()
        }

        val entry = BillingManager.newHistoryEntry(
            fileName = fileName,
            durationSeconds = duration,
            mode = mode,
            diarize = diarize,
            appCostUsd = cost,
            balanceBefore = balanceBefore,
            balanceAfter = afterBalance,
            transcript = result.transcript,
            audioPath = persistedAudio,
            provider = result.providerLabel
        )
        BillingManager.appendHistory(context, entry)
        if (result.transcript.isNotBlank()) {
            TranscriptHistoryStore.upsertById(
                context = context,
                id = entry.id,
                sourceTitleOrUrl = fileName,
                providerUsed = result.providerLabel,
                durationSec = duration,
                text = result.transcript,
                timestampMs = entry.timestampMs
            )
            // Auto-guardar guion en Descargas/KlipNote/Texto
            saveTranscriptTxt(context, fileName, result.transcript)
        }
        onHistoryChanged(BillingManager.loadHistory(context))

        onText(result.transcript)
        onMeta(result, entry.id)
        onPhase(UiPhase.Done(result))
        if (result.transcript.isNotBlank()) {
            copyToClipboard(context, result.transcript)
            snackbar.showSnackbar(context.getString(R.string.copied))
        }
    } catch (e: ManualRouteException) {
        onPhase(
            UiPhase.Error(
                message = e.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.error_generic),
                isManualTest = true
            )
        )
    } catch (e: Exception) {
        onPhase(
            UiPhase.Error(
                message = e.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.error_generic),
                isManualTest = routingSelection is RoutingSelection.Manual
            )
        )
    } finally {
        AudioExtractor.cleanup(extracted)
        sttCompact?.let { runCatching { it.delete() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SiteCookiesSheet(
    site: CookieSite,
    onDismiss: () -> Unit,
    onSaved: (Int) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf("webview") } // webview | paste
    var pasteText by remember { mutableStateOf("") }
    var status by remember(site) {
        mutableStateOf(context.getString(R.string.cookie_webview_title, site.label))
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var busy by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Card,
        contentColor = OnBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.cookie_webview_heading, site.label),
                fontWeight = FontWeight.Bold,
                color = OnBg,
                fontSize = 18.sp
            )
            Text(
                stringResource(R.string.cookie_webview_privacy),
                color = Muted,
                fontSize = 12.sp
            )
            CookieStore.noteFor(context, site)?.let {
                Text(it, color = Amber, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip(
                    selected = mode == "webview",
                    label = stringResource(R.string.cookie_mode_webview),
                    enabled = !busy,
                    onClick = { mode = "webview" }
                )
                ModeChip(
                    selected = mode == "paste",
                    label = stringResource(R.string.cookie_paste_txt),
                    enabled = !busy,
                    onClick = { mode = "paste" }
                )
            }

            if (mode == "webview") {
                DisposableEffect(site) {
                    onDispose {
                        webViewRef?.apply {
                            stopLoading()
                            destroy()
                        }
                        webViewRef = null
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KlipNeutrals.ChipBg)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString().orEmpty()
                                        return !url.startsWith("https://", ignoreCase = true)
                                    }

                                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        url: String?
                                    ): Boolean {
                                        return !url.orEmpty().startsWith("https://", ignoreCase = true)
                                    }
                                }
                                loadUrl(site.loginUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { webViewRef = it }
                    )
                }
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        try {
                            CookieManager.getInstance().flush()
                            val ua = webViewRef?.settings?.userAgentString
                            val count = CookieStore.saveFromCookieManager(context, site, ua)
                            status = context.getString(R.string.cookie_saved_count, count)
                            onSaved(count)
                        } catch (t: Throwable) {
                            status = t.message ?: context.getString(R.string.cookie_save_failed)
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = OnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save_webview_cookies), fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    placeholder = {
                        Text(stringResource(R.string.cookie_paste_placeholder), color = Muted)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Muted.copy(alpha = 0.4f),
                        focusedTextColor = OnBg,
                        unfocusedTextColor = OnBg,
                        cursorColor = Accent
                    )
                )
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        try {
                            val count = CookieStore.saveFromPaste(context, site, pasteText)
                            status = context.getString(R.string.cookie_saved_count, count)
                            onSaved(count)
                        } catch (t: Throwable) {
                            status = t.message ?: context.getString(R.string.cookie_paste_invalid)
                            busy = false
                        }
                    },
                    enabled = !busy && pasteText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = OnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save_pasted_cookies), fontWeight = FontWeight.SemiBold)
                }
            }

            Text(status, color = Muted, fontSize = 12.sp)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.close), color = Muted)
            }
        }
    }
}
