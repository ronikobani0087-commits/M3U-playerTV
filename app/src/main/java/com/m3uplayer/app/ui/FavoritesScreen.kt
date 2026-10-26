package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.DEFAULT_LIST

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val lists = vm.user.favLists
    var selectedId by rememberSaveable { mutableStateOf(DEFAULT_LIST) }
    val selected = lists.firstOrNull { it.id == selectedId } ?: lists.firstOrNull()

    var newListDialog by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var itemMenu by remember { mutableStateOf<String?>(null) }
    var moveKey by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "المفضلة", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lists, key = { it.id }) { l ->
                FilterChip(
                    selected = l.id == selected?.id,
                    onClick = { selectedId = l.id },
                    label = { Text("${l.name} (${l.keys.size})") },
                )
            }
            item {
                AssistChip(
                    onClick = { newListDialog = true },
                    label = { Text("قائمة جديدة") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) },
                )
            }
        }

        if (selected == null) {
            EmptyHint("لا توجد قوائم")
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.name, Modifier.weight(1f), fontSize = 16.sp, color = TextDim)
                IconButton(onClick = { listMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "خيارات القائمة") }
            }
            val tiles = selected.keys.mapNotNull { vm.tileFor(it) }
            if (tiles.isEmpty()) {
                EmptyHint("القائمة فارغة.\nأضف قنوات أو أفلاماً أو مسلسلات (بنجمة ⭐ أو بالضغط المطوّل) من الأقسام الأخرى.")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(tiles, key = { it.key }) { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .tvClickable(focusScale = 1f, background = CardBg, onClick = { vm.open(t.key) }, onLongClick = { itemMenu = t.key })
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LogoThumb(t.logo, 52.dp)
                            Column(Modifier.weight(1f)) {
                                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                                Text(t.subtitle, color = TextDim, fontSize = 12.sp)
                            }
                            IconButton(onClick = { itemMenu = t.key }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "المزيد", tint = TextDim)
                            }
                        }
                    }
                }
            }
        }
    }

    if (newListDialog) {
        TextInputDialog("قائمة مفضلة جديدة", "", "إنشاء", { newListDialog = false }) {
            selectedId = vm.createList(it)
            newListDialog = false
        }
    }

    if (listMenu && selected != null) {
        val actions = buildList {
            add(MenuAction("إعادة تسمية القائمة", dismiss = false) { listMenu = false; renameDialog = true })
            if (selected.id != DEFAULT_LIST) {
                add(MenuAction("حذف القائمة") {
                    selectedId = DEFAULT_LIST
                    vm.deleteList(selected.id)
                })
            }
        }
        ActionDialog(selected.name, actions) { listMenu = false }
    }
    if (renameDialog && selected != null) {
        TextInputDialog("إعادة تسمية القائمة", selected.name, "حفظ", { renameDialog = false }) {
            vm.renameList(selected.id, it)
            renameDialog = false
        }
    }

    // إجراءات عنصر داخل قائمة المفضلة: ترتيب، نقل، إزالة
    itemMenu?.let { key ->
        val tile = vm.tileFor(key)
        if (tile == null || selected == null) {
            itemMenu = null
        } else {
            val actions = buildList {
                add(MenuAction(if (tile.subtitle == "مسلسل") "فتح المسلسل" else "تشغيل") { vm.open(key) })
                add(MenuAction("نقل لأعلى") { vm.shift(selected.id, key, -1) })
                add(MenuAction("نقل لأسفل") { vm.shift(selected.id, key, +1) })
                if (lists.size > 1) {
                    add(MenuAction("نقل إلى قائمة أخرى…", dismiss = false) { itemMenu = null; moveKey = key })
                }
                add(MenuAction("إزالة من هذه القائمة") { vm.removeFromList(selected.id, key) })
            }
            ActionDialog(tile.title, actions) { itemMenu = null }
        }
    }

    moveKey?.let { key ->
        val from = selected
        if (from == null) {
            moveKey = null
        } else {
            val targets = lists.filter { it.id != from.id }.map { l ->
                MenuAction(l.name) { vm.moveBetween(from.id, l.id, key) }
            }
            ActionDialog("نقل إلى…", targets) { moveKey = null }
        }
    }
}
