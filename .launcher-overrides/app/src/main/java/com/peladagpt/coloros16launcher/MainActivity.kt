package com.peladagpt.coloros16launcher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private data class HomeApp(val title: String, val packageName: String, val icon: Bitmap)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = 0x100 or 0x400 or 0x200
        setContent { ColorOSHome() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorOSHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("coloros_home", 0) }
    var apps by remember { mutableStateOf(emptyList<HomeApp>()) }
    var drawer by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var clock by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var grid by remember { mutableIntStateOf(prefs.getInt("grid", 4).coerceIn(4, 5)) }
    var iconScale by remember { mutableFloatStateOf(prefs.getFloat("icon_scale", 1f).coerceIn(.85f, 1.2f)) }
    var labels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var tone by remember { mutableIntStateOf(prefs.getInt("tone", 0)) }
    var editMode by remember { mutableStateOf(false) }

    fun refreshApps() {
        val pm = context.packageManager
        apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .mapNotNull { info -> runCatching {
                HomeApp(info.loadLabel(pm).toString(), info.activityInfo.packageName, drawableBitmap(info.loadIcon(pm)))
            }.getOrNull() }.distinctBy { it.packageName }.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }
    LaunchedEffect(Unit) {
        refreshApps()
        while (true) {
            val now = Date()
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            date = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }
    BackHandler(drawer || settings || editMode) {
        when { editMode -> editMode = false; drawer -> { drawer = false; query = "" }; settings -> settings = false }
    }

    val ink = if (dark) Color.White else Color(0xFF20242A)
    val muted = ink.copy(alpha = .62f)
    val palette = if (dark) listOf(Color(0xFF17191D), Color(0xFF282B31), Color(0xFF111317)) else when (tone) {
        1 -> listOf(Color(0xFFE7D8D0), Color(0xFFB9C9D4), Color(0xFFE8E5DE))
        2 -> listOf(Color(0xFFD8E6D9), Color(0xFFA8C8BD), Color(0xFFE9E5D8))
        else -> listOf(Color(0xFFB9CBD9), Color(0xFF879FB7), Color(0xFFE0D9D0))
    }
    val homeApps = remember(apps) { apps.filterNot { it.packageName == context.packageName } }
    val pageSize = grid * 4
    val pageCount = maxOf(1, (homeApps.size + pageSize - 1) / pageSize)
    val pager = rememberPagerState(pageCount = { pageCount })
    val filtered = remember(homeApps, query) { if (query.isBlank()) homeApps else homeApps.filter { it.title.contains(query, true) } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(palette)).pointerInput(drawer) {
        var dy = 0f
        detectVerticalDragGestures(onVerticalDrag = { _, amount -> dy += amount }, onDragEnd = {
            if (dy < -35f) drawer = true
            if (dy > 35f) drawer = false
            dy = 0f
        })
    }) {
        Box(Modifier.offset((-90).dp, 180.dp).size(270.dp).blur(75.dp).background(Color(0x5577A9C8), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(80.dp, (-100).dp).size(260.dp).blur(80.dp).background(Color(0x5589BBAA), CircleShape))

        AnimatedContent(settings, transitionSpec = {
            (fadeIn() + slideInVertically { it / 8 }) togetherWith (fadeOut() + slideOutVertically { -it / 8 })
        }, label = "screen") { showSettings ->
            if (showSettings) {
                SettingsScreen(dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                    grid, { grid = it; prefs.edit().putInt("grid", it).apply() },
                    iconScale, { iconScale = it; prefs.edit().putFloat("icon_scale", it).apply() },
                    labels, { labels = it; prefs.edit().putBoolean("labels", it).apply() },
                    tone, { tone = it; prefs.edit().putInt("tone", it).apply() }, { settings = false })
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(date, color = ink.copy(alpha = .78f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(clock, color = ink, fontSize = 60.sp, lineHeight = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(color = Color.White.copy(alpha = .30f), shape = RoundedCornerShape(22.dp)) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Cloud, null, tint = ink, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("—°", color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text("Clima", color = muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp, end = 5.dp))
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(Color.White.copy(alpha = .34f))
                        .clickable { drawer = true }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = muted)
                        Spacer(Modifier.width(10.dp))
                        Text("Pesquisar aplicativos", color = muted, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.Apps, null, tint = muted)
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalPager(state = pager, modifier = Modifier.weight(1f), key = { it }) { page ->
                        val pageApps = homeApps.drop(page * pageSize).take(pageSize)
                        LazyVerticalGrid(columns = GridCells.Fixed(grid), modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = false, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(19.dp), contentPadding = PaddingValues(top = 5.dp, bottom = 10.dp)) {
                            items(pageApps, key = { it.packageName }) { app ->
                                HomeIcon(app, iconScale, labels, ink, {
                                    runCatching { context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)) }
                                }, { editMode = true })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(pageCount) { index ->
                            Box(Modifier.padding(horizontal = 3.dp).size(if (pager.currentPage == index) 16.dp else 5.dp, 5.dp)
                                .clip(CircleShape).background(ink.copy(alpha = if (pager.currentPage == index) .8f else .28f)))
                        }
                    }
                    Surface(color = Color.White.copy(alpha = .32f), shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        val dock = homeApps.filter { app ->
                            val p = app.packageName.lowercase()
                            p.contains("dialer") || p.contains("messaging") || p.contains("chrome") || p.contains("camera")
                        }.take(4).ifEmpty { homeApps.take(4) }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            dock.forEach { app ->
                                Image(app.icon.asImageBitmap(), app.title, Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).clickable {
                                    runCatching { context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)) }
                                })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("ColorOS 16 • Independent", color = muted, fontSize = 10.sp)
                        Row {
                            Icon(Icons.Rounded.Widgets, null, tint = muted, modifier = Modifier.clickable { drawer = true })
                            Spacer(Modifier.width(18.dp))
                            Icon(Icons.Rounded.Settings, null, tint = muted, modifier = Modifier.clickable { settings = true })
                        }
                    }
                }
            }
        }

        AnimatedVisibility(drawer, enter = fadeIn() + slideInVertically { it / 3 } + scaleIn(initialScale = .96f),
            exit = fadeOut() + slideOutVertically { it / 4 } + scaleOut(targetScale = .98f), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().background(if (dark) Color(0xF21A1C20) else Color(0xF2F3F4F6))
                .statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Todos os aplicativos", color = if (dark) Color.White else Color(0xFF20242A), fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                        Text(filtered.size.toString() + " aplicativos", color = muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.Settings, null, tint = muted, modifier = Modifier.clickable { drawer = false; settings = true })
                }
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Pesquisar") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(22.dp))
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(grid), modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        HomeIcon(app, iconScale, true, if (dark) Color.White else Color(0xFF20242A), {
                            drawer = false; query = ""
                            runCatching { context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)) }
                        }, {})
                    }
                }
            }
        }
        if (editMode) Surface(color = Color(0xE92A3038), shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 35.dp).clickable { editMode = false }) {
            Text("Concluído", color = Color.White, modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeIcon(app: HomeApp, scale: Float, showLabel: Boolean, ink: Color, onClick: () -> Unit, onLongClick: () -> Unit) {
    val size by animateFloatAsState(scale, spring(stiffness = Spring.StiffnessMedium), label = "icon")
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(app.icon.asImageBitmap(), app.title, Modifier.size((54 * size).dp).clip(RoundedCornerShape(18.dp)))
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(app.title, color = ink, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsScreen(
    dark: Boolean, onDark: (Boolean) -> Unit, grid: Int, onGrid: (Int) -> Unit,
    iconScale: Float, onScale: (Float) -> Unit, labels: Boolean, onLabels: (Boolean) -> Unit,
    tone: Int, onTone: (Int) -> Unit, onBack: () -> Unit
) {
    val ink = if (dark) Color.White else Color(0xFF20242A)
    Column(Modifier.fillMaxSize().background(if (dark) Color(0xFF17191D) else Color(0xFFF3F4F6))
        .statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = ink, fontSize = 34.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text("Personalização", color = ink, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("TELA INICIAL", color = ink.copy(alpha = .55f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        SettingCard("Colunas da tela inicial", ink) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (4..5).forEach { n -> FilterChip(grid == n, { onGrid(n) }, label = { Text(n.toString()) }) }
            }
        }
        SettingCard("Tamanho dos ícones", ink) { Slider(iconScale, onScale, valueRange = .85f..1.2f) }
        SettingCard("Nomes dos aplicativos", ink) { Switch(labels, onLabels) }
        Spacer(Modifier.height(18.dp))
        Text("APARÊNCIA", color = ink.copy(alpha = .55f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        SettingCard("Modo escuro", ink) { Switch(dark, onDark) }
        SettingCard("Papel de parede", ink) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Color(0xFFB9CBD9), Color(0xFFE7D8D0), Color(0xFFD8E6D9)).forEachIndexed { index, color ->
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color).clickable { onTone(index) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("ColorOS 16 • Independent Edition", color = ink.copy(alpha = .5f), fontSize = 11.sp)
    }
}

@Composable
private fun SettingCard(title: String, ink: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp).clip(RoundedCornerShape(22.dp))
        .background(ink.copy(alpha = .055f)).padding(16.dp)) {
        Text(title, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

private fun drawableBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(192)
    val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(192)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return bitmap
}
