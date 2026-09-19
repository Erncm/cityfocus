package com.cityfocus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF10151F)) {
                    CityFocusScreen()
                }
            }
        }
    }
}

data class SessionState(
    val buildings: Int = 0,
    val coins: Int = 0,
    val defenseCount: Int = 0,
    val isRunning: Boolean = false,
    val secondsLeft: Int = 0,
    val bombFlash: Boolean = false
)

@Composable
fun CityFocusScreen() {
    var state by remember { mutableStateOf(SessionState()) }
    val scope = rememberCoroutineScope()
    var timerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun completeSuccess() {
        state = state.copy(isRunning = false, buildings = state.buildings + 1, coins = state.coins + 10)
    }

    fun triggerBomb() {
        timerJob?.cancel()
        state = if (state.defenseCount > 0) {
            state.copy(defenseCount = state.defenseCount - 1, bombFlash = true, isRunning = false)
        } else {
            state.copy(buildings = (state.buildings - 1).coerceAtLeast(0), bombFlash = true, isRunning = false)
        }
    }

    fun startSession(minutes: Int) {
        val total = minutes * 60
        state = state.copy(isRunning = true, secondsLeft = total, bombFlash = false)
        timerJob = scope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                state = state.copy(secondsLeft = remaining)
            }
            completeSuccess()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.isRunning) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && state.isRunning) {
                triggerBomb()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🪙 ${state.coins}", color = Color.White, fontSize = 18.sp)
            Button(
                onClick = {
                    if (state.coins >= 20) {
                        state = state.copy(coins = state.coins - 20, defenseCount = state.defenseCount + 1)
                    }
                },
                enabled = state.coins >= 20
            ) { Text("🛡 Hava Savunması (20)") }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Hava Savunması: ${state.defenseCount}", color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
                .background(if (state.bombFlash) Color(0xFF5A1A1A) else Color(0xFF1B2436)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                repeat(state.buildings.coerceAtMost(12)) { index ->
                    val height = (60 + (index % 5) * 20).dp
                    Box(
                        modifier = Modifier.padding(horizontal = 3.dp).width(24.dp).height(height)
                            .background(Color(0xFF4D7CFE), RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Binalar: ${state.buildings}", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        if (state.isRunning) {
            val minutes = state.secondsLeft / 60
            val seconds = state.secondsLeft % 60
            Text("%02d:%02d".format(minutes, seconds), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { triggerBomb() }) { Text("İptal Et") }
        } else {
            Text("Süre seç ve başla", color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                listOf(10, 25, 45, 60).forEach { minutes ->
                    Button(onClick = { startSession(minutes) }, modifier = Modifier.padding(4.dp)) {
                        Text("$minutes dk")
                    }
                }
            }
        }
    }
}
