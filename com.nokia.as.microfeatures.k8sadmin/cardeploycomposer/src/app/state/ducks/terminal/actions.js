/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { utils } from "../../../utilities"
import { OPEN_TERMINAL, CLOSE_TERMINAL, START_TERMINAL } from "./types";

const { action } = utils;

// Basis actions
// Open a terminal for a runtime
export const startTerminal = (id,pod,url) => action( START_TERMINAL,{ id }, { pod:pod, url:url })
export const openTerminal = () => action( OPEN_TERMINAL )
export const closeTerminal = () => action( CLOSE_TERMINAL )