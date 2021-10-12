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
export const CREATE_SNAPSHOT = 'snapshot/CREATE_SNAPSHOT'
export const DELETE_SNAPSHOT = 'snapshot/DELETE_SNAPSHOT'
export const CONFIRM_DELETE_SNAPSHOT = 'snapshot/CONFIRM_DELETE_SNAPSHOT'
export const CANCEL_DELETE_SNAPSHOT = 'snapshot/CANCEL_DELETE_SNAPSHOT'
export const RESET_OPERATION = 'snapshot/RESET_OPERATION'

// Specifics actions types for requests
export const CRSNAPSHOT = createRequestTypes('snapshot/CR-SNAPSHOT');
export const DELSNAPSHOT = createRequestTypes('snapshot/DEL-SNAPSHOT');