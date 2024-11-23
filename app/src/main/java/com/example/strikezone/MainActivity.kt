package com.example.strikezone

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.strikezone.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)
		//권한 요청
		if (!Settings.canDrawOverlays(this)) {
			val intent = Intent(
				Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
				Uri.parse("package:$packageName")
			)
			val REQUEST_CODE_OVERLAY = 10
			startActivityForResult(intent, REQUEST_CODE_OVERLAY)
		}
		//알림 권한 확인
		val notificationManager = NotificationManagerCompat.from(this)
		if (!notificationManager.areNotificationsEnabled()) {
			// 알림 권한이 없는 경우
			// 사용자에게 알림 권한을 허용하도록 요청
			AlertDialog.Builder(this)
				.setTitle("알림 권한")
				.setMessage("플로팅 뷰가 실행 중인지 알림을 통해 확인할 수 있습니다.\n알림 권한을 허용하시겠습니까?")
				.setPositiveButton("허용") { dialog, which ->
					// 앱 설정 화면으로 이동
					val intent = Intent()
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
						intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
					} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
						intent.putExtra("app_package", this.packageName)
						intent.putExtra("app_uid", this.applicationInfo.uid)
					} else {
						intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
						intent.addCategory(Intent.CATEGORY_DEFAULT)
						intent.data = Uri.parse("package:" + this.packageName)
					}
					this.startActivity(intent)
				}
				.setNegativeButton("취소", null)
				.show()
		} else {
			// 알림 권한이 있는 경우
			// 알림 표시
		}
		serviceStartObserve()
		binding.button.setOnClickListener {
			if (!serviceStart.value!!) {
				// Check for foreground service permission
				if (ContextCompat.checkSelfPermission(
						this,
						android.Manifest.permission.FOREGROUND_SERVICE
					) != PackageManager.PERMISSION_GRANTED
				) {
					// Request the permission
					ActivityCompat.requestPermissions(
						this,
						arrayOf(android.Manifest.permission.FOREGROUND_SERVICE),
						REQUEST_CODE_FOREGROUND_SERVICE
					)
				} else {
					// Permission already granted, start the service
					startFloatingViewService()
				}
			} else {
				stopFloatingViewService()
			}
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_CODE_FOREGROUND_SERVICE) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission granted, start the service
				startFloatingViewService()
			} else {
				// Permission denied, handle accordingly (e.g., show a message)
				// ...
			}
		}
	}

	private fun startFloatingViewService() {
		val intent = Intent(this, FloatingViewService::class.java)
		startService(intent)
		serviceStart.value = true
	}

	private fun stopFloatingViewService() {
		val intent = Intent(this, FloatingViewService::class.java)
		stopService(intent)
		serviceStart.value = false
	}

	companion object {
		private const val REQUEST_CODE_FOREGROUND_SERVICE = 1001
		var serviceStart = MutableLiveData<Boolean>(false)
	}

	private fun serviceStartObserve(){
		serviceStart.observe(this, Observer {
			if (it){
				binding.button.text = "stop"
			}
			else{
				binding.button.text = "start"
			}
		})
	}
}