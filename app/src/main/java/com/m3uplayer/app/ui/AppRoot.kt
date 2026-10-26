package com.m3uplayer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.Tab
import com.m3uplayer.app.player.PlayerScreen

@Composable
fun AppRoot(vm: MainViewModel) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AppTheme {
            val holder = rememberSaveableStateHolder()
            Box(Modifier.fillMaxSize().background(AppBackground)) {
                val req = vm.playing
                BackHandler(enabled = req == null && vm.showAdd && vm.user.playlists.isNotEmpty()) {
                    vm.error = null
                    vm.addingPlaylist = false
                }
                when {
                    req != null -> PlayerScreen(vm, req)
                    vm.showAdd -> AddPlaylistScreen(vm)
                    else -> holder.SaveableStateProvider("main") { MainScaffold(vm, holder) }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel, holder: SaveableStateHolder) {
    BackHandler(enabled = vm.openSeries != null) { vm.openSeries = null }
    BackHandler(enabled = vm.openSeries == null && vm.tab != Tab.HOME) { vm.tab = Tab.HOME }

    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 600.dp
        val content: @Composable (Modifier) -> Unit = { m ->
            Box(m) {
                val series = vm.openSeries
                if (series != null) {
                    holder.SaveableStateProvider("series_$series") { SeriesDetailScreen(vm, series) }
                } else {
                    holder.SaveableStateProvider("tab_${vm.tab.name}") {
                        when (vm.tab) {
                            Tab.HOME -> HomeScreen(vm)
                            Tab.LIVE -> LiveScreen(vm)
                            Tab.MOVIES -> MoviesScreen(vm)
                            Tab.SERIES -> SeriesScreen(vm)
                            Tab.FAVORITES -> FavoritesScreen(vm)
                            Tab.SETTINGS -> SettingsScreen(vm)
                        }
                    }
                }
                if (vm.loading && vm.library != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(androidx.compose.ui.Alignment.TopCenter), color = Accent)
                }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(containerColor = Color(0x33000000)) {
                    Spacer(Modifier.weight(1f))
                    Tab.entries.forEach { t ->
                        NavigationRailItem(
                            selected = vm.tab == t,
                            onClick = { vm.openSeries = null; vm.tab = t },
                            icon = { Icon(t.icon(), contentDescription = t.label()) },
                            label = { Text(t.label(), fontSize = 11.sp, maxLines = 1) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                content(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                content(Modifier.weight(1f).fillMaxWidth())
                NavigationBar(containerColor = Color(0x66000000)) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = vm.tab == t,
                            onClick = { vm.openSeries = null; vm.tab = t },
                            icon = { Icon(t.icon(), contentDescription = t.label()) },
                            label = { Text(t.label(), fontSize = 10.sp, maxLines = 1) },
                        )
                    }
                }
            }
        }
    }
}
