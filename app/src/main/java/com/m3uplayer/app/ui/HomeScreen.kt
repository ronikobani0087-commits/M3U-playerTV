package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.Kind

@Composable
fun HomeScreen(vm: MainViewModel) {
    val lib = vm.library ?: run { NeedLibrary(vm); return }
    val continueList = remember(vm.user.progress, lib) { vm.continueWatching() }
    var menuKey by remember { mutableStateOf<String?>(null) }

    val pinned = vm.user.pinned.mapNotNull { p -> lib.byId[p.itemId]?.let { p to it } }
    val favRows = vm.user.favLists.map { l -> l to l.keys.mapNotNull { vm.tileFor(it) } }.filter { it.second.isNotEmpty() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الرئيسية", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${vm.activePlaylist()?.name.orEmpty()} · ${lib.live.size} قناة · ${lib.movies.size} فيلم · ${lib.series.size} مسلسل",
                        color = TextDim,
                    )
                }
                IconButton(onClick = { vm.refreshActive() }) { Icon(Icons.Default.Refresh, contentDescription = "تحديث") }
            }
        }

        if (continueList.isNotEmpty()) {
            item {
                Column {
                    SectionTitle("متابعة المشاهدة")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(continueList, key = { it.item.id }) { e ->
                            val it = e.item
                            PosterCard(
                                title = it.seriesName ?: it.name,
                                subtitle = if (it.kind == Kind.SERIES) it.episodeLabel() else "فيلم",
                                imageUrl = it.logo,
                                modifier = Modifier.width(220.dp),
                                aspect = 16f / 9f,
                                progress = e.fraction,
                                onClick = { vm.play(it) },
                                onLongClick = { menuKey = it.seriesName?.let(vm::seriesKey) ?: it.id },
                            )
                        }
                    }
                }
            }
        }

        if (pinned.isNotEmpty()) {
            item {
                Column {
                    SectionTitle("مباريات وقنوات مهمة")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pinned, key = { it.second.id }) { (p, item) ->
                            PosterCard(
                                title = p.label,
                                subtitle = item.name,
                                imageUrl = item.logo,
                                modifier = Modifier.width(200.dp),
                                aspect = 16f / 9f,
                                badge = "مباشر",
                                contentScale = ContentScale.Fit,
                                onClick = { vm.play(item) },
                                onLongClick = { menuKey = item.id },
                            )
                        }
                    }
                }
            }
        }

        items(favRows, key = { it.first.id }) { (list, tiles) ->
            Column {
                SectionTitle(list.name)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tiles, key = { it.key }) { t ->
                        val live = t.kind == Kind.LIVE
                        PosterCard(
                            title = t.title,
                            subtitle = t.subtitle,
                            imageUrl = t.logo,
                            modifier = Modifier.width(if (live) 180.dp else 130.dp),
                            aspect = if (live) 16f / 9f else 2f / 3f,
                            contentScale = if (live) ContentScale.Fit else ContentScale.Crop,
                            onClick = { vm.open(t.key) },
                            onLongClick = { menuKey = t.key },
                        )
                    }
                }
            }
        }

        if (continueList.isEmpty() && pinned.isEmpty() && favRows.isEmpty()) {
            item {
                Text(
                    "ما تتابعه وما تضيفه إلى المفضلة أو تثبّته من البث الحي سيظهر هنا.\n" +
                        "ابدأ من أقسام البث الحي أو الأفلام أو المسلسلات.",
                    color = TextDim,
                )
            }
        }
    }

    menuKey?.let { KeyActionsDialog(vm, it) { menuKey = null } }
}
