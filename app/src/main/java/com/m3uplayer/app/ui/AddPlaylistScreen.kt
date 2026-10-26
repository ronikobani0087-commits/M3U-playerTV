package com.m3uplayer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.m3uplayer.app.MainViewModel

@Composable
fun AddPlaylistScreen(vm: MainViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val canGoBack = vm.user.playlists.isNotEmpty()
    val submit = { if (!vm.loading && url.isNotBlank()) vm.addPlaylist(name, url) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (canGoBack) {
                IconButton(onClick = { vm.error = null; vm.addingPlaylist = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            } else Spacer(Modifier.width(48.dp))
            Text(
                "أضف قائمة التشغيل", Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "أدخل رابط M3U الخاص بك. سيتم تصنيف القنوات والأفلام والمسلسلات تلقائياً.",
            color = TextDim, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        Surface(
            color = CardBg, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(),
        ) {
            BoxWithConstraints(Modifier.padding(20.dp)) {
                val nameField: @Composable (Modifier) -> Unit = { m ->
                    Column(m) {
                        Text("اسم قائمة التشغيل (اختياري)")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name, onValueChange = { name = it }, enabled = !vm.loading,
                            placeholder = { Text("name") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                    }
                }
                val urlField: @Composable (Modifier) -> Unit = { m ->
                    Column(m) {
                        Text("أدخل رابط M3U")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = url, onValueChange = { url = it }, enabled = !vm.loading,
                            placeholder = { Text("http://server_domain:8080/get.php?...") },
                            singleLine = true,
                            textStyle = TextStyle(textDirection = TextDirection.Ltr),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )
                    }
                }
                if (maxWidth >= 640.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        urlField(Modifier.weight(1f))
                        nameField(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        nameField(Modifier)
                        urlField(Modifier)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = submit,
            enabled = !vm.loading && url.isNotBlank(),
            modifier = Modifier.widthIn(min = 260.dp).height(52.dp),
        ) { Text("أضف قائمة التشغيل") }

        Spacer(Modifier.height(16.dp))
        if (vm.loading) {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(8.dp))
            Text(vm.loadMessage, color = TextDim)
        }
        vm.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
