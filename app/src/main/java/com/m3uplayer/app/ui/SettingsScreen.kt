package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3uplayer.app.MainViewModel
import com.m3uplayer.app.data.PlaylistEntry

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var confirmDelete by remember { mutableStateOf<PlaylistEntry?>(null) }
    val activeId = vm.activePlaylist()?.id

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("الإعدادات", style = MaterialTheme.typography.headlineSmall) }
        item { SectionTitle("قوائم التشغيل") }

        items(vm.user.playlists, key = { it.id }) { p ->
            Surface(color = CardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(p.name, Modifier.weight(1f), fontSize = 16.sp)
                        if (p.id == activeId) Text("نشطة", color = Accent, fontSize = 13.sp)
                    }
                    Text(p.url, color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (p.id != activeId) TextButton(onClick = { vm.selectPlaylist(p.id) }) { Text("تفعيل") }
                        else TextButton(onClick = { vm.refreshActive() }, enabled = !vm.loading) { Text("تحديث") }
                        TextButton(onClick = { confirmDelete = p }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }

        item { Button(onClick = { vm.addingPlaylist = true }) { Text("إضافة قائمة جديدة") } }

        if (vm.loading) item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Accent, strokeWidth = 2.dp)
                Text(vm.loadMessage, color = TextDim)
            }
        }
        vm.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { SectionTitle("البيانات") }
        item { OutlinedButton(onClick = { vm.clearProgress() }) { Text("مسح سجل المتابعة") } }
        item { Text("مشغل M3U · الإصدار 1.0\nالتطبيق مشغل فقط ولا يتضمن أي محتوى أو قوائم تشغيل.", color = TextDim, fontSize = 12.sp) }
    }

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("حذف القائمة؟") },
            text = { Text("سيتم حذف «${p.name}» من التطبيق. (المفضلة المرتبطة بها ستبقى محفوظة)") },
            confirmButton = { TextButton(onClick = { vm.deletePlaylist(p.id); confirmDelete = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("إلغاء") } },
        )
    }
}
