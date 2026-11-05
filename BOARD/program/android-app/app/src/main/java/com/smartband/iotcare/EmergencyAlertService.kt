package com.smartband.iotcare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.concurrent.Executors

class EmergencyAlertService : Service() {
    companion object {
        const val ACTION_STOP_ALARM = "com.smartband.iotcare.STOP_ALARM"
        const val ACTION_UPDATE_BACKEND = "com.smartband.iotcare.UPDATE_BACKEND"
        const val EXTRA_BACKEND_URL = "backend_url"
        private const val STATUS_CHANNEL = "smartband_status"
        private const val ALERT_CHANNEL = "smartband_emergency_v2"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val preferences by lazy { getSharedPreferences("smartband_alerts", MODE_PRIVATE) }
    private var alarmPlayer: MediaPlayer? = null
    private var polling = false
    private val poll = object : Runnable {
        override fun run() {
            if (!polling) return
            worker.execute { checkAlerts() }
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(STATUS_CHANNEL, "Kết nối Smartband", NotificationManager.IMPORTANCE_LOW))
        notifications.createNotificationChannel(NotificationChannel(ALERT_CHANNEL, "SOS và cảnh báo ngã", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)
        })
        startForeground(1, NotificationCompat.Builder(this, STATUS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tận Tâm đang theo dõi Smartband")
            .setContentText("Cảnh báo SOS và ngã đang bật")
            .setContentIntent(openAppIntent())
            .setOngoing(true).build())
        polling = true
        handler.post(poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> stopAlarm()
            ACTION_UPDATE_BACKEND -> {
                val url = intent.getStringExtra(EXTRA_BACKEND_URL)?.trimEnd('/')
                if (!url.isNullOrBlank()) preferences.edit().putString("backend_url", url).apply()
            }
        }
        return START_STICKY
    }

    private fun checkAlerts() {
        val backend = preferences.getString("backend_url", "http://192.168.100.247:8787") ?: return
        if (!backend.startsWith("http://") && !backend.startsWith("https://")) return
        val connection = try { URL("$backend/api/v1/alerts").openConnection() as HttpURLConnection } catch (_: Exception) { return }
        try {
            connection.connectTimeout = 1200
            connection.readTimeout = 1200
            if (connection.responseCode != 200) return
            val alerts = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("alerts") ?: return
            if (alerts.length() == 0) return
            val alert = alerts.optJSONObject(alerts.length() - 1) ?: return
            if (alert.optString("device_id") != "smartband-001") return
            val kind = alert.optString("alert_type")
            if (kind != "SOS" && kind != "FALL") return
            val id = "${alert.optString("device_id")}:$kind:${alert.optLong("alert_event_ms")}:${alert.optString("received_at")}"
            if (id == preferences.getString("last_alert", null)) return
            preferences.edit().putString("last_alert", id).apply()
            val received = runCatching { Instant.parse(alert.optString("received_at")) }.getOrNull()
            if (received == null || Instant.now().epochSecond - received.epochSecond !in 0..30) return
            handler.post { showEmergency(kind) }
        } catch (_: Exception) {
            // The next poll retries when Wi-Fi or the backend becomes available.
        } finally {
            connection.disconnect()
        }
    }

    private fun showEmergency(kind: String) {
        val title = if (kind == "SOS") "SOS từ Smartband" else "Phát hiện ngã từ Smartband"
        val stopIntent = Intent(this, EmergencyAlertService::class.java).setAction(ACTION_STOP_ALARM)
        val stopAction = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        getSystemService(NotificationManager::class.java).notify(2, NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("Mở ứng dụng để xem và xác nhận an toàn")
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_lock_silent_mode_off, "Tắt chuông", stopAction)
            .build())
        if (alarmPlayer == null) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                alarmPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    setDataSource(this@EmergencyAlertService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }
        val vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
            else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 600, 500), 0))
    }

    private fun stopAlarm() {
        alarmPlayer?.run { stop(); release() }
        alarmPlayer = null
        val vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
            else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
        getSystemService(NotificationManager::class.java).cancel(2)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(this, 1,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    override fun onDestroy() {
        polling = false
        handler.removeCallbacks(poll)
        worker.shutdownNow()
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
