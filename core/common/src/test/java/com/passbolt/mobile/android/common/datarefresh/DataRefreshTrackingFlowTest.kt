package com.passbolt.mobile.android.common.datarefresh

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.CriticalDataReady
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.Idle.NotCompleted
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.InProgress
import com.passbolt.mobile.android.common.datarefresh.DataRefreshStatus.LoadingSecondary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataRefreshTrackingFlowTest {

    private lateinit var dataRefreshTrackingFlow: DataRefreshTrackingFlow

    @Before
    fun setup() {
        dataRefreshTrackingFlow = DataRefreshTrackingFlow()
    }

    @Test
    fun `initial state is NotCompleted`() {
        assertThat(dataRefreshTrackingFlow.dataRefreshStatusFlow.value)
            .isEqualTo(NotCompleted)
    }

    @Test
    fun `updateStatus changes the state`() {
        dataRefreshTrackingFlow.updateStatus(InProgress)
        assertThat(dataRefreshTrackingFlow.dataRefreshStatusFlow.value)
            .isEqualTo(InProgress)
    }

    @Test
    fun `isCriticalDataReady returns true when state is CriticalDataReady`() {
        dataRefreshTrackingFlow.updateStatus(CriticalDataReady)
        assertThat(dataRefreshTrackingFlow.isCriticalDataReady()).isTrue()
    }

    @Test
    fun `isCriticalDataReady returns false when state is not CriticalDataReady`() {
        dataRefreshTrackingFlow.updateStatus(InProgress)
        assertThat(dataRefreshTrackingFlow.isCriticalDataReady()).isFalse()
    }

    @Test
    fun `isLoadingSecondary returns true when state is LoadingSecondary`() {
        dataRefreshTrackingFlow.updateStatus(LoadingSecondary)
        assertThat(dataRefreshTrackingFlow.isLoadingSecondary()).isTrue()
    }

    @Test
    fun `isLoadingSecondary returns false when state is not LoadingSecondary`() {
        dataRefreshTrackingFlow.updateStatus(InProgress)
        assertThat(dataRefreshTrackingFlow.isLoadingSecondary()).isFalse()
    }

    @Test
    fun `isInteractive returns true for CriticalDataReady`() {
        dataRefreshTrackingFlow.updateStatus(CriticalDataReady)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isTrue()
    }

    @Test
    fun `isInteractive returns true for LoadingSecondary`() {
        dataRefreshTrackingFlow.updateStatus(LoadingSecondary)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isTrue()
    }

    @Test
    fun `isInteractive returns true for FinishedWithSuccess`() {
        dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isTrue()
    }

    @Test
    fun `isInteractive returns false for InProgress`() {
        dataRefreshTrackingFlow.updateStatus(InProgress)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isFalse()
    }

    @Test
    fun `isInteractive returns false for NotCompleted`() {
        dataRefreshTrackingFlow.updateStatus(NotCompleted)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isFalse()
    }

    @Test
    fun `awaitCriticalDataReady suspends until CriticalDataReady`() = runTest {
        var completed = false

        // Launch awaiting coroutine
        val job = launch {
            dataRefreshTrackingFlow.awaitCriticalDataReady()
            completed = true
        }

        // Ensure it hasn't completed yet
        advanceUntilIdle()
        assertThat(completed).isFalse()

        // Transition to CriticalDataReady
        dataRefreshTrackingFlow.updateStatus(CriticalDataReady)
        advanceUntilIdle()

        // Now it should be completed
        assertThat(completed).isTrue()
        job.cancel()
    }

    @Test
    fun `awaitCriticalDataReady completes for LoadingSecondary`() = runTest {
        var completed = false

        val job = launch {
            dataRefreshTrackingFlow.awaitCriticalDataReady()
            completed = true
        }

        advanceUntilIdle()
        assertThat(completed).isFalse()

        dataRefreshTrackingFlow.updateStatus(LoadingSecondary)
        advanceUntilIdle()

        assertThat(completed).isTrue()
        job.cancel()
    }

    @Test
    fun `progressive state transitions work correctly`() {
        // Simulate progressive loading state flow
        dataRefreshTrackingFlow.updateStatus(InProgress)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isFalse()
        assertThat(dataRefreshTrackingFlow.isInProgress()).isTrue()

        dataRefreshTrackingFlow.updateStatus(CriticalDataReady)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isTrue()
        assertThat(dataRefreshTrackingFlow.isCriticalDataReady()).isTrue()

        dataRefreshTrackingFlow.updateStatus(LoadingSecondary)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isTrue()
        assertThat(dataRefreshTrackingFlow.isLoadingSecondary()).isTrue()

        dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)
        assertThat(dataRefreshTrackingFlow.isInteractive()).isTrue()
    }
}
