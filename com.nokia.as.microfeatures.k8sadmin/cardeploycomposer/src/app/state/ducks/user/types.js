/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

export const REQ_USER_INFO = 'REQ_USER_INFO';
export const LOGOUT = 'user/LOGOUT';

// Specifics actions types for requests
export const USERINFO = createRequestTypes('user/USERINFO');
export const DISCONNECT = createRequestTypes('user/DISCONNECT');