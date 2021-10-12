/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import reducer from "./reducers";
import * as routeTypes from "./types";
import * as routeOperations from "./sagas";
import { default as routeSelectors } from "./selectors"

export {
    routeTypes,
    routeOperations,
    routeSelectors
}

export default reducer;