package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import com.m3uplayer.app.MainViewModel

@Composable
fun SeriesDetailScreen(vm: MainViewModel, name: String) {
    val series = vm.library?.seriesMap?.get(name)
    if (series == null) {
        EmptyHint("المسلسل غير موجود")
        return
    }
    val key = vm.seriesKey(name)
    val seasons = series.seasons
    var season by rememberSaveable(name) { mutableStateOf(seasons.firstOrNull() ?: 1) }
    val target = vm.continueTarget(series)
    val hasProgress = series.episodes.any { vm.user.progress.containsKey(it.id) }
    val episodes = series.episodes.filter { it.season == season }
    var showListDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.openSeries = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Text(
                    series.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = { vm.toggleFav(key) }) {
                    val fav = vm.isFav(key)
                    Icon(
                        if (fav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "مفضلة", tint = if (fav) Accent else TextDim,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Box(Modifier.width(110.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF))) {
                    if (!series.logo.isNullOrBlank()) {
                        AsyncImage(series.logo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${series.episodes.size} حلقة · ${seasons.size} موسم", color = TextDim)
                    Text(series.group, color = TextDim, fontSize = 12.sp)
                    Button(onClick = { vm.play(target) }) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (hasProgress) "متابعة · ${target.episodeLabel(series.episodes.indexOf(target))}" else "ابدأ المشاهدة")
                    }
                    OutlinedButton(onClick = { showListDialog = true }) { Text("إضافة إلى قائمة مفضلة…") }
                }
            }
        }
        if (seasons.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons) { s ->
                        FilterChip(selected = s == season, onClick = { season = s }, label = { Text("الموسم $s") })
                    }
                }
            }
        }
        itemsIndexed(episodes, key = { _, e -> e.id }) { index, e ->
            val f = vm.progressOf(e.id)
            Row(
                Modifier
                    .fillMaxWidth()
                    .tvClickable(focusScale = 1f, background = CardBg, onClick = { vm.play(e) })
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    if (f >= 0.95f) Icons.Default.CheckCircle else Icons.Default.PlayArrow, null,
                    tint = if (f >= 0.95f) Accent else TextDim,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        e.episodeLabel(series.episodes.indexOf(e)), fontSize = 15.sp,
                    )
                    Text(e.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDim, fontSize = 12.sp)
                    if (f in 0.01f..0.949f) {
                        LinearProgressIndicator(
                            progress = { f }, color = Accent, trackColor = Color(0x44FFFFFF),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp),
                        )
                    }
                }
            }
        }
    }

    if (showListDialog) {
        ChooseListDialog(vm, listOf(key), onDismiss = { showListDialog = false })
    }
}
