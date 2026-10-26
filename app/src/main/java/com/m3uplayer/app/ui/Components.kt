@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.m3uplayer.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.Item
import com.m3uplayer.app.data.Kind

/** نقر + ضغط مطوّل + إبراز عند التركيز (للتحكم بالريموت على شاشات الأندرويد). */
@Composable
fun Modifier.tvClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    background: Color = Color.Transparent,
    focusScale: Float = 1.04f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusScale else 1f, label = "focusScale")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .onFocusChanged { focused = it.isFocused }
        .clip(shape)
        .background(background)
        .border(if (focused) 3.dp else 0.dp, if (focused) Accent else Color.Transparent, shape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
}

@Composable
fun PosterCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    aspect: Float = 2f / 3f,
    progress: Float? = null,
    badge: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier.tvClickable(background = CardBg, onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .background(Color(0x33FFFFFF))
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (contentScale == ContentScale.Fit) 12.dp else 0.dp),
                )
            } else {
                Icon(
                    Icons.Default.PlayCircle, null, tint = TextDim,
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                )
            }
            if (badge != null) {
                Text(
                    badge,
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color(0xCCE53935), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (progress != null && progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                    color = Accent,
                    trackColor = Color(0x66000000),
                )
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(
                title, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = TextDim)
            }
        }
    }
}

@Composable
fun LogoThumb(url: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x22FFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        } else {
            Icon(Icons.Default.LiveTv, null, tint = TextDim, modifier = Modifier.size(size / 2))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

@Composable
fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextDim, fontSize = 15.sp)
    }
}

@Composable
fun NeedLibrary(vm: MainViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (vm.loading) {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(16.dp))
            Text(vm.loadMessage, color = TextDim)
        } else {
            Text(vm.error ?: "لا توجد قائمة تشغيل نشطة", color = if (vm.error != null) MaterialTheme.colorScheme.error else TextDim)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.refreshActive() }) { Text("إعادة المحاولة") }
            TextButton(onClick = { vm.addingPlaylist = true }) { Text("إضافة قائمة أخرى") }
        }
    }
}

fun Item.episodeLabel(index: Int = 0): String {
    val ep = if (episode > 0) episode else index + 1
    return if (season > 0) "الموسم $season · الحلقة $ep" else "الحلقة $ep"
}

// ---------------------------------------------------------------- الباقات

@Composable
fun GroupList(
    groups: List<String>,
    counts: Map<String, Int>,
    total: Int,
    selected: String?,
    onSelect: (String?) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "__all") {
            GroupRow("كل الباقات", total, selected == null, onClick = { onSelect(null) })
        }
        items(groups, key = { it }) { g ->
            GroupRow(g, counts[g] ?: 0, selected == g, onClick = { onSelect(g) }, onLongClick = { onLongPress(g) })
        }
    }
}

@Composable
private fun GroupRow(name: String, count: Int, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .tvClickable(
                shape = RoundedCornerShape(10.dp),
                background = if (selected) Purple else CardBg,
                focusScale = 1f,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Text("$count", color = if (selected) Color.White else TextDim, fontSize = 12.sp)
    }
}

@Composable
fun GroupPickerDialog(
    groups: List<String>,
    counts: Map<String, Int>,
    total: Int,
    selected: String?,
    onSelect: (String?) -> Unit,
    onLongPress: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر الباقة") },
        text = {
            GroupList(
                groups, counts, total, selected,
                onSelect = { onSelect(it); onDismiss() },
                onLongPress = onLongPress,
                modifier = Modifier.fillMaxWidth().height(380.dp),
            )
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
fun FilterBar(
    groupLabel: String?,
    query: String,
    onQuery: (String) -> Unit,
    onPickGroup: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onPickGroup != null) {
            OutlinedButton(onClick = onPickGroup) {
                Icon(Icons.Default.Folder, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(groupLabel ?: "كل الباقات", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 130.dp))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("بحث…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, null) }
                }
            },
        )
    }
}

// ---------------------------------------------------------------- الحوارات

data class MenuAction(val label: String, val dismiss: Boolean = true, val onClick: () -> Unit)

@Composable
fun ActionDialog(title: String, actions: List<MenuAction>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                actions.forEach { a ->
                    TextButton(
                        onClick = {
                            if (a.dismiss) onDismiss()
                            a.onClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(a.label, Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** اختيار قائمة مفضلة (أو إنشاء قائمة جديدة) لإضافة مجموعة عناصر إليها. */
@Composable
fun ChooseListDialog(
    vm: MainViewModel,
    keys: List<String>,
    onDismiss: () -> Unit,
    title: String = "اختر قائمة المفضلة",
    suggestedName: String = "",
) {
    var newName by remember { mutableStateOf(suggestedName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                vm.user.favLists.forEach { l ->
                    TextButton(
                        onClick = { vm.addToList(l.id, keys); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("${l.name}  (${l.keys.size})", Modifier.fillMaxWidth()) }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("قائمة جديدة") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val id = vm.createList(newName)
                        vm.addToList(id, keys)
                        onDismiss()
                    },
                ) { Text("إنشاء قائمة وإضافة") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** قائمة إجراءات قناة/فيلم/مسلسل: تشغيل، مفضلة، تثبيت في الرئيسية. */
@Composable
fun KeyActionsDialog(vm: MainViewModel, key: String, onDismiss: () -> Unit) {
    val tile = vm.tileFor(key)
    if (tile == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    var sub by remember { mutableIntStateOf(0) }
    when (sub) {
        1 -> ChooseListDialog(vm, listOf(key), onDismiss)
        2 -> TextInputDialog("اسم المباراة / عنوان التثبيت", tile.title, "تثبيت", onDismiss) {
            vm.pin(key, it)
            onDismiss()
        }
        else -> {
            val actions = buildList {
                add(MenuAction(if (tile.kind == Kind.SERIES) "فتح المسلسل" else "تشغيل") { vm.open(key) })
                if (vm.isFav(key)) add(MenuAction("إزالة من المفضلة") { vm.toggleFav(key) })
                else add(MenuAction("إضافة إلى المفضلة") { vm.toggleFav(key) })
                add(MenuAction("إضافة إلى قائمة مفضلة أخرى…", dismiss = false) { sub = 1 })
                if (tile.kind == Kind.LIVE) {
                    if (vm.isPinned(key)) add(MenuAction("إلغاء التثبيت من الرئيسية") { vm.unpin(key) })
                    else add(MenuAction("تثبيت في الرئيسية (مباراة مهمة)", dismiss = false) { sub = 2 })
                }
            }
            ActionDialog(tile.title, actions, onDismiss)
        }
    }
}

/** إجراءات باقة كاملة: إضافتها إلى قائمة مفضلة. */
@Composable
fun GroupActionsDialog(vm: MainViewModel, group: String, keys: List<String>, onDismiss: () -> Unit) {
    ChooseListDialog(
        vm = vm,
        keys = keys,
        onDismiss = onDismiss,
        title = "إضافة الباقة «$group» (${keys.size}) إلى قائمة",
        suggestedName = group,
    )
}
