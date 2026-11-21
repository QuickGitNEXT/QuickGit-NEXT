package com.quickgit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickgit.app.data.RepoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Edit GitHub wiki pages (owner/repo.wiki.git) on Chromebook / large tablet layouts.
 */
@Composable
fun WikiScreen(
    repoPath: String,
    repoManager: RepoManager,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var wikiDir by remember { mutableStateOf<File?>(null) }
    var pages by remember { mutableStateOf(emptyList<RepoManager.WikiPage>()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var content by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reloadPages(dir: File) {
        pages = repoManager.listWikiPages(dir)
        if (selected == null || pages.none { it.fileName == selected }) {
            selected = pages.firstOrNull()?.fileName
        }
        selected?.let { name ->
            content = repoManager.readWikiPage(dir, name).getOrDefault("")
        }
    }

    fun loadWiki() {
        scope.launch {
            loading = true
            error = null
            val r = withContext(Dispatchers.IO) { repoManager.ensureWikiRepo(repoPath) }
            loading = false
            r.fold(
                onSuccess = { dir ->
                    wikiDir = dir
                    reloadPages(dir)
                },
                onFailure = {
                    wikiDir = null
                    pages = emptyList()
                    error = it.message ?: "Could not open wiki"
                }
            )
        }
    }

    LaunchedEffect(repoPath) { loadWiki() }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Wiki",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { loadWiki() }, enabled = !loading && !saving) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = { showNew = true }, enabled = wikiDir != null && !saving) {
                Icon(Icons.Default.Add, contentDescription = "New page")
            }
            IconButton(
                onClick = {
                    val dir = wikiDir ?: return@IconButton
                    val name = selected ?: return@IconButton
                    scope.launch {
                        saving = true
                        val r = withContext(Dispatchers.IO) {
                            repoManager.saveWikiPage(dir, name, content)
                        }
                        saving = false
                        r.fold(
                            {
                                onMessage("Wiki page saved")
                                reloadPages(dir)
                            },
                            { onMessage("Save failed: ${it.message}") }
                        )
                    }
                },
                enabled = wikiDir != null && selected != null && !saving
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Row(Modifier.fillMaxSize().padding(8.dp)) {
                    Surface(
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .widthIn(min = 120.dp, max = 240.dp)
                            .weight(0.34f)
                            .fillMaxHeight()
                    ) {
                        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                            items(pages, key = { it.fileName }) { page ->
                                val sel = page.fileName == selected
                                Text(
                                    page.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (sel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                        )
                                        .clickable {
                                            selected = page.fileName
                                            wikiDir?.let { dir ->
                                                content = repoManager.readWikiPage(dir, page.fileName).getOrDefault("")
                                            }
                                        }
                                        .padding(10.dp),
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (pages.isEmpty()) {
                                item {
                                    Text(
                                        "No pages. Tap + to create.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        enabled = selected != null && !saving
                    )
                }
            }
        }
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New wiki page") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dir = wikiDir ?: return@Button
                        val title = newTitle.trim()
                        if (title.isBlank()) return@Button
                        showNew = false
                        scope.launch {
                            saving = true
                            val r = withContext(Dispatchers.IO) { repoManager.createWikiPage(dir, title) }
                            saving = false
                            r.fold(
                                { name ->
                                    onMessage("Created $name")
                                    reloadPages(dir)
                                    selected = name
                                    content = repoManager.readWikiPage(dir, name).getOrDefault("")
                                    newTitle = ""
                                },
                                { onMessage("Create failed: ${it.message}") }
                            )
                        }
                    },
                    enabled = newTitle.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNew = false }) { Text("Cancel") }
            }
        )
    }
}
