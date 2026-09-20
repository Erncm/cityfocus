package com.cityfocus.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgPage = Color(0xFF17181C)
private val BgCard = Color(0xFF1F2024)
private val TextPrimary = Color(0xFFF2F2F0)
private val TextSecondary = Color(0xFF8A8A8E)
private val BorderNeutral = Color(0xFF3A3B40)
private val BgBomb = Color(0xFF2A1E1E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameState.load(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgPage) {
                    CityFocusScreen()
                }
            }
        }
    }
}

@Composable
fun CityFocusScreen() {
    val context = LocalContext.current
    val coins by GameState.coins.collectAsState()
    val buildings by GameState.buildings.collectAsState()
    val defenseCount by GameState.defenseCount.collectAsState()
    val isRunning by GameState.isRunning.collectAsState()
    val secondsLeft by GameState.secondsLeft.collectAsState()
    val bombFlash by GameState.bombFlash.collectAsState()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun startSession(minutes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_MINUTES, minutes)
        }
        context.startForegroundService(intent)
    }

    fun cancelSession() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$coins", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { GameState.buyDefense(context) },
                enabled = coins >= 20,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary),
                border = BorderStroke(1.dp, BorderNeutral)
            ) { Text("hava savunması · 20", fontSize = 12.sp) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("savunma: $defenseCount", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp))
                .background(if (bombFlash) BgBomb else BgCard)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 10.dp)) {
                val groundY = size.height - 4f
                drawRect(color = Color(0xFF2A2A2A), topLeft = Offset(0f, groundY), size = Size(size.width, 4f))
                val slotWidth = 100f
                var cursorX = 16f
                val count = buildings.coerceAtMost(6)
                for (i in 0 until count) {
                    when (i % 3) {
                        0 -> drawCornerShop(cursorX, groundY)
                        1 -> drawNeonTower(cursorX, groundY)
                        else -> drawBrickApartment(cursorX, groundY)
                    }
                    cursorX += slotWidth
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("binalar: $buildings", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(32.dp))

        if (isRunning) {
            val minutes = secondsLeft / 60
            val seconds = secondsLeft % 60
            Text("%02d:%02d".format(minutes, seconds), color = TextPrimary, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("uygulamayı kapatsan da sayaç devam eder", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cancelSession() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary),
                border = BorderStroke(1.dp, BorderNeutral)
            ) { Text("iptal et") }
        } else {
            Text("süre seç ve başla", color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                listOf(10, 25, 45, 60).forEach { minutes ->
                    Button(
                        onClick = { startSession(minutes) },
                        modifier = Modifier.padding(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderNeutral)
                    ) { Text("$minutes dk") }
                }
            }
        }
    }
}

private fun DrawScope.drawBrickApartment(originX: Float, groundY: Float) {
    val top = groundY - 130f
    fun ax(lx: Float) = originX + lx
    fun ay(ly: Float) = top + ly
    val cone = Path().apply {
        moveTo(ax(10f), ay(-55f)); lineTo(ax(90f), ay(-55f)); lineTo(ax(50f), ay(-75f)); close()
    }
    drawPath(cone, color = Color(0xFF4A2F17))
    drawRect(Color(0xFF8B5A2B), topLeft = Offset(ax(15f), ay(-55f)), size = Size(50f, 28f))
    listOf(20f, 28f, 52f, 60f).forEach { lx ->
        drawLine(Color(0xFF5A3A1E), Offset(ax(lx), ay(-27f)), Offset(ax(lx), ay(-4f)), strokeWidth = 2f)
    }
    drawLine(Color(0xFF333333), Offset(ax(50f), ay(-75f)), Offset(ax(50f), ay(-95f)), strokeWidth = 1f)
    drawCircle(Color(0xFFE23B3B), radius = 3f, center = Offset(ax(50f), ay(-97f)))
    drawRect(Color(0xFF3B2415), topLeft = Offset(ax(-6f), ay(-4f)), size = Size(102f, 6f))
    drawRect(Color(0xFFB8592E), topLeft = Offset(ax(0f), ay(0f)), size = Size(90f, 130f))
    listOf(14f, 42f, 70f, 98f).forEach { ly ->
        drawRect(Color(0xFF7A3B1E), topLeft = Offset(ax(0f), ay(ly)), size = Size(90f, 2f))
    }
    val windowRows = listOf(
        8f to listOf(Color(0xFF1B2A4A), Color(0xFFF4D35E), Color(0xFF1B2A4A)),
        48f to listOf(Color(0xFFF4D35E), Color(0xFF1B2A4A), Color(0xFFF4D35E)),
        78f to listOf(Color(0xFF1B2A4A), Color(0xFFF4D35E), Color(0xFF1B2A4A))
    )
    val cols = listOf(12f, 36f, 60f)
    windowRows.forEach { (ly, colors) ->
        cols.forEachIndexed { i, lx -> drawRect(colors[i], topLeft = Offset(ax(lx), ay(ly)), size = Size(14f, 16f)) }
    }
    drawRect(Color(0xFF4A2F17), topLeft = Offset(ax(35f), ay(100f)), size = Size(20f, 30f))
    drawLine(Color(0xFF1A1A1A), Offset(ax(82f), ay(0f)), Offset(ax(82f), ay(130f)), strokeWidth = 1f)
    listOf(28f, 58f, 88f).forEach { ly ->
        drawRect(Color(0xFF1A1A1A), topLeft = Offset(ax(78f), ay(ly)), size = Size(10f, 2f))
    }
}

private fun DrawScope.drawNeonTower(originX: Float, groundY: Float) {
    val top = groundY - 190f
    fun ax(lx: Float) = originX + lx
    fun ay(ly: Float) = top + ly
    drawRect(Color(0xFF555555), topLeft = Offset(ax(10f), ay(-8f)), size = Size(14f, 8f))
    drawRect(Color(0xFF555555), topLeft = Offset(ax(50f), ay(-8f)), size = Size(14f, 8f))
    drawLine(Color(0xFF444444), Offset(ax(20f), ay(-15f)), Offset(ax(20f), ay(0f)), strokeWidth = 2f)
    drawLine(Color(0xFF444444), Offset(ax(70f), ay(-15f)), Offset(ax(70f), ay(0f)), strokeWidth = 2f)
    drawRect(Color(0xFFFF3D81), topLeft = Offset(ax(15f), ay(-35f)), size = Size(60f, 20f), style = Stroke(width = 2f))
    listOf(15f to -35f, 45f to -35f, 75f to -35f, 15f to -15f, 45f to -15f, 75f to -15f).forEach { (lx, ly) ->
        drawCircle(Color(0xFFFFD23D), radius = 2f, center = Offset(ax(lx), ay(ly)))
    }
    drawRect(Color(0xFF2B2E3A), topLeft = Offset(ax(0f), ay(0f)), size = Size(80f, 190f))
    val rows = listOf(
        14f to listOf(Color(0xFFF4D35E), Color(0xFF1B2A4A), Color(0xFFF4D35E)),
        40f to listOf(Color(0xFF1B2A4A), Color(0xFFF4D35E), Color(0xFF1B2A4A)),
        66f to listOf(Color(0xFFF4D35E), Color(0xFFF4D35E), Color(0xFF1B2A4A)),
        92f to listOf(Color(0xFF1B2A4A), Color(0xFF1B2A4A), Color(0xFFF4D35E)),
        118f to listOf(Color(0xFFF4D35E), Color(0xFF1B2A4A), Color(0xFFF4D35E)),
        144f to listOf(Color(0xFF1B2A4A), Color(0xFFF4D35E), Color(0xFF1B2A4A))
    )
    val cols = listOf(12f, 36f, 60f)
    rows.forEach { (ly, colors) ->
        cols.forEachIndexed { i, lx -> drawRect(colors[i], topLeft = Offset(ax(lx), ay(ly)), size = Size(12f, 14f)) }
    }
    drawRect(Color(0xFF1B2A4A), topLeft = Offset(ax(25f), ay(170f)), size = Size(20f, 20f))
}

private fun DrawScope.drawCornerShop(originX: Float, groundY: Float) {
    val top = groundY - 70f
    fun ax(lx: Float) = originX + lx
    fun ay(ly: Float) = top + ly
    drawRect(Color(0xFF2B2E3A), topLeft = Offset(ax(18f), ay(-28f)), size = Size(60f, 12f))
    val stripeColors = listOf(
        Color(0xFF1D9E75), Color(0xFFF2E9D8), Color(0xFF1D9E75), Color(0xFFF2E9D8),
        Color(0xFF1D9E75), Color(0xFFF2E9D8), Color(0xFF1D9E75), Color(0xFFF2E9D8)
    )
    val stripeStarts = listOf(-2f, 8f, 18f, 28f, 38f, 48f, 58f, 68f)
    stripeStarts.forEachIndexed { i, lx -> drawRect(stripeColors[i], topLeft = Offset(ax(lx), ay(-14f)), size = Size(10f, 14f)) }
    listOf(-2f, 18f, 38f, 58f).forEach { lx ->
        val tri = Path().apply {
            moveTo(ax(lx), ay(0f)); lineTo(ax(lx + 5f), ay(0f)); lineTo(ax(lx + 2f), ay(8f)); close()
        }
        drawPath(tri, color = Color(0xFF1D9E75))
    }
    drawRect(Color(0xFFC9A227), topLeft = Offset(ax(0f), ay(0f)), size = Size(70f, 70f))
    drawRect(Color(0xFF1B2A4A), topLeft = Offset(ax(16f), ay(12f)), size = Size(16f, 16f))
    drawLine(Color(0xFFF2E9D8), Offset(ax(24f), ay(12f)), Offset(ax(24f), ay(28f)), strokeWidth = 1f)
    drawLine(Color(0xFFF2E9D8), Offset(ax(16f), ay(20f)), Offset(ax(32f), ay(20f)), strokeWidth = 1f)
    drawRect(Color(0xFF4A2F17), topLeft = Offset(ax(46f), ay(40f)), size = Size(20f, 30f))
}
