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

import com.passbolt.mobile.android.core.commonfolders.usecase.FoldersInteractor
import com.passbolt.mobile.android.core.commongroups.usecase.GroupsInteractor
import com.passbolt.mobile.android.core.idlingresource.ResourcesFullRefreshIdlingResource
import com.passbolt.mobile.android.core.resources.usecase.ResourceInteractor
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypesInteractor
import com.passbolt.mobile.android.core.users.UsersInteractor
import com.passbolt.mobile.android.database.snapshot.ResourcesSnapshot
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
import com.passbolt.mobile.android.metadata.interactor.MetadataKeysInteractor
import com.passbolt.mobile.android.metadata.interactor.MetadataSessionKeysInteractor
import org.koin.dsl.module

fun testHomeDataModule(
    foldersInteractor: FoldersInteractor,
    resourcesInteractor: ResourceInteractor,
    groupsInteractor: GroupsInteractor,
    usersInteractor: UsersInteractor,
    resourceTypesInteractor: ResourceTypesInteractor,
    metadataKeysInteractor: MetadataKeysInteractor,
    metadataSessionKeysInteractor: MetadataSessionKeysInteractor,
    getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    resourcesFullRefreshIdlingResource: ResourcesFullRefreshIdlingResource,
    resourcesSnapshot: ResourcesSnapshot,
) = module {
    single { foldersInteractor }
    single { resourcesInteractor }
    single { groupsInteractor }
    single { usersInteractor }
    single { resourceTypesInteractor }
    single { metadataKeysInteractor }
    single { metadataSessionKeysInteractor }
    single { getFeatureFlagsUseCase }
    single { resourcesFullRefreshIdlingResource }
    single { resourcesSnapshot }
    single {
        HomeDataInteractor(
            foldersInteractor = get(),
            resourcesInteractor = get(),
            groupsInteractor = get(),
            usersInteractor = get(),
            resourceTypesInteractor = get(),
            metadataKeysInteractor = get(),
            metadataSessionKeysInteractor = get(),
            featureFlagsUseCase = get(),
            resourcesFullRefreshIdlingResource = get(),
            resourcesSnapshot = get(),
        )
    }
}
