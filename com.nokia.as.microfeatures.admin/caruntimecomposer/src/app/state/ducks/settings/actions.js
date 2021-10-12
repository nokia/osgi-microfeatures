/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import * as types from "./types";

const { action } = utils;

export const setMenuOptions = (options) => action(types.SET_MENU_OPTIONS,{options})
export const setThemeOptions = (options) => action(types.SET_THEME_OPTIONS,{options})
export const setBrowseObrOptions =  (options) => action(types.SET_BROWSEOBR_OPTIONS,{options})
