/*
 *     RIGLogger - A Ham Radio Logging Solution for Android with Cloudlog
 *     Copyright (C) 2025 Gong Zhile
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.rad1o.riglogger

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import cn.rad1o.riglogger.databinding.ActivityMainBinding
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainViewModel

    private var rigControlService: RIGControlService? = null
    private var bound = false

    private lateinit var badge: BadgeDrawable
    private lateinit var navController: NavController

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as RIGControlService.LocalBinder
            rigControlService = binder.getService()
            bound = true

            val cloudlogUrl: String? = applicationContext.let {
                PreferenceManager
                    .getDefaultSharedPreferences(it)
                    .getString("cloudlog_url", null)
            }

            val cloudlogApiKey: String? = applicationContext.let {
                PreferenceManager
                    .getDefaultSharedPreferences(it)
                    .getString("cloudlog_apikey", null)
            }

            if ((cloudlogApiKey != null) && (cloudlogUrl != null)) {
                rigControlService?.configureCloudlog("$cloudlogUrl/api/radio", cloudlogApiKey)
            } else {
                Toast.makeText(applicationContext,
                    getString(R.string.rig_control_won_t_upload_cloudlog_settings_not_found),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
         /*   badge.backgroundColor = getColor(R.color.error)*/
            bound = false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        DynamicColors.applyToActivitiesIfAvailable(application)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001 // Request code; doesn't matter here
                )
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: NavigationBarView = binding.navView as NavigationBarView
        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                    as NavHostFragment).navController

        navView.setupWithNavController(navController)

        badge = navView.getOrCreateBadge(R.id.navigation_rigctl)
        if (viewModel.isServiceRunning)
            badge.backgroundColor = getColor(R.color.success)
        else
            badge.backgroundColor = getColor(R.color.error)
        badge.isVisible = true

        startService()
    }

    fun redrawRigctl() {
        if (navController.currentDestination?.id == R.id.navigation_rigctl) {
            navController.navigate(R.id.navigation_rigctl)
        }
    }

    override fun onStart() {
        super.onStart()

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                badge.backgroundColor = getColor(R.color.error)
                viewModel.isServiceRunning = false

                try {
                    unbindService(connection)
                } catch (_: Exception) {
                    null
                }

                bound = false

                redrawRigctl()
            }
        }, IntentFilter("cn.rad1o.riglogger.ACTION_SERVICE_STOPPED"), RECEIVER_EXPORTED)

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                badge.backgroundColor = getColor(R.color.success)
                viewModel.isServiceRunning = true

                redrawRigctl()
            }
        }, IntentFilter("cn.rad1o.riglogger.ACTION_SERVICE_STARTED"), RECEIVER_EXPORTED)
    }

    fun startService() {
        if (bound) {
            try {
                unbindService(connection)
            }  catch (_: Exception) {
                null
            }
            bound = false
        }
        val intent = Intent(this, RIGControlService::class.java)
        stopService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }
}