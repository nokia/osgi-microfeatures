/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

// Views identifiers useful for actions and paths
import { views } from '../../state/ducks/views/constants'

// App: Views/Components
import Welcome from '../pages/welcome'
import Features from '../pages/features'
import DashBoard from '../pages/dashboard'
import Assemblies from '../pages/assemblies'
import Snapshots from '../pages/snapshots'
import Obrs from '../pages/obrs'
import Settings from '../pages/settings'
import Helps from '../pages/helps'

export const viewsMap = [
    {
        path: '/',
        exact: true,
        component: Welcome
    },
    {
        id: views.DASHBOARD,
        component: DashBoard
    },
    {
        id: views.DASHBOARD,
        component: DashBoard
    },
    {
        id : views.FEATURES,
        component: Features
    },
    {
        id : views.ASSEMBLIES,
        component: Assemblies
    },
    {
        id : views.SNAPSHOTS,
        component: Snapshots
    },
    {
        id : views.OBRS,
        component: Obrs
    },
    {
        id : views.SETTINGS,
        component: Settings
    },
    {
        id : views.HELPS,
        component: Helps
    }
]
