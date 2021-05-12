package com.infinity.shapefactorymeasurement

import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.infinity.shapefactorymeasurement.databinding.ActivityMainBinding
import com.infinity.shapefactorymeasurement.viewmodel.ViewModel

class MainActivity : AppCompatActivity() {
    val viewModel : ViewModel by viewModels()
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            val toast = Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            if (!shouldShowRequestPermissionRationale(this.toString())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }
}