package com.gatekeeper.mobile.ui.screens.traffic

import androidx.lifecycle.ViewModel
import com.gatekeeper.mobile.data.db.dao.CountryCount
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.data.repository.TrafficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(
    trafficRepository: TrafficRepository
) : ViewModel() {

    val recentConnections: Flow<List<ConnectionLog>> = trafficRepository.observeRecent(200)
    val blockedConnections: Flow<List<ConnectionLog>> = trafficRepository.observeBlocked()
    val topCountries: Flow<List<CountryCount>> = trafficRepository.observeTopCountries()
    val totalConnections: Flow<Int> = trafficRepository.observeTotalCount()
    val totalBytesIn: Flow<Long?> = trafficRepository.observeTotalBytesIn()
    val totalBytesOut: Flow<Long?> = trafficRepository.observeTotalBytesOut()
}
