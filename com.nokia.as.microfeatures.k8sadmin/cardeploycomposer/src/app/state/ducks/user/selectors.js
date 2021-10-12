/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import { ADMINISTRATOR_ROLE } from './constants'
const moduleKey = 'user'

export const getCurrentUser = (state) => state;
export const getRoles = state => state.roles;
export const isLoggedIn = state => state.isLoggedIn;
export const isAdministrator = (state) => {
    let roles = getRoles(state);
    return roles.includes(ADMINISTRATOR_ROLE);
}

export default utils.globalizeSelectors({
    getCurrentUser,
    getRoles,
    isLoggedIn,
    isAdministrator
},moduleKey)