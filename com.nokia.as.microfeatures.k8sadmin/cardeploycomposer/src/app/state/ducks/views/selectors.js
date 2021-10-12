/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
const moduleKey = 'views'

export const getActiveView = (state) => state.activeView;

export default utils.globalizeSelectors({
    getActiveView
},moduleKey)