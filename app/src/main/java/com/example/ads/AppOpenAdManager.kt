package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val context: Context) {

    companion object {
        private const val TAG = "WafiAppOpenAdManager"
        // Official Google AdMob test ad unit ID for App Open Ads
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257390208"
        private const val PREFS_NAME = "wafi_ads_prefs"
        private const val KEY_LAST_SHOWN_TIME = "last_ad_shown_time"
        // 4 hours frequency capping
        private const val FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000L
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0

    init {
        try {
            MobileAds.initialize(context) {}
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds: ${e.message}")
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun loadAd(isAdsRemoved: Boolean) {
        if (isAdsRemoved) {
            Log.d(TAG, "Ads are removed by user purchase. Skipping ad load.")
            return
        }

        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        try {
            AppOpenAd.load(
                context,
                AD_UNIT_ID,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(TAG, "App Open Ad loaded successfully.")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.w(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            isLoadingAd = false
            Log.e(TAG, "Exception during ad load: ${e.message}")
        }
    }

    fun showAdIfAvailable(
        activity: Activity,
        isAdsRemoved: Boolean,
        onShowAdCompleteListener: () -> Unit = {}
    ) {
        if (isAdsRemoved) {
            onShowAdCompleteListener()
            return
        }

        if (isShowingAd) {
            return
        }

        // Check 4-hour frequency capping
        val lastShownTime = prefs.getLong(KEY_LAST_SHOWN_TIME, 0L)
        val now = System.currentTimeMillis()
        if (lastShownTime > 0 && (now - lastShownTime) < FOUR_HOURS_MILLIS) {
            Log.d(TAG, "App Open Ad skipped due to frequency capping (< 4 hours).")
            onShowAdCompleteListener()
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "App Open Ad not available yet. Preloading for next launch.")
            onShowAdCompleteListener()
            loadAd(isAdsRemoved)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                prefs.edit().putLong(KEY_LAST_SHOWN_TIME, System.currentTimeMillis()).apply()
                onShowAdCompleteListener()
                loadAd(isAdsRemoved)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.w(TAG, "App Open Ad failed to show: ${adError.message}")
                onShowAdCompleteListener()
                loadAd(isAdsRemoved)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "App Open Ad showed full screen content.")
            }
        }

        try {
            isShowingAd = true
            appOpenAd?.show(activity)
        } catch (e: Exception) {
            isShowingAd = false
            Log.e(TAG, "Exception showing App Open Ad: ${e.message}")
            onShowAdCompleteListener()
        }
    }
}
