package com.nuvio.tv.updater
import android.content.Context
import androidx.lifecycle.viewModelScope
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DanexusAutomaticUpdateTest {
    @Test fun deliveredDebugApkAutomaticallyChecksAndThrottlesRepeatedResumes() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<UpdateRepository>()
        val preferences = mockk<UpdatePreferences>()
        every { preferences.updateBannerEnabled } returns flowOf(true)
        every { preferences.ignoredTag } returns flowOf(null)
        coEvery { preferences.initializeForkStream() } just Runs
        coEvery { repository.getLatestUpdate(any()) } returns Result.failure(IllegalStateException("No release"))
        coEvery { preferences.setLastCheckAtMs(any()) } just Runs
        val vm = UpdateViewModel(mockk<Context>(relaxed = true), repository, preferences, mockk())
        try {
            runCurrent()
            coVerify(exactly = 1) { repository.getLatestUpdate(any()) }
            vm.checkForUpdates(false, false)
            runCurrent()
            coVerify(exactly = 1) { repository.getLatestUpdate(any()) }
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
    @Test fun resumeWaitsForPreferencesBeforeCheckingTheReleaseStream() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val ready = CompletableDeferred<Boolean>()
        val repository = mockk<UpdateRepository>()
        val preferences = mockk<UpdatePreferences>()
        every { preferences.updateBannerEnabled } returns flow { emit(ready.await()) }
        every { preferences.ignoredTag } returns flowOf(null)
        coEvery { preferences.initializeForkStream() } just Runs
        coEvery { repository.getLatestUpdate(any()) } returns Result.failure(IllegalStateException("No release"))
        coEvery { preferences.setLastCheckAtMs(any()) } just Runs
        val vm = UpdateViewModel(mockk<Context>(relaxed = true), repository, preferences, mockk())
        try {
            vm.checkForUpdates(false, false)
            runCurrent()
            coVerify(exactly = 0) { repository.getLatestUpdate(any()) }
            ready.complete(true)
            runCurrent()
            coVerify(exactly = 1) { repository.getLatestUpdate(any()) }
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun disabledBannerDoesNotAutomaticallyRequestARelease() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<UpdateRepository>(relaxed = true)
        val preferences = mockk<UpdatePreferences>()
        every { preferences.updateBannerEnabled } returns flowOf(false)
        coEvery { preferences.initializeForkStream() } just Runs
        val vm = UpdateViewModel(mockk<Context>(relaxed = true), repository, preferences, mockk())
        try {
            runCurrent()
            coVerify(exactly = 0) { repository.getLatestUpdate(any()) }
            assertFalse(vm.uiState.value.showBanner)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
