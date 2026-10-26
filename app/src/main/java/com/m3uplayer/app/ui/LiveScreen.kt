package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.Item

@Composable
fun LiveScreen(vm: MainViewModel) {
    val lib = vm.library ?: run { NeedLibrary(vm); return }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var pickGroup by remember { mutableStateOf(false) }
    var groupActions by remember { mutableStateOf<String?>(null) }
    var menuKey by remember { mutableStateOf<String?>(null) }

    val channels = remember(lib, group, query) {
        lib.live.filter { (group == null || it.group == group) && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                GroupList(
                    groups = lib.liveGroups, counts = lib.liveGroupCounts, total = lib.live.size,
                    selected = group, onSelect = { group = it }, onLongPress = { groupActions = it },
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                )
                Column(Modifier.weight(1f)) {
                    FilterBar(group, query, { query = it }, onPickGroup = null)
                    ChannelList(vm, channels, onMenu = { menuKey = it })
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                FilterBar(group, query, { query = it }, onPickGroup = { pickGroup = true })
                ChannelList(vm, channels, onMenu = { menuKey = it })
            }
        }
    }

    if (pickGroup) {
        GroupPickerDialog(
            groups = lib.liveGroups, counts = lib.liveGroupCounts, total = lib.live.size,
            selected = group, onSelect = { group = it },
            onLongPress = { groupActions = it }, onDismiss = { pickGroup = false },
        )
    }
    groupActions?.let { g ->
        GroupActionsDialog(vm, g, lib.live.filter { it.group == g }.map { it.id }) { groupActions = null }
    }
    menuKey?.let { KeyActionsDialog(vm, it) { menuKey = null } }
}

@Composable
private fun ChannelList(vm: MainViewModel, channels: List<Item>, onMenu: (String) -> Unit) {
    if (channels.isEmpty()) {
        EmptyHint("لا توجد قنوات مطابقة")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(channels, key = { it.id }) { item ->
            ChannelRow(
                item = item,
                fav = vm.isFav(item.id),
                pinned = vm.isPinned(item.id),
                onPlay = { vm.play(item) },
                onMenu = { onMenu(item.id) },
                onFav = { vm.toggleFav(item.id) },
            )
        }
    }
}

@Composable
private fun ChannelRow(item: Item, fav: Boolean, pinned: Boolean, onPlay: () -> Unit, onMenu: () -> Unit, onFav: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .tvClickable(focusScale = 1f, background = CardBg, onClick = onPlay, onLongClick = onMenu)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LogoThumb(item.logo, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
            Text(
                if (pinned) "${item.group} · مثبتة في الرئيسية" else item.group,
                maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDim, fontSize = 12.sp,
            )
        }
        IconButton(onClick = onFav) {
            Icon(
                if (fav) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "مفضلة", tint = if (fav) Accent else TextDim,
            )
        }
        IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, contentDescription = "المزيد", tint = TextDim) }
    }
}
