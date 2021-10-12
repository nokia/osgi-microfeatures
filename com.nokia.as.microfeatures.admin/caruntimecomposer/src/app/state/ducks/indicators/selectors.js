/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
const moduleKey = 'indicators'

export const isLoading = (state) => state.loading.isLoading
export const loadingMsg = (state) => state.loading.msg
export const isWelcome = (state) => state.loading.welcome

export default utils.globalizeSelectors({
    isLoading,
    isWelcome,
    loadingMsg
},moduleKey)