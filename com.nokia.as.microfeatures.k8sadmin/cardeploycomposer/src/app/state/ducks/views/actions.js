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

export const setView = (view) => action(types.SET_VIEW,{view})