/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
const moduleKey = 'settings'

export const getMenuType = (state) => state.menu.type;
export const getThemeOptions = (state) => state.theme;
export const getThemeName = (state) => state.theme.name;
export const getThemeStyle = (state) => state.theme.style;
export const getThemeColor = (state) => state.theme.color;
export const getThemeLogo = (state) => state.theme.logo;
export const isEnabledLocalObrActions = (state) => state.browseObr.displayLocalActions;



export default utils.globalizeSelectors({
    getMenuType,
    getThemeName,
    getThemeLogo,
    getThemeColor,
    getThemeStyle,
    isEnabledLocalObrActions
},moduleKey)