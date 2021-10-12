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

// Specifics actions types for requests
export const OBRS = createRequestTypes('entities/OBRS');
export const FEATURES = createRequestTypes('entities/FEATURES');
export const ASSEMBLIES = createRequestTypes('entities/ASSEMBLIES');
export const SNAPSHOTS = createRequestTypes('entities/SNAPSHOTS');
export const RELOADREPOS = createRequestTypes('entities/RELOADREPOS');