package id.jo.jomanager.data.model
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SystemInfo(
    val model: String,
    val codename: String,
    val androidVersion: String,
    val sdk: Int,
    val soc: String,
    val board: String = "",
    val fingerprint: String,
    // Display Information
    val screenResolution: String,
    val displayTechnology: String,
    val refreshRate: String,
    val screenDpi: String
)