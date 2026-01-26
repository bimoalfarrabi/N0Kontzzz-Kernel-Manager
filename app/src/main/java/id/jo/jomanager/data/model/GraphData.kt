package id.jo.jomanager.data.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class GraphData(
    val cpuLoadHistory: ImmutableList<Float> = persistentListOf(),
    val cpuSpeedHistory: ImmutableList<Float> = persistentListOf(),
    val gpuHistory: ImmutableList<Float> = persistentListOf(),
    val cpuGraphMode: GraphMode = GraphMode.LOAD
)

enum class GraphMode {
    SPEED,
    LOAD
}
