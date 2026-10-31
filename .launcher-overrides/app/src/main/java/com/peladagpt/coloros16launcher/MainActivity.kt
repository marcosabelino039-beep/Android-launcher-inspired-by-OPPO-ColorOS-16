package com.peladagpt.coloros16launcher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    var customFolders by remember { mutableStateOf<List<String>>(context.getSharedPreferences("launcher_prefs", 0).getStringSet("custom_folders", emptySet<String>())?.toList()?.sorted() ?: emptyList()) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var socialFolderName by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getString("folder_name_Social", "Social") ?: "Social") }
    var toolsFolderName by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getString("folder_name_Ferramentas", "Ferramentas") ?: "Ferramentas") }
    var settings by remember { mutableStateOf(false) }
    var gridColumns by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("grid_columns", 4)) }
    var dockCount by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("dock_count", 3).coerceIn(1, 5)) }
    var drawerColumns by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("drawer_columns", 4).coerceIn(3, 6)) }
    var homeIconSize by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("home_icon_size", 54).coerceIn(42, 72)) }
    var homeLabelSize by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("home_label_size", 10).coerceIn(8, 14)) }
    var folderOpacity by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("folder_opacity", 20).coerceIn(10, 40)) }
    var folderIconSize by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("folder_icon_size", 54).coerceIn(44, 72)) }
    var iconShape by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getString("icon_shape", "squircle") ?: "squircle") }
    var showHomeLabels by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getBoolean("show_home_labels", true)) }
    var animationSpeed by remember { mutableIntStateOf(context.getSharedPreferences("launcher_prefs", 0).getInt("animation_speed", 100).coerceIn(60, 150)) }
    var clock by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<LaunchApp?>(null) }
    var pinnedPackages by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getStringSet("pinned_apps", emptySet())?.toSet() ?: emptySet()) }
    var pinnedOrder by remember { mutableStateOf((context.getSharedPreferences("launcher_prefs", 0).getString("pinned_order", "")?.ifBlank { pinnedPackages.sorted().joinToString("|") } ?: pinnedPackages.sorted().joinToString("|")).split("|").filter { it.isNotBlank() }) }
    var wallpaperUri by remember { mutableStateOf(context.getSharedPreferences("launcher_prefs", 0).getString("wallpaper_uri", null)) }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            wallpaperUri = uri.toString()
            context.getSharedPreferences("launcher_prefs", 0).edit().putString("wallpaper_uri", wallpaperUri).apply()
        }
    }

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
            .sortedBy { app -> pinnedOrder.indexOf(app.packageName).let { if (it < 0) Int.MAX_VALUE else it } }
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
        if (wallpaperUri != null && !settings) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(android.net.Uri.parse(wallpaperUri)).crossfade(true).build(),
                contentDescription = "Papel de parede",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color(0x660B1426)))
        }
        // Soft ambient highlights behind the glass surfaces.
        Box(Modifier.offset(x = (-80).dp, y = 190.dp).size(260.dp).blur(75.dp).background(Color(0x665E9EFF), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = (-110).dp).size(250.dp).blur(80.dp).background(Color(0x5576D8C8), CircleShape))

        AnimatedContent(
            targetState = settings,
            transitionSpec = {
                (fadeIn(tween(220 * animationSpeed / 100)) + slideInVertically(tween(320 * animationSpeed / 100)) { if (targetState) it / 8 else -it / 10 }) togetherWith
                    (fadeOut(tween(160 * animationSpeed / 100)) + slideOutVertically(tween(260 * animationSpeed / 100)) { if (targetState) -it / 10 else it / 8 })
            },
            label = "main-screen"
        ) { showSettings ->
            if (showSettings) {
                SettingsPage(gridColumns = gridColumns, onGridColumnsChange = { columns -> gridColumns = columns; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("grid_columns", columns).apply() }, dockCount = dockCount, onDockCountChange = { count -> dockCount = count; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("dock_count", count).apply() }, drawerColumns = drawerColumns, onDrawerColumnsChange = { count -> drawerColumns = count; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("drawer_columns", count).apply() }, iconSize = homeIconSize, onIconSizeChange = { size -> homeIconSize = size; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("home_icon_size", size).apply() }, labelSize = homeLabelSize, onLabelSizeChange = { size -> homeLabelSize = size; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("home_label_size", size).apply() }, folderOpacity = folderOpacity, onFolderOpacityChange = { value -> folderOpacity = value; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("folder_opacity", value).apply() }, folderIconSize = folderIconSize, onFolderIconSizeChange = { value -> folderIconSize = value; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("folder_icon_size", value).apply() }, iconShape = iconShape, onIconShapeChange = { shape -> iconShape = shape; context.getSharedPreferences("launcher_prefs", 0).edit().putString("icon_shape", shape).apply() }, showLabels = showHomeLabels, onShowLabelsChange = { enabled -> showHomeLabels = enabled; context.getSharedPreferences("launcher_prefs", 0).edit().putBoolean("show_home_labels", enabled).apply() }, animationSpeed = animationSpeed, onAnimationSpeedChange = { speed -> animationSpeed = speed; context.getSharedPreferences("launcher_prefs", 0).edit().putInt("animation_speed", speed).apply() }, hasWallpaper = wallpaperUri != null, onChooseWallpaper = { wallpaperPicker.launch(arrayOf("image/*")) }, onRemoveWallpaper = { wallpaperUri = null; context.getSharedPreferences("launcher_prefs", 0).edit().remove("wallpaper_uri").apply() }, onBack = { settings = false })
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
                            BatteryTile(Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(25.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Aplicativos", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("＋ Pasta", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, modifier = Modifier.clickable { newFolderName = ""; showCreateFolder = true })
                            Spacer(Modifier.width(14.dp))
                            Text("Editar", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, modifier = Modifier.clickable { settings = true })
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(gridColumns), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(17.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(favorites) { app -> AppIcon(app, iconSize = homeIconSize.dp, iconShape = iconShape, showLabel = showHomeLabels, labelSize = homeLabelSize, onClick = { launch(app, context) }, onLongClick = { selectedApp = app }) }
                        item { FolderIcon(label = socialFolderName, iconShape = iconShape, folderSize = folderIconSize.dp, backgroundOpacity = folderOpacity / 100f, apps = apps.filter { app -> listOf("instagram", "facebook", "tiktok", "twitter", "discord", "reddit", "whatsapp", "pinterest", "snapchat").any { term -> app.label.contains(term, true) || app.packageName.contains(term, true) } }.take(4)) { folderTitle = "Social" } }
                        item { FolderIcon(label = toolsFolderName, iconShape = iconShape, folderSize = folderIconSize.dp, backgroundOpacity = folderOpacity / 100f, apps = apps.filter { app -> listOf("settings", "file", "manager", "clock", "calculator", "camera", "gallery", "music", "browser", "security", "notes").any { term -> app.label.contains(term, true) || app.packageName.contains(term, true) } }.take(4)) { folderTitle = "Ferramentas" } }
                        items(customFolders, key = { "custom_folder_" + it }) { name ->
                            val saved: Set<String> = context.getSharedPreferences("launcher_prefs", 0).getStringSet("folder_apps_" + name, emptySet<String>()) ?: emptySet()
                            FolderIcon(label = name, iconShape = iconShape, folderSize = folderIconSize.dp, backgroundOpacity = folderOpacity / 100f, apps = apps.filter { it.packageName in saved }.take(4)) { folderTitle = name }
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .15f)).padding(horizontal = 15.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        favorites.take(dockCount).forEach { app -> SmallAppIcon(app, iconShape) { launch(app, context) } }
                        Surface(color = Color.White.copy(alpha = .18f), shape = CircleShape, modifier = Modifier.size(48.dp).clickable { drawer = true }) {
                            Icon(Icons.Rounded.Apps, "Abrir aplicativos", tint = Color.White, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = drawer,
            enter = fadeIn(tween(180 * animationSpeed / 100)) + slideInVertically(tween(340 * animationSpeed / 100)) { it / 3 } + scaleIn(initialScale = .97f, animationSpec = spring(dampingRatio = .82f, stiffness = 360f)),
            exit = fadeOut(tween(150 * animationSpeed / 100)) + slideOutVertically(tween(260 * animationSpeed / 100)) { it / 4 } + scaleOut(targetScale = .985f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().background(Color(0xF2182235)).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Todos os aplicativos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Fechar", color = Color.White.copy(alpha = .75f), modifier = Modifier.clickable { drawer = false })
                }
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Pesquisar aplicativos") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(22.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White.copy(alpha = .6f), unfocusedBorderColor = Color.White.copy(alpha = .2f), focusedLeadingIconColor = Color.White, unfocusedLeadingIconColor = Color.White, focusedPlaceholderColor = Color.White.copy(alpha = .5f), unfocusedPlaceholderColor = Color.White.copy(alpha = .5f)))
                Spacer(Modifier.height(18.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(drawerColumns), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.packageName }) { app -> AppIcon(app, iconShape = iconShape, onClick = { launch(app, context); drawer = false }, onLongClick = { selectedApp = app }) }
                }
            }
        }
    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            title = { Text("Criar pasta") },
            text = { OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it.take(24) }, singleLine = true, label = { Text("Nome da pasta") }) },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFolderName.trim()
                    if (name.isNotEmpty() && customFolders.none { it.equals(name, ignoreCase = true) } && !name.equals("Social", true) && !name.equals("Ferramentas", true)) {
                        customFolders = customFolders + name
                        context.getSharedPreferences("launcher_prefs", 0).edit().putStringSet("custom_folders", customFolders.toSet()).putStringSet("folder_apps_" + name, emptySet()).apply()
                    }
                    showCreateFolder = false
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { showCreateFolder = false }) { Text("Cancelar") } }
        )
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
                    val nextOrder = if (isPinned) pinnedOrder.filterNot { it == app.packageName } else pinnedOrder.filterNot { it == app.packageName } + app.packageName
                    pinnedPackages = next
                    pinnedOrder = nextOrder
                    context.getSharedPreferences("launcher_prefs", 0).edit()
                        .putStringSet("pinned_apps", next)
                        .putString("pinned_order", nextOrder.joinToString("|")).apply()
                    selectedApp = null
                }) { Text(if (isPinned) "Remover da tela inicial" else "Adicionar") }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned && pinnedOrder.indexOf(app.packageName) > 0) {
                        TextButton(onClick = {
                            val order = pinnedOrder.toMutableList()
                            val index = order.indexOf(app.packageName)
                            val previous = order[index - 1]
                            order[index - 1] = order[index]
                            order[index] = previous
                            pinnedOrder = order
                            context.getSharedPreferences("launcher_prefs", 0).edit().putString("pinned_order", order.joinToString("|")).apply()
                            selectedApp = null
                        }) { Text("Mover para esquerda") }
                    }
                    TextButton(onClick = { selectedApp = null }) { Text("Fechar") }
                }
            }
        )
    }

    folderTitle?.let { title ->
        val prefs = context.getSharedPreferences("launcher_prefs", 0)
        val prefKey = "folder_apps_" + title
        val defaultPackages = remember(apps, title) {
            val terms = if (title == "Social") listOf("instagram", "facebook", "tiktok", "twitter", "discord", "reddit", "telegram", "whatsapp", "pinterest", "snapchat")
                        else if (title == "Ferramentas") listOf("settings", "file", "manager", "clock", "calculator", "camera", "gallery", "music", "browser", "security", "weather", "notes")
                        else emptyList()
            apps.filter { app -> terms.any { term -> app.label.contains(term, true) || app.packageName.contains(term, true) } }
                .map { it.packageName }.toSet()
        }
        var selectedPackages by remember(title, apps) {
            mutableStateOf(prefs.getStringSet(prefKey, null)?.toSet() ?: defaultPackages)
        }
        val folderApps = apps.filter { it.packageName in selectedPackages }
        FolderDialog(
            title = title,
            displayName = when (title) { "Social" -> socialFolderName; "Ferramentas" -> toolsFolderName; else -> title },
            iconShape = iconShape,
            apps = folderApps,
            allApps = apps,
            selectedPackages = selectedPackages,
            onToggleApp = { app ->
                val next = if (app.packageName in selectedPackages) selectedPackages - app.packageName
                           else selectedPackages + app.packageName
                selectedPackages = next
                prefs.edit().putStringSet(prefKey, next).apply()
            },
            onLaunch = { app -> launch(app, context) },
            onRename = { newName ->
                if (title == "Social") { socialFolderName = newName; prefs.edit().putString("folder_name_Social", newName).apply() }
                else if (title == "Ferramentas") { toolsFolderName = newName; prefs.edit().putString("folder_name_Ferramentas", newName).apply() }
                else if (title in customFolders && newName.isNotBlank() && customFolders.none { it.equals(newName, ignoreCase = true) }) {
                    customFolders = customFolders.map { if (it == title) newName else it }
                    prefs.edit().putStringSet("custom_folders", customFolders.toSet())
                        .putStringSet("folder_apps_" + newName, selectedPackages)
                        .remove(prefKey).apply()
                    folderTitle = null
                }
            },
            canDelete = title in customFolders,
            onDelete = {
                customFolders = customFolders.filterNot { it == title }
                prefs.edit().putStringSet("custom_folders", customFolders.toSet()).remove(prefKey).apply()
                folderTitle = null
            },
            onDismiss = { folderTitle = null }
        )
    }
}

@Composable
private fun BatteryTile(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var percent by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val status = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = status?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = status?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
            percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val state = status?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            charging = state == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                state == android.os.BatteryManager.BATTERY_STATUS_FULL
            delay(30_000)
        }
    }
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .10f)).padding(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bateria", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(if (charging) "Carregando" else "Dispositivo", color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = .18f))) {
                Box(Modifier.fillMaxWidth(if (percent >= 0) percent / 100f else 0f).fillMaxHeight().clip(CircleShape).background(Color(0xFF9DE6C1)))
            }
            Spacer(Modifier.width(8.dp))
            Text(if (percent >= 0) "$percent%" else "--", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(app: LaunchApp, iconSize: androidx.compose.ui.unit.Dp = 54.dp, iconShape: String = "squircle", showLabel: Boolean = true, onClick: () -> Unit, onLongClick: () -> Unit = {}, labelSize: Int = 10) {
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
        AppArtwork(app.icon, Modifier.size(iconSize), iconShape)
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(app.label, color = Color.White, fontSize = labelSize.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SmallAppIcon(app: LaunchApp, iconShape: String = "squircle", onClick: () -> Unit) {
    AppArtwork(app.icon, Modifier.size(42.dp).clickable { onClick() }, iconShape)
}

@Composable
private fun FolderIcon(label: String = "Social", iconShape: String = "squircle", folderSize: androidx.compose.ui.unit.Dp = 54.dp, backgroundOpacity: Float = .20f, apps: List<LaunchApp> = emptyList(), onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier.size(folderSize).clip(RoundedCornerShape(folderSize * .31f))
                .background(Color.White.copy(alpha = backgroundOpacity)).padding(folderSize * .13f),
            contentAlignment = Alignment.Center
        ) {
            if (apps.isEmpty()) {
                Box(Modifier.size(folderSize * .63f).clip(RoundedCornerShape(folderSize * .19f)).background(Color.White.copy(alpha = backgroundOpacity)))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    apps.chunked(2).forEach { rowApps ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            rowApps.forEach { app -> AppArtwork(app.icon, Modifier.size(folderSize * .315f), iconShape) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun AppArtwork(drawable: Drawable, modifier: Modifier = Modifier, iconShape: String = "squircle") {
    val bitmap = remember(drawable) {
        val bmp = Bitmap.createBitmap( drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }
    val shape = when (iconShape) { "circle" -> CircleShape; "rounded" -> RoundedCornerShape(18.dp); else -> RoundedCornerShape(28) }
    Image(bitmap, null, modifier.clip(shape))
}

private fun launch(app: LaunchApp, context: android.content.Context) {
    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
}

@Composable
private fun SettingsPage(gridColumns: Int, onGridColumnsChange: (Int) -> Unit, dockCount: Int, onDockCountChange: (Int) -> Unit, drawerColumns: Int, onDrawerColumnsChange: (Int) -> Unit, iconSize: Int, onIconSizeChange: (Int) -> Unit, labelSize: Int, onLabelSizeChange: (Int) -> Unit, folderOpacity: Int, onFolderOpacityChange: (Int) -> Unit, folderIconSize: Int, onFolderIconSizeChange: (Int) -> Unit, iconShape: String, onIconShapeChange: (String) -> Unit, showLabels: Boolean, onShowLabelsChange: (Boolean) -> Unit, animationSpeed: Int, onAnimationSpeedChange: (Int) -> Unit, hasWallpaper: Boolean, onChooseWallpaper: () -> Unit, onRemoveWallpaper: () -> Unit, onBack: () -> Unit) {
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
        Spacer(Modifier.height(18.dp))
        Text("Colunas da gaveta", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$drawerColumns colunas", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(value = drawerColumns.toFloat(), onValueChange = { onDrawerColumnsChange(it.roundToInt().coerceIn(3, 6)) }, valueRange = 3f..6f, steps = 2)
        Spacer(Modifier.height(18.dp))
        Text("Ícones no dock", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$dockCount aplicativos", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(value = dockCount.toFloat(), onValueChange = { onDockCountChange(it.roundToInt().coerceIn(1, 5)) }, valueRange = 1f..5f, steps = 3)
        Spacer(Modifier.height(18.dp))
        Text("Tamanho dos ícones", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$iconSize dp", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(
            value = iconSize.toFloat(),
            onValueChange = { onIconSizeChange(it.roundToInt().coerceIn(42, 72)) },
            valueRange = 42f..72f,
            steps = 5
        )
        Spacer(Modifier.height(18.dp))
        Text("Tamanho dos nomes", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$labelSize sp", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(value = labelSize.toFloat(), onValueChange = { onLabelSizeChange(it.roundToInt().coerceIn(8, 14)) }, valueRange = 8f..14f, steps = 5)
        Spacer(Modifier.height(18.dp))
        Text("Transparência das pastas", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$folderOpacity%", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(value = folderOpacity.toFloat(), onValueChange = { onFolderOpacityChange(it.roundToInt().coerceIn(10, 40)) }, valueRange = 10f..40f, steps = 5)
        Spacer(Modifier.height(18.dp))
        Text("Tamanho dos ícones das pastas", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("$folderIconSize dp", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(value = folderIconSize.toFloat(), onValueChange = { onFolderIconSizeChange(it.roundToInt().coerceIn(44, 72)) }, valueRange = 44f..72f, steps = 6)
        Spacer(Modifier.height(18.dp))
        Text("Formato dos ícones", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("squircle" to "ColorOS", "rounded" to "Arredondado", "circle" to "Circular").forEach { (shape, label) ->
                Surface(color = if (iconShape == shape) Color(0xFF8EDBC4).copy(alpha = .28f) else Color.White.copy(alpha = .09f), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).clickable { onIconShapeChange(shape) }) {
                    Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Velocidade das animações", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("${animationSpeed}%", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
        Slider(value = animationSpeed.toFloat(), onValueChange = { onAnimationSpeedChange(it.roundToInt().coerceIn(60, 150)) }, valueRange = 60f..150f, steps = 8)
        Spacer(Modifier.height(12.dp))
        Text("Papel de parede", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onChooseWallpaper, modifier = Modifier.weight(1f)) {
                Text(if (hasWallpaper) "Trocar imagem" else "Escolher imagem")
            }
            if (hasWallpaper) {
                TextButton(onClick = onRemoveWallpaper) { Text("Remover") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .09f)).clickable { onShowLabelsChange(!showLabels) }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nomes dos aplicativos", color = Color.White, fontSize = 14.sp)
                Text("Mostrar nomes abaixo dos ícones", color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
            }
            Switch(checked = showLabels, onCheckedChange = onShowLabelsChange)
        }
        Spacer(Modifier.height(12.dp))
        Text("As opções acima são aplicadas à tela inicial e ficam salvas neste dispositivo.", color = Color.White.copy(alpha = .5f), fontSize = 11.sp)
    }
}

@Composable
private fun FolderDialog(
    title: String,
    displayName: String,
    apps: List<LaunchApp>,
    allApps: List<LaunchApp>,
    selectedPackages: Set<String>,
    onToggleApp: (LaunchApp) -> Unit,
    onLaunch: (LaunchApp) -> Unit,
    onRename: (String) -> Unit,
    iconShape: String,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var folderName by remember(displayName) { mutableStateOf(displayName) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = .82f, stiffness = 380f)),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xF226344B),
            tonalElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(folderName, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (editing) "Selecione os aplicativos da pasta" else "${apps.size} aplicativos",
                            color = Color.White.copy(alpha = .62f),
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { renaming = !renaming }) { Text(if (renaming) "Voltar" else "Nome", color = Color.White) }
                    TextButton(onClick = { editing = !editing }) {
                        Text(if (editing) "Concluir" else "Editar", color = Color.White)
                    }
                    if (canDelete) TextButton(onClick = onDelete) { Text("Excluir", color = Color(0xFFFFA0A0)) }
                }
                if (renaming) {
                    OutlinedTextField(value = folderName, onValueChange = { folderName = it.take(24) }, singleLine = true, label = { Text("Nome da pasta") })
                    TextButton(onClick = { val clean = folderName.trim(); if (clean.isNotEmpty()) onRename(clean); renaming = false }) { Text("Salvar nome", color = Color.White) }
                }
                Spacer(Modifier.height(14.dp))
                if (editing) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 430.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allApps, key = { it.packageName }) { app ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleApp(app) }.padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = app.packageName in selectedPackages,
                                    onCheckedChange = { onToggleApp(app) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF8EDBC4),
                                        uncheckedColor = Color.White.copy(alpha = .65f),
                                        checkmarkColor = Color(0xFF17233D)
                                    )
                                )
                                Text(
                                    app.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else if (apps.isEmpty()) {
                    Text("Esta pasta está vazia. Toque em Editar para adicionar aplicativos.", color = Color.White.copy(alpha = .75f))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(17.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            AppIcon(app, iconShape = iconShape, onClick = { onLaunch(app); onDismiss() })
                        }
                    }
                }
            }
        }
    }
}
