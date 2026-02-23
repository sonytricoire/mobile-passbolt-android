package com.passbolt.mobile.android.core.fulldatarefresh

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commongroups.usecase.GroupsInteractor
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypesInteractor
import com.passbolt.mobile.android.core.users.UsersInteractor
import com.passbolt.mobile.android.database.snapshot.ResourcesSnapshot
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataSessionKeysInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDataInteractorTest {

    private lateinit var foldersInteractor: FoldersInteractor
    private lateinit var resourcesInteractor: ResourceInteractor
    private lateinit var groupsInteractor: GroupsInteractor
    private lateinit var usersInteractor: UsersInteractor
    private lateinit var resourceTypesInteractor: ResourceTypesInteractor
    private lateinit var metadataKeysInteractor: MetadataKeysInteractor
    private lateinit var metadataSessionKeysInteractor: MetadataSessionKeysInteractor
    private lateinit var featureFlagsUseCase: GetFeatureFlagsUseCase
    private lateinit var resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource
    private lateinit var resourcesSnapshot: ResourcesSnapshot

    private lateinit var homeDataInteractor: HomeDataInteractor

    @Before
    fun setup() {
        foldersInteractor = mockk()
        resourcesInteractor = mockk()
        groupsInteractor = mockk()
        usersInteractor = mockk()
        resourceTypesInteractor = mockk()
        metadataKeysInteractor = mockk()
        metadataSessionKeysInteractor = mockk()
        featureFlagsUseCase = mockk()
        resourcesFullRefreshIdlingResource = mockk(relaxed = true)
        resourcesSnapshot = mockk(relaxed = true)

        homeDataInteractor = HomeDataInteractor(
            foldersInteractor = foldersInteractor,
            resourcesInteractor = resourcesInteractor,
            groupsInteractor = groupsInteractor,
            usersInteractor = usersInteractor,
            resourceTypesInteractor = resourceTypesInteractor,
            metadataKeysInteractor = metadataKeysInteractor,
            metadataSessionKeysInteractor = metadataSessionKeysInteractor,
            featureFlagsUseCase = featureFlagsUseCase,
            resourcesFullRefreshIdlingResource = resourcesFullRefreshIdlingResource,
            resourcesSnapshot = resourcesSnapshot
        )
    }

    @Test
    fun `loadCriticalData loads resource types and resources in parallel`() = runTest {
        // Given
        coEvery { resourceTypesInteractor.fetchAndSaveResourceTypes() } returns
            ResourceTypesInteractor.Output.Success
        coEvery { resourcesInteractor.fetchAndSaveResources() } returns
            ResourceInteractor.Output.Success

        // When
        val result = homeDataInteractor.loadCriticalData()

        // Then
        assertThat(result).isEqualTo(HomeDataInteractor.Output.Success)
        coVerify { resourceTypesInteractor.fetchAndSaveResourceTypes() }
        coVerify { resourcesInteractor.fetchAndSaveResources() }
    }

    @Test
    fun `loadCriticalData returns failure when resource types fails`() = runTest {
        // Given
        coEvery { resourceTypesInteractor.fetchAndSaveResourceTypes() } returns
            ResourceTypesInteractor.Output.Failure(AuthenticationState.Authenticated)
        coEvery { resourcesInteractor.fetchAndSaveResources() } returns
            ResourceInteractor.Output.Success

        // When
        val result = homeDataInteractor.loadCriticalData()

        // Then
        assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
    }

    @Test
    fun `loadCriticalData returns failure when resources fails`() = runTest {
        // Given
        coEvery { resourceTypesInteractor.fetchAndSaveResourceTypes() } returns
            ResourceTypesInteractor.Output.Success
        coEvery { resourcesInteractor.fetchAndSaveResources() } returns
            ResourceInteractor.Output.Failure(AuthenticationState.Authenticated)

        // When
        val result = homeDataInteractor.loadCriticalData()

        // Then
        assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
    }

    @Test
    fun `refreshAllHomeScreenData invokes callback after critical data loads`() = runTest {
        // Given
        var callbackInvoked = false
        coEvery { resourceTypesInteractor.fetchAndSaveResourceTypes() } returns
            ResourceTypesInteractor.Output.Success
        coEvery { resourcesInteractor.fetchAndSaveResources() } returns
            ResourceInteractor.Output.Success
        // Mock all secondary data as success
        coEvery { usersInteractor.fetchAndSaveUsers() } returns UsersInteractor.Output.Success
        coEvery { groupsInteractor.fetchAndSaveGroups() } returns GroupsInteractor.Output.Success
        coEvery { foldersInteractor.fetchAndSaveFolders() } returns FoldersInteractor.Output.Success
        coEvery { featureFlagsUseCase.execute(any()) } returns mockk {
            every { featureFlags } returns mockk {
                every { isV5MetadataAvailable } returns false
            }
        }

        // When
        homeDataInteractor.refreshAllHomeScreenData(
            onCriticalDataReady = { callbackInvoked = true }
        )

        // Then
        assertThat(callbackInvoked).isTrue()
    }

    @Test
    fun `refreshAllHomeScreenData returns failure immediately if critical data fails`() = runTest {
        // Given
        coEvery { resourceTypesInteractor.fetchAndSaveResourceTypes() } returns
            ResourceTypesInteractor.Output.Failure(AuthenticationState.Authenticated)
        coEvery { resourcesInteractor.fetchAndSaveResources() } returns
            ResourceInteractor.Output.Success

        // When
        val result = homeDataInteractor.refreshAllHomeScreenData()

        // Then
        assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
        verify { resourcesSnapshot.clear() }
        verify { resourcesFullRefreshIdlingResource.setIdle(true) }
    }
}
