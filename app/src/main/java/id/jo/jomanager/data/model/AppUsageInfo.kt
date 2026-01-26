package id.jo.jomanager.data.model

import android.graphics.Bitmap

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val formattedTime: String,
    val usagePercentage: Int,
    val powerConsumptionMah: Double,
    val icon: Bitmap?
)
