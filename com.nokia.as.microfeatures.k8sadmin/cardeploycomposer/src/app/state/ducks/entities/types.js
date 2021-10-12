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
export const REQ_DATA = 'REQ_DATA'

// Action to select an OBR among remote OBRs ( i.e not localhost ) to be loaded
export const SELECT_OBR = 'SELECT_OBR'

// Start/stop polling to get runtimes list
export const POLL_START_RUNTIMES = 'entities/POLL_START_RUNTIMES'
export const POLL_STOP_RUNTIMES = 'entities/POLL_STOP_RUNTIMES'

// Start/stop polling to get routes list
export const POLL_START_ROUTES = 'entities/POLL_START_ROUTES'
export const POLL_STOP_ROUTES = 'entities/POLL_STOP_ROUTES'

// Start/stop polling to get functions list
export const POLL_START_FUNCTIONS = 'entities/POLL_START_FUNCTIONS'
export const POLL_STOP_FUNCTIONS = 'entities/POLL_STOP_FUNCTIONS'

// Specifics actions types for requests
export const OBRS = createRequestTypes('entities/OBRS');
export const FEATURES = createRequestTypes('entities/FEATURES');
export const RUNTIMES = createRequestTypes('entities/RUNTIMES');
export const ROUTES = createRequestTypes('entities/ROUTES');
export const FUNCTIONS = createRequestTypes('entities/FUNCTIONS');
export const RELOADREPOS = createRequestTypes('entities/RELOADREPOS');