package id.jo.jomanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.jo.jomanager.data.model.StorageInfo
import id.jo.jomanager.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageInfoViewModel @Inject constructor(
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _storageInfo = MutableStateFlow(
        StorageInfo(
            totalSpace = 0L,
            freeSpace = 0L,
            usedSpace = 0L,
            internalTotalSpace = 0L,
            internalFreeSpace = 0L,
            internalUsedSpace = 0L,
            externalTotalSpace = null,
            externalFreeSpace = null,
            externalUsedSpace = null,
            hasExternalStorage = false
        )
    )
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    init {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            storageRepository.getStorageInfo().collect { info ->
                _storageInfo.value = info
            }
        }
    }

    fun refreshStorageInfo() {
        loadStorageInfo()
    }

    fun formatStorageSize(bytes: Long): String {
        return storageRepository.formatStorageSize(bytes)
    }
}
