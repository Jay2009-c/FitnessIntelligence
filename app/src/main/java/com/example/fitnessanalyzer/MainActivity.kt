package com.example.fitnessanalyzer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fitnessanalyzer.data.local.FitnessDatabase
import com.example.fitnessanalyzer.data.local.UserPreferences
import com.example.fitnessanalyzer.data.repository.WorkoutRepositoryImpl
import com.example.fitnessanalyzer.domain.usecase.CalculateAdvancedMetricsUseCase
import com.example.fitnessanalyzer.presentation.dashboard.DashboardScreen
import com.example.fitnessanalyzer.presentation.dashboard.DashboardViewModel
import com.example.fitnessanalyzer.presentation.dashboard.DashboardViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val neededPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.WeightRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.HeightRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.DistanceRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.StepsRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.SpeedRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.ElevationGainedRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.PowerRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = FitnessDatabase.getDatabase(applicationContext)
        val workoutDao = database.workoutDao()
        val userPreferences = UserPreferences(applicationContext)
        val calculateUseCase = CalculateAdvancedMetricsUseCase()
        val repository = WorkoutRepositoryImpl(applicationContext, workoutDao, userPreferences, calculateUseCase)

        val factory = DashboardViewModelFactory(repository, workoutDao, calculateUseCase, userPreferences)
        val viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        // Setup the asynchronous authorization screen dialog engine trigger
        val requestPermissionContract = PermissionController.createRequestPermissionResultContract()
        val permissionLauncher = registerForActivityResult(requestPermissionContract) { granted ->
            if (granted.containsAll(neededPermissions)) {
                viewModel.triggerLocalSyncPipeline()
            } else {
                Toast.makeText(this, "Permissions are required to analyze local data records.", Toast.LENGTH_LONG).show()
            }
        }

        // Initialize connection state monitoring procedures
        val sdkStatus = HealthConnectClient.getSdkStatus(this)
        if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
            val client = HealthConnectClient.getOrCreate(this)
            lifecycleScope.launch {
                val granted = client.permissionController.getGrantedPermissions()
                if (!granted.containsAll(neededPermissions)) {
                    android.util.Log.d("HealthConnect", "Requesting permissions: $neededPermissions")
                    permissionLauncher.launch(neededPermissions)
                } else {
                    viewModel.triggerLocalSyncPipeline()
                }
            }
        } else {
            // Check if it's missing or needs update
            val packageName = "com.google.android.apps.healthdata"
            val isInstalled = try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                false
            }

            if (!isInstalled) {
                Toast.makeText(this, "Health Connect app is required. Redirecting to Play Store...", Toast.LENGTH_LONG).show()
                try {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("market://details?id=$packageName")
                        setPackage("com.android.vending")
                    })
                } catch (e: Exception) {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    })
                }
            } else {
                Toast.makeText(this, "Health Connect is not supported or needs an update on this device.", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            DashboardScreen(viewModel = viewModel)
        }
    }
}
