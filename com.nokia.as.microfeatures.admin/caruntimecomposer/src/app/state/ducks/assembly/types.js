/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

// Basis actions types
export const CREATE_ASSEMBLY = 'assembly/CREATE_ASSEMBLY'
export const RESET_ASSEMBLY = 'assembly/RESET_ASSEMBLY'
export const PRESET_ASSEMBLY = 'assembly/PRESET_ASSEMBLY'

export const DELETE_ASSEMBLY = 'assembly/DELETE_ASSEMBLY'
export const CONFIRM_DELETE_ASSEMBLY = 'assembly/CONFIRM_DELETE_ASSEMBLY'
export const CANCEL_DELETE_ASSEMBLY = 'assembly/CANCEL_DELETE_ASSEMBLY'

export const SET_SELECTED_RUNTIME = 'assembly/SET_SELECTED_RUNTIME'

export const TOGGLE_FEATURE = 'assembly/TOGGLE_FEATURE'
export const SET_SELECTED_FEATURES = 'assembly/SET_SELECTED_FEATURES'

export const SET_SELECTED_QUICK = 'assembly/SET_SELECTED_QUICK'

export const OPENCLOSE_ASMBPANEL = 'assembly/OPENCLOSE_ASMBPANEL'

// Specifics actions types for requests
export const ASSEMBLY = createRequestTypes('assembly/CR-ASSEMBLY');
export const DELASSEMBLY = createRequestTypes('assembly/DEL-ASSEMBLY');