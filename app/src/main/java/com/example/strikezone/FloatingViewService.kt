package com.example.strikezone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatingViewService : Service() {

	private lateinit var windowManager: WindowManager
	private lateinit var floatingView: View
	private lateinit var sharedPreferences: SharedPreferences
	private lateinit var params: WindowManager.LayoutParams
	override fun onCreate() {
		super.onCreate()
		sharedPreferences = getSharedPreferences("FloatingViewPrefs", Context.MODE_PRIVATE)
		// WindowManager 초기화
		windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

		// 플로팅 뷰 초기화
		floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)

		// WindowManager.LayoutParams 설정
		params = WindowManager.LayoutParams(
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
			//초기 좌표
			x = sharedPreferences.getInt("lastX", 0)
			y = sharedPreferences.getInt("lastY", 0)
			width = sharedPreferences.getInt("lastWidth", 100)
			height = sharedPreferences.getInt("lastHeight", 100)
		}

		// 플로팅 뷰 추가
		windowManager.addView(floatingView, params)

		// 플로팅 뷰 드래그 처리
		floatingView.setOnTouchListener(object : View.OnTouchListener {
			private var initialX = 0
			private var initialY = 0
			private var initialTouchX = 0f
			private var initialTouchY = 0f
			private var initialWidth = 100
			private var initialHeight = 100

			override fun onTouch(v: View, event: MotionEvent): Boolean {
				when (event.action) {
					MotionEvent.ACTION_DOWN -> {
						initialX = if(params.x>=0) params.x else 0
						initialY = if(params.y>=0) params.y else 0
						initialTouchX = event.rawX
						initialTouchY = event.rawY
						initialWidth = params.width
						initialHeight = params.height

						return true
					}
					MotionEvent.ACTION_MOVE -> {
						// 드래그 처리
						params.x = initialX + (event.rawX - initialTouchX).toInt()
						params.y = initialY + (event.rawY - initialTouchY).toInt()
						Log.d("testt", "event.rawX: ${event.rawX}, initialX: $initialX, initialWidth: $initialWidth")
						Log.d("testt", "event.rawY: ${event.rawY}, initialY: $initialY, initialHeight: $initialHeight")

						windowManager.updateViewLayout(floatingView, params)

						return true
					}
				}
				return false
			}
		})

		floatingView.findViewById<TextView>(R.id.floating_text).setOnTouchListener(object : View.OnTouchListener{
			private var initialWidth = 100
			private var initialHeight = 100
			private var initialTouchX = 0f
			private var initialTouchY = 0f
			override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
				when(motionEvent.action){
					MotionEvent.ACTION_DOWN -> {
						initialTouchX = motionEvent.rawX
						initialTouchY = motionEvent.rawY
						initialWidth = params.width
						initialHeight = params.height
						return true
					}
					MotionEvent.ACTION_UP -> {
						params.width = initialWidth + (motionEvent.rawX - initialTouchX).toInt()
						params.height = initialHeight + (motionEvent.rawY - initialTouchY).toInt()
						windowManager.updateViewLayout(floatingView, params)
						return true
					}

				}
				return false
			}
		})

		floatingView.findViewById<View>(R.id.floating_close).setOnClickListener {
			MainActivity.serviceStart.value = false
			stopSelf()
		}
	}

	override fun onDestroy() {

		saveLastPosition() // 서비스 종료 시 마지막 위치 저장
		windowManager.removeView(floatingView)
		super.onDestroy()
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

	private fun saveLastPosition() {
		val editor = sharedPreferences.edit()
		editor.putInt("lastX", params.x)
		editor.putInt("lastY", params.y)
		editor.putInt("lastWidth", params.width)
		editor.putInt("lastHeight", params.height)
		editor.apply()
	}

}
