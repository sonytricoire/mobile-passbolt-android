/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
package com.passbolt.mobile.android.common.datarefresh

import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.CriticalDataReady
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.LoadingSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class DataRefreshTrackingFlow {
    val dataRefreshStatusFlow: StateFlow<DataRefreshStatus>
        get() = _dataRefreshStatusFlow
    private val _dataRefreshStatusFlow = MutableStateFlow<DataRefreshStatus>(NotCompleted)

    fun updateStatus(newStatus: DataRefreshStatus) {
        _dataRefreshStatusFlow.value = newStatus
    }

    fun isInProgress(): Boolean = _dataRefreshStatusFlow.value is InProgress

    suspend fun awaitIdle() {
        _dataRefreshStatusFlow.first { it is Idle }
    }

    /**
     * Returns true if critical data (resource types and resources) is ready.
     * UI can become interactive at this point.
     */
    fun isCriticalDataReady(): Boolean = _dataRefreshStatusFlow.value is CriticalDataReady

    /**
     * Returns true if secondary data is currently loading in the background.
     * This is a non-blocking state where user interaction is allowed.
     */
    fun isLoadingSecondary(): Boolean = _dataRefreshStatusFlow.value is LoadingSecondary

    /**
     * Suspends until critical data is ready (either CriticalDataReady, LoadingSecondary, or Idle state).
     * Use this to wait for the point where UI can become interactive.
     */
    suspend fun awaitCriticalDataReady() {
        _dataRefreshStatusFlow.first { it is CriticalDataReady || it is LoadingSecondary || it is Idle }
    }

    /**
     * Returns true if the UI should be interactive.
     * Interactive states are: CriticalDataReady, LoadingSecondary, and Idle (except NotCompleted).
     * During InProgress and NotCompleted states, UI should remain blocked.
     */
    fun isInteractive(): Boolean {
        return when (val status = _dataRefreshStatusFlow.value) {
            is CriticalDataReady -> true
            is LoadingSecondary -> true
            is Idle -> status !is NotCompleted
            else -> false
        }
    }
}
