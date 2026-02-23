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
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataSessionKeysInteractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDataInteractorTest : KoinTest {
    private val homeDataInteractor: HomeDataInteractor by inject()

    private val mockFoldersInteractor: FoldersInteractor = mock()
    private val mockResourcesInteractor: ResourceInteractor = mock()
    private val mockGroupsInteractor: GroupsInteractor = mock()
    private val mockUsersInteractor: UsersInteractor = mock()
    private val mockResourceTypesInteractor: ResourceTypesInteractor = mock()
    private val mockMetadataKeysInteractor: MetadataKeysInteractor = mock()
    private val mockMetadataSessionKeysInteractor: MetadataSessionKeysInteractor = mock()
    private val mockGetFeatureFlagsUseCase: GetFeatureFlagsUseCase = mock()
    private val mockResourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource = mock()
    private val mockResourcesSnapshot: ResourcesSnapshot = mock()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                testHomeDataModule(
                    foldersInteractor = mockFoldersInteractor,
                    resourcesInteractor = mockResourcesInteractor,
                    groupsInteractor = mockGroupsInteractor,
                    usersInteractor = mockUsersInteractor,
                    resourceTypesInteractor = mockResourceTypesInteractor,
                    metadataKeysInteractor = mockMetadataKeysInteractor,
                    metadataSessionKeysInteractor = mockMetadataSessionKeysInteractor,
                    getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
                    resourcesFullRefreshIdlingResource = mockResourcesFullRefreshIdlingResource,
                    resourcesSnapshot = mockResourcesSnapshot,
                ),
            )
        }

    @After
    fun tearDown() {
        reset(
            mockFoldersInteractor,
            mockResourcesInteractor,
            mockGroupsInteractor,
            mockUsersInteractor,
            mockResourceTypesInteractor,
            mockMetadataKeysInteractor,
            mockMetadataSessionKeysInteractor,
            mockGetFeatureFlagsUseCase,
            mockResourcesFullRefreshIdlingResource,
            mockResourcesSnapshot,
        )
    }

    @Test
    fun `refresh should call all interactors in parallel when V5 metadata is available`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = true)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .doReturn(MetadataKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            homeDataInteractor.refreshAllHomeScreenData()

            // Then - verify all interactors were called
            verify(mockMetadataKeysInteractor).fetchAndSaveMetadataKeys()
            verify(mockMetadataSessionKeysInteractor).fetchMetadataSessionKeys()
            verify(mockMetadataSessionKeysInteractor).saveMetadataSessionKeysCache()
            verify(mockResourceTypesInteractor).fetchAndSaveResourceTypes()
            verify(mockUsersInteractor).fetchAndSaveUsers()
            verify(mockGroupsInteractor).fetchAndSaveGroups()
            verify(mockFoldersInteractor).fetchAndSaveFolders()
            verify(mockResourcesInteractor).fetchAndSaveResources()
            verify(mockResourcesFullRefreshIdlingResource).setIdle(false)
            verify(mockResourcesFullRefreshIdlingResource).setIdle(true)
            verify(mockResourcesSnapshot).populateForCurrentAccount()
            verify(mockResourcesSnapshot).clear()
        }

    @Test
    fun `refresh should return success when all interactors succeed`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = true)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .doReturn(MetadataKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            val result = homeDataInteractor.refreshAllHomeScreenData()

            // Then
            assertThat(result).isInstanceOf(HomeDataInteractor.Output.Success::class.java)
            assertThat(result.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `refresh should return failure when resource types interactor fails`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = true)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .doReturn(MetadataKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(
                    ResourceTypesInteractor.Output.Failure(
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Session,
                        ),
                    ),
                )
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            val result = homeDataInteractor.refreshAllHomeScreenData()

            // Then
            assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
            assertThat(result.authenticationState).isInstanceOf(AuthenticationState.Unauthenticated::class.java)
        }

    @Test
    fun `refresh should return failure when users interactor fails`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = true)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .doReturn(MetadataKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(
                    UsersInteractor.Output.Failure(
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Session,
                        ),
                    ),
                )
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            val result = homeDataInteractor.refreshAllHomeScreenData()

            // Then
            assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
            assertThat(result.authenticationState).isInstanceOf(AuthenticationState.Unauthenticated::class.java)
        }

    @Test
    fun `refresh should skip metadata operations when V5 metadata is not available`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = false)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            val result = homeDataInteractor.refreshAllHomeScreenData()

            // Then
            assertThat(result).isInstanceOf(HomeDataInteractor.Output.Success::class.java)
            verify(mockResourceTypesInteractor).fetchAndSaveResourceTypes()
            verify(mockUsersInteractor).fetchAndSaveUsers()
            verify(mockGroupsInteractor).fetchAndSaveGroups()
            verify(mockFoldersInteractor).fetchAndSaveFolders()
            verify(mockResourcesInteractor).fetchAndSaveResources()
            // Metadata operations should not be called when V5 metadata is not available
            verify(mockMetadataKeysInteractor, org.mockito.kotlin.never()).fetchAndSaveMetadataKeys()
            verify(
                mockMetadataSessionKeysInteractor,
                org.mockito.kotlin.never(),
            ).fetchMetadataSessionKeys()
            verify(
                mockMetadataSessionKeysInteractor,
                org.mockito.kotlin.never(),
            ).saveMetadataSessionKeysCache()
        }

    @Test
    fun `refresh should return failure when metadata keys interactor fails`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = true)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .doReturn(
                    MetadataKeysInteractor.Output.Failure(
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Session,
                        ),
                    ),
                )
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            val result = homeDataInteractor.refreshAllHomeScreenData()

            // Then
            assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
            assertThat(result.authenticationState).isInstanceOf(AuthenticationState.Unauthenticated::class.java)
        }

    @Test
    fun `refresh should return failure when save metadata session keys fails`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = true)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .doReturn(MetadataKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .doReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .doReturn(
                    MetadataSessionKeysInteractor.Output.Failure(
                        AuthenticationState.Unauthenticated(
                            AuthenticationState.Unauthenticated.Reason.Session,
                        ),
                    ),
                )
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            val result = homeDataInteractor.refreshAllHomeScreenData()

            // Then
            assertThat(result).isInstanceOf(HomeDataInteractor.Output.Failure::class.java)
            assertThat(result.authenticationState).isInstanceOf(AuthenticationState.Unauthenticated::class.java)
        }

    @Test
    fun `refresh should set idling resource correctly`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = false)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            homeDataInteractor.refreshAllHomeScreenData()

            // Then
            verify(mockResourcesFullRefreshIdlingResource).setIdle(false)
            verify(mockResourcesFullRefreshIdlingResource).setIdle(true)
        }

    @Test
    fun `refresh should populate and clear resources snapshot`() =
        runTest {
            // Given
            val featureFlags = FeatureFlagsModel(isV5MetadataAvailable = false)
            whenever(mockGetFeatureFlagsUseCase.execute(any()))
                .doReturn(GetFeatureFlagsUseCase.Output(featureFlags))
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .doReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .doReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .doReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders())
                .doReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources())
                .doReturn(ResourceInteractor.Output.Success)

            // When
            homeDataInteractor.refreshAllHomeScreenData()

            // Then
            verify(mockResourcesSnapshot).populateForCurrentAccount()
            verify(mockResourcesSnapshot).clear()
        }
}
