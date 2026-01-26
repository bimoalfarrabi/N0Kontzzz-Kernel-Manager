package id.jo.jomanager.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import id.jo.jomanager.manager.TileUpdateManager
import id.jo.jomanager.service.RootActionService
import id.jo.jomanager.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KgslTileService : TileService() {

    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var tileUpdateManager: TileUpdateManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var collectorJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            tileUpdateManager.updateFlow.collectLatest {
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        collectorJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        collectorJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, RootActionService::class.java).apply {
            action = RootActionService.ACTION_TOGGLE_KGSL
        }
        startForegroundService(intent)
    }

    fun updateTile() {
        val isEnabled = preferenceManager.getKgslSkipZeroing()
        qsTile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.label = "KGSL Zeroing"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = if (isEnabled) "Active" else "Inactive"
        }
        qsTile.updateTile()
    }
}