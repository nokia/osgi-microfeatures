/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import reducer from "./reducers";
import * as terminalTypes from "./types";
import * as terminalActions from "./actions";
import { default as terminalSelectors } from "./selectors"

export {
    terminalTypes,
    terminalActions,
    terminalSelectors
}
export default reducer;