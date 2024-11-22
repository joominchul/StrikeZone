package com.example.strikezone

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.strikezone.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
	private var serviceStart = false
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
		binding.button.setOnClickListener {
			if (!serviceStart) {
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
		binding.button.text = "stop"
		serviceStart = true
	}

	private fun stopFloatingViewService() {
		val intent = Intent(this, FloatingViewService::class.java)
		stopService(intent)
		binding.button.text = "start"
		serviceStart = false
	}

	companion object {
		private const val REQUEST_CODE_FOREGROUND_SERVICE = 1001
	}
}