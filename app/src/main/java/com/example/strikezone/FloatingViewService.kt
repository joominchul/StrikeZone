package com.example.strikezone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FloatingViewService : Service() {

	private lateinit var windowManager: WindowManager
	private lateinit var floatingView: View

	override fun onCreate() {
		super.onCreate()

		// WindowManager 초기화
		windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

		// 플로팅 뷰 초기화
		floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)

		// WindowManager.LayoutParams 설정
		val params = WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
			else
				WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT
		).apply {
			gravity = Gravity.TOP or Gravity.START
			x = 0
			y = 100
		}

		// 플로팅 뷰 추가
		windowManager.addView(floatingView, params)

		// 플로팅 뷰 드래그 처리
		floatingView.setOnTouchListener(object : View.OnTouchListener {
			private var initialX = 0
			private var initialY = 0
			private var initialTouchX = 0f
			private var initialTouchY = 0f

			override fun onTouch(v: View, event: MotionEvent): Boolean {
				when (event.action) {
					MotionEvent.ACTION_DOWN -> {
						initialX = params.x
						initialY = params.y
						initialTouchX = event.rawX
						initialTouchY = event.rawY
						return true
					}
					MotionEvent.ACTION_MOVE -> {
						params.x = initialX + (event.rawX - initialTouchX).toInt()
						params.y = initialY + (event.rawY - initialTouchY).toInt()
						windowManager.updateViewLayout(floatingView, params)
						return true
					}
				}
				return false
			}
		})
	}

	override fun onDestroy() {
		super.onDestroy()
		windowManager.removeView(floatingView)
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				"floating_view_channel",
				"Floating View Service",
				NotificationManager.IMPORTANCE_LOW
			)
			val manager = getSystemService(NotificationManager::class.java)
			manager?.createNotificationChannel(channel)
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		createNotificationChannel()

		val notification = NotificationCompat.Builder(this, "floating_view_channel")
			.setContentTitle("플로팅 뷰 실행 중")
			.setContentText("플로팅 창이 실행 중입니다.")
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.build()

		startForeground(1, notification)

		return START_STICKY
	}
}
