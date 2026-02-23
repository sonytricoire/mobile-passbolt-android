package com.passbolt.mobile.android.common.datarefresh

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DataRefreshStatusTest {

    @Test
    fun `CriticalDataReady is instance of DataRefreshStatus`() {
        val status: DataRefreshStatus = DataRefreshStatus.CriticalDataReady
        assertThat(status).isInstanceOf(DataRefreshStatus::class.java)
    }

    @Test
    fun `LoadingSecondary is instance of DataRefreshStatus`() {
        val status: DataRefreshStatus = DataRefreshStatus.LoadingSecondary
        assertThat(status).isInstanceOf(DataRefreshStatus::class.java)
    }

    @Test
    fun `all states are distinct`() {
        val idle = DataRefreshStatus.Idle.NotCompleted
        val inProgress = DataRefreshStatus.InProgress
        val criticalReady = DataRefreshStatus.CriticalDataReady
        val loadingSecondary = DataRefreshStatus.LoadingSecondary
        val success = DataRefreshStatus.Idle.FinishedWithSuccess
        val failure = DataRefreshStatus.Idle.FinishedWithFailure

        assertThat(setOf(idle, inProgress, criticalReady, loadingSecondary, success, failure))
            .hasSize(6)
    }
}
