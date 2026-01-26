
package id.jo.jomanager.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileUpdateManager @Inject constructor() {
    private val _updateFlow = MutableSharedFlow<String>(replay = 0)
    val updateFlow = _updateFlow.asSharedFlow()

    suspend fun notifyUpdate(action: String) {
        _updateFlow.emit(action)
    }
}
