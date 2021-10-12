/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import reducer from "./reducers";
import * as viewsTypes from "./types";
import * as viewsActions from "./actions";
import * as viewsConst from "./constants";
import { default as viewsSelectors } from "./selectors"

export {
    viewsConst,
    viewsTypes,
    viewsActions,
    viewsSelectors
}

export default reducer;