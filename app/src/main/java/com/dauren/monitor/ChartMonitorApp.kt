package com.dauren.monitor

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.dauren.monitor.core.common.coroutines.AppInitializer
import com.scichart.charting.visuals.SciChartSurface
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChartMonitorApp : Application() {

    @Inject
    lateinit var initializers: Set<@JvmSuppressWildcards AppInitializer>

    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        initSciChartLicense()
        initializers.forEach(AppInitializer::init)
    }

    /**
     * StrictMode in debug catches accidental disk / network / long work on main plus leaked
     * closables, activities and registrations. `penaltyLog` only — no `penaltyDeath`, so a benign
     * startup violation won't crash the app.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build(),
        )
    }

    /** Must run before any [SciChartSurface] is created. Key comes from `local.properties` via BuildConfig. */
    private fun initSciChartLicense() {
        val key = BuildConfig.SCICHART_LICENSE
        if (key.isBlank()) {
            Log.w(TAG, "SciChart license is missing: add scichart.license to local.properties (see README)")
            return
        }
        runCatching { SciChartSurface.setRuntimeLicenseKey(key) }
            .onFailure { Log.e(TAG, "SciChart license key is invalid or expired", it) }
    }

    private companion object {
        const val TAG = "ChartMonitorApp"
    }
}
