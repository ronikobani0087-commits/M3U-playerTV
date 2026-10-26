package com.m3uplayer.app.player

import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.Kind
import com.m3uplayer.app.data.PlayRequest
import com.m3uplayer.app.data.USER_AGENT
import com.m3uplayer.app.data.findActivity
import com.m3uplayer.app.ui.episodeLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(vm: MainViewModel, req: PlayRequest) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    val scope = rememberCoroutineScope()

    var current by remember(req) { mutableStateOf(req.item) }
    val currentNow by rememberUpdatedState(current)
    var overlayVisible by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var retries by remember { mutableIntStateOf(0) }

    val player = remember {
        val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val dataSource = OkHttpDataSource.Factory(http).setUserAgent(USER_AGENT)
        ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .build()
    }

    fun saveNow() {
        val d = player.duration
        if (currentNow.kind != Kind.LIVE && d > 0) vm.onProgress(currentNow, player.currentPosition, d)
    }

    val nextEpisode = remember(current.id) {
        val i = req.queue.indexOfFirst { it.id == current.id }
        if (i >= 0) req.queue.getOrNull(i + 1) else null
    }

    // تدوير الشاشة وإخفاء أشرطة النظام أثناء التشغيل
    DisposableEffect(Unit) {
        val act = activity
        act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = act?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) errorText = null
                if (state == Player.STATE_ENDED && currentNow.kind != Kind.LIVE) {
                    val d = player.duration
                    if (d > 0) vm.onProgress(currentNow, d, d)
                    val i = req.queue.indexOfFirst { it.id == currentNow.id }
                    val next = if (i >= 0) req.queue.getOrNull(i + 1) else null
                    if (next != null) current = next else vm.closePlayer()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (currentNow.kind == Kind.LIVE && retries < 3) {
                    retries++
                    scope.launch {
                        delay(2_000)
                        player.prepare()
                    }
                } else {
                    errorText = "تعذر تشغيل هذا المحتوى. تحقق من الرابط أو من اتصال الإنترنت."
                }
            }
        }
        player.addListener(listener)
        onDispose {
            saveNow()
            player.removeListener(listener)
            player.release()
        }
    }

    // تحميل العنصر الحالي (وعند الانتقال للحلقة التالية)
    LaunchedEffect(current.id) {
        errorText = null
        retries = 0
        val item = current
        val media = MediaItem.Builder().setUri(item.url).apply {
            if (item.url.contains(".m3u8", ignoreCase = true)) setMimeType(MimeTypes.APPLICATION_M3U8)
        }.build()
        val start = if (item.kind == Kind.LIVE) 0L else vm.resumePosition(item)
        player.setMediaItem(media, start)
        player.prepare()
        player.playWhenReady = true
    }

    // حفظ موضع المشاهدة كل 10 ثوانٍ
    LaunchedEffect(current.id) {
        while (true) {
            delay(10_000)
            saveNow()
        }
    }

    BackHandler { vm.closePlayer() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                PlayerView(c).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                    useController = true
                    keepScreenOn = true
                    controllerShowTimeoutMs = 4_000
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v ->
                        overlayVisible = v == View.VISIBLE
                    })
                    requestFocus()
                }
            },
        )

        AnimatedVisibility(visible = overlayVisible, modifier = Modifier.align(Alignment.TopCenter)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.closePlayer() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Text(
                    text = if (current.kind == Kind.SERIES) {
                        "${current.seriesName.orEmpty()} · ${current.episodeLabel(req.queue.indexOfFirst { it.id == current.id })}"
                    } else current.name,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (nextEpisode != null) {
                    TextButton(onClick = { current = nextEpisode }) { Text("الحلقة التالية", color = Color.White) }
                }
            }
        }

        errorText?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).background(Color(0xCC000000)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(msg, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        errorText = null
                        retries = 0
                        player.prepare()
                        player.playWhenReady = true
                    }) { Text("إعادة المحاولة") }
                    OutlinedButton(onClick = { vm.closePlayer() }) { Text("رجوع") }
                }
            }
        }
    }
}
