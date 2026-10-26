package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m3uplayer.app.MainViewModel

data class GridEntry(val key: String, val title: String, val logo: String?, val group: String, val progress: Float?)

@Composable
fun MoviesScreen(vm: MainViewModel) {
    val lib = vm.library ?: run { NeedLibrary(vm); return }
    val entries = remember(lib, vm.user.progress) {
        lib.movies.map { GridEntry(it.id, it.name, it.logo, it.group, vm.progressOf(it.id).takeIf { f -> f in 0.01f..0.949f }) }
    }
    PosterGrid(vm, entries, lib.movieGroups, lib.movieGroupCounts, "movies", onOpen = { vm.open(it) })
}

@Composable
fun SeriesScreen(vm: MainViewModel) {
    val lib = vm.library ?: run { NeedLibrary(vm); return }
    val entries = remember(lib) {
        lib.series.map { GridEntry(vm.seriesKey(it.name), it.name, it.logo, it.group, null) }
    }
    PosterGrid(vm, entries, lib.seriesGroups, lib.seriesGroupCounts, "series", onOpen = { vm.open(it) })
}

@Composable
private fun PosterGrid(
    vm: MainViewModel,
    entries: List<GridEntry>,
    groups: List<String>,
    counts: Map<String, Int>,
    stateKey: String,
    onOpen: (String) -> Unit,
) {
    var group by rememberSaveable(stateKey) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(stateKey) { mutableStateOf("") }
    var pickGroup by remember { mutableStateOf(false) }
    var groupActions by remember { mutableStateOf<String?>(null) }
    var menuKey by remember { mutableStateOf<String?>(null) }

    val shown = remember(entries, group, query) {
        entries.filter { (group == null || it.group == group) && (query.isBlank() || it.title.contains(query, ignoreCase = true)) }
    }

    Column(Modifier.fillMaxSize()) {
        FilterBar(group, query, { query = it }, onPickGroup = { pickGroup = true })
        if (shown.isEmpty()) {
            EmptyHint(if (entries.isEmpty()) "لا يوجد محتوى في هذا القسم ضمن القائمة" else "لا توجد نتائج مطابقة")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { it.key }) { e ->
                    PosterCard(
                        title = e.title,
                        subtitle = null,
                        imageUrl = e.logo,
                        progress = e.progress,
                        onClick = { onOpen(e.key) },
                        onLongClick = { menuKey = e.key },
                    )
                }
            }
        }
    }

    if (pickGroup) {
        GroupPickerDialog(
            groups = groups, counts = counts, total = entries.size,
            selected = group, onSelect = { group = it },
            onLongPress = { groupActions = it }, onDismiss = { pickGroup = false },
        )
    }
    groupActions?.let { g ->
        GroupActionsDialog(vm, g, entries.filter { it.group == g }.map { it.key }) { groupActions = null }
    }
    menuKey?.let { KeyActionsDialog(vm, it) { menuKey = null } }
}
