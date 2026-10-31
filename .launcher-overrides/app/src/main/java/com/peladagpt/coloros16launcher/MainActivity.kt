package com.peladagpt.coloros16launcher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class LaunchApp(val label: String, val packageName: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { ColorOSLauncher() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorOSLauncher() {
    val context = LocalContext.current
    val pm = context.packageManager
    var apps by remember { mutableStateOf(emptyList<LaunchApp>()) }
    var drawer by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var folderTitle by remember { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(false) }
    var gridColumns by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("grid_columns", 4)) }
    var clock by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<LaunchApp?>(null) }
    var pinnedPackages by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getStringSet("pinned_apps", emptySet())?.toSet() ?: emptySet()) }

    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = pm.queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) null
                else LaunchApp(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm))
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    val favorites = remember(apps, pinnedPackages) {
        val chosen = apps.filter { it.packageName in pinnedPackages }
        if (chosen.isNotEmpty()) chosen else apps.take(4)
    }
    val filtered = remember(apps, search) {
        if (search.isBlank()) apps else apps.filter { it.label.contains(search, true) }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF17233D), Color(0xFF354B70), Color(0xFF111827))))
            .pointerInput(drawer) {
                var total = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount -> total += amount },
                    onDragEnd = { if (total < -35f) drawer = true; if (total > 35f) drawer = false; total = 0f }
                )
            }
    ) {
        // Soft ambient highlights behind the glass surfaces.
        Box(Modifier.offset(x = (-80).dp, y = 190.dp).size(260.dp).blur(75.dp).background(Color(0x665E9EFF), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = (-110).dp).size(250.dp).blur(80.dp).background(Color(0x5576D8C8), CircleShape))

        AnimatedContent(targetState = settings, label = "main-screen") { showSettings ->
            if (showSettings) {
                SettingsPage(gridColumns = gridColumns, onGridColumnsChange = { columns -> gridColumns = columns; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("grid_columns", columns).apply() }, onBack = { settings = false })
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("ColorOS", color = Color.White.copy(alpha = .78f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("16", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = Color.White.copy(alpha = .13f), shape = CircleShape, modifier = Modifier.size(42.dp).clickable { settings = true }) {
                            Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                    }

                    Spacer(Modifier.height(26.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = .12f)).blur(0.dp).padding(22.dp)) {
                        Text(clock, color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        Text(SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() }, color = Color.White.copy(alpha = .78f), fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Widgets, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Espaço inteligente", color = Color.White.copy(alpha = .9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("Widgets", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WidgetTile("Clima", "Adicione uma cidade", Modifier.weight(1f))
                            WidgetTile("Bateria", "Nível do dispositivo", Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(25.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Aplicativos", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Editar", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, modifier = Modifier.clickable { settings = true })
                    }
                    Spacer(Modifier.height(15.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(gridColumns), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(17.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(favorites) { app -> AppIcon(app, onClick = { launch(app, context) }, onLongClick = { selectedApp = app }) }
                        item { FolderIcon { folderTitle = "Social" } }
                        item { FolderIcon(label = "Ferramentas") { folderTitle = "Ferramentas" } }
                    }

                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .15f)).padding(horizontal = 15.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        favorites.take(3).forEach { app -> SmallAppIcon(app) { launch(app, context) } }
                        Surface(color = Color.White.copy(alpha = .18f), shape = CircleShape, modifier = Modifier.size(48.dp).clickable { drawer = true }) {
                            Icon(Icons.Rounded.Apps, "Abrir aplicativos", tint = Color.White, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = drawer, enter = fadeIn() + scaleIn(initialScale = .96f), exit = fadeOut() + scaleOut(targetScale = .98f), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().background(Color(0xF2182235)).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Todos os aplicativos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Fechar", color = Color.White.copy(alpha = .75f), modifier = Modifier.clickable { drawer = false })
                }
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Pesquisar aplicativos") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(22.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White.copy(alpha = .6f), unfocusedBorderColor = Color.White.copy(alpha = .2f), focusedLeadingIconColor = Color.White, unfocusedLeadingIconColor = Color.White, focusedPlaceholderColor = Color.White.copy(alpha = .5f), unfocusedPlaceholderColor = Color.White.copy(alpha = .5f)))
                Spacer(Modifier.height(18.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.packageName }) { app -> AppIcon(app, onClick = { launch(app, context); drawer = false }, onLongClick = { selectedApp = app }) }
                }
            }
        }
    }

    selectedApp?.let { app ->
        val isPinned = app.packageName in pinnedPackages
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text(app.label) },
            text = { Text(if (isPinned) "Este aplicativo está na tela inicial." else "Adicionar este aplicativo à tela inicial?") },
            confirmButton = {
                TextButton(onClick = {
                    val next = if (isPinned) pinnedPackages - app.packageName else pinnedPackages + app.packageName
                    pinnedPackages = next
                    context.getSharedPreferences("launcher_prefs", 0).edit().putStringSet("pinned_apps", next).apply()
                    selectedApp = null
                }) { Text(if (isPinned) "Remover da tela inicial" else "Adicionar") }
            },
            dismissButton = { TextButton(onClick = { selectedApp = null }) { Text("Cancelar") } }
        )
    }

    folderTitle?.let { title ->
        val folderApps = remember(apps, title) {
            val terms = if (title == "Social") listOf("instagram", "facebook", "tiktok", "twitter", "discord", "reddit", "telegram", "whatsapp", "pinterest", "snapchat")
                        else listOf("settings", "file", "manager", "clock", "calculator", "camera", "gallery", "music", "browser", "security", "weather", "notes")
            apps.filter { app -> terms.any { term -> app.label.contains(term, true) || app.packageName.contains(term, true) } }
                .ifEmpty { apps.take(12) }
        }
        FolderDialog(title = title, apps = folderApps, onLaunch = { app -> launch(app, context) }, onDismiss = { folderTitle = null })
    }
}

@Composable
private fun WidgetTile(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .10f)).padding(13.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Color.White.copy(alpha = .62f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppIcon(app: LaunchApp, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .86f else 1f, spring(dampingRatio = .42f, stiffness = 420f), label = "icon-press")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().scale(scale).combinedClickable(onClick = {
        scope.launch {
            pressed = true
            delay(85)
            pressed = false
            onClick()
        }
    }, onLongClick = onLongClick)) {
        AppArtwork(app.icon, Modifier.size(54.dp))
        Spacer(Modifier.height(6.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SmallAppIcon(app: LaunchApp, onClick: () -> Unit) {
    AppArtwork(app.icon, Modifier.size(42.dp).clickable { onClick() })
}

@Composable
private fun FolderIcon(label: String = "Social", onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = .20f)).padding(7.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF80B8FF)))
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFFFA78C)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFB9A0FF)))
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF8EDBC4)))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun AppArtwork(drawable: Drawable, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        val bmp = Bitmap.createBitmap( drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }
    Image(bitmap, null, modifier.clip(RoundedCornerShape(15.dp)))
}

private fun launch(app: LaunchApp, context: android.content.Context) {
    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
}

@Composable
private fun SettingsPage(gridColumns: Int, onGridColumnsChange: (Int) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF17233D)).statusBarsPadding().navigationBarsPadding().padding(22.dp)) {
        Text("‹  Voltar", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onBack() })
        Spacer(Modifier.height(26.dp))
        Text("Personalização", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Ajuste a experiência da tela inicial", color = Color.White.copy(alpha = .65f), fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Text("Grade da tela inicial", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$gridColumns colunas", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(
            value = gridColumns.toFloat(),
            onValueChange = { onGridColumnsChange(it.roundToInt().coerceIn(3, 6)) },
            valueRange = 3f..6f,
            steps = 2
        )
        Spacer(Modifier.height(12.dp))
        listOf("Formato dos ícones" to "Arredondado", "Tamanho dos ícones" to "Padrão", "Animações" to "Fluidas", "Efeito de profundidade" to "Ativado", "Desfoque e transparência" to "Vidro fosco", "Gestos" to "Deslizar para abrir a gaveta", "Widgets" to "Espaço inteligente").forEach { (title, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .09f)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Text(value, color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("A quantidade de colunas é aplicada imediatamente e salva neste dispositivo. As demais opções ainda são prévias visuais.", color = Color.White.copy(alpha = .5f), fontSize = 11.sp)
    }
}

@Composable
private fun FolderDialog(
    title: String,
    apps: List<LaunchApp>,
    onLaunch: (LaunchApp) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xF226344B),
            tonalElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(apps.size.toString() + " aplicativos", color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Fechar", color = Color.White) }
                }
                Spacer(Modifier.height(14.dp))
                if (apps.isEmpty()) {
                    Text("Nenhum aplicativo encontrado nesta pasta.", color = Color.White.copy(alpha = .75f))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(17.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            AppIcon(app) { onLaunch(app); onDismiss() }
                        }
                    }
                }
            }
        }
    }
}
