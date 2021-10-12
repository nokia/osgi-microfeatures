/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import reducer from "./reducers";
import * as entitiesTypes from "./types";
import * as entitiesOperations from "./sagas";
import { default as entitiesSelectors } from "./selectors"



export {
    entitiesTypes,
    entitiesSelectors,
    entitiesOperations
}

export default reducer;