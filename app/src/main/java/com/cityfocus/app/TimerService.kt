package com.cityfocus.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

object GameState {
    val coins = MutableStateFlow(0)
    val buildings = MutableStateFlow(0)
    val defenseCount = MutableStateFlow(0)
    val bombFlash = MutableStateFlow(false)
    val isRunning = MutableStateFlow(false)
    val secondsLeft = MutableStateFlow(0)
    var totalSeconds = 0

    private const val PREFS = "cityfocus_prefs"

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        coins.value = p.getInt("coins", 0)
        buildings.value = p.getInt("buildings", 0)
        defenseCount.value = p.getInt("defense", 0)
    }

    private fun save(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putInt("coins", coins.value)
            .putInt("buildings", buildings.value)
            .putInt("defense", defenseCount.value)
            .apply()
    }

    fun completeSuccess(context: Context) {
        buildings.value += 1
        coins.value += 10
        isRunning.value = false
        save(context)
    }

    fun buyDefense(context: Context) {
        if (coins.value >= 20) {
            coins.value -= 20
            defenseCount.value += 1
            save(context)
        }
    }

    fun triggerBomb(context: Context) {
        if (defenseCount.value > 0) {
            defenseCount.value -= 1
        } else {
            buildings.value = (buildings.value - 1).coerceAtLeast(0)
        }
        bombFlash.value = true
        isRunning.value = false
        save(context)
    }
}

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIF_ID = 1
        const val ACTION_START = "com.cityfocus.app.START"
        const val ACTION_CANCEL = "com.cityfocus.app.CANCEL"
        const val EXTRA_MINUTES = "minutes"
    }

    override fun onCreate() {
        super.onCreate()
        GameState.load(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCountdown(intent.getIntExtra(EXTRA_MINUTES, 25))
            ACTION_CANCEL -> {
                GameState.triggerBomb(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(minutes: Int) {
        job?.cancel()
        val total = minutes * 60
        GameState.totalSeconds = total
        GameState.secondsLeft.value = total
        GameState.isRunning.value = true
        GameState.bombFlash.value = false

        startForeground(NOTIF_ID, buildNotification(total, total))

        job = serviceScope.launch {
            var remaining = total
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining -= 1
                GameState.secondsLeft.value = remaining
                updateNotification()
            }
            if (remaining <= 0) {
                GameState.completeSuccess(this@TimerService)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(GameState.secondsLeft.value, GameState.totalSeconds))
    }

    private fun buildNotification(remaining: Int, total: Int): Notification {
        val minutes = remaining / 60
        val seconds = remaining % 60
        val timeText = "%02d:%02d".format(minutes, seconds)

        val cancelIntent = Intent(this, TimerService::class.java).apply { action = ACTION_CANCEL }
        val cancelPending = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CityFocus çalışıyor")
            .setContentText("Kalan süre: $timeText")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPending)
            .setProgress(total, total - remaining, false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "İptal et", cancelPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Odaklanma Sayacı", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
