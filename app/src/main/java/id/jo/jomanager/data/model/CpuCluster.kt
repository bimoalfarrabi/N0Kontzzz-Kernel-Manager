package id.jo.jomanager.data.model
import kotlinx.serialization.Serializable

@Serializable
data class CpuCluster(
    val name: String,
    val minFreq: Int,
    val maxFreq: Int,
    val governor: String,
    val availableGovernors: List<String>
)