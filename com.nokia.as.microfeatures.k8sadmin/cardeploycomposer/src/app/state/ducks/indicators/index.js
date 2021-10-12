/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import reducer from "./reducers";
import * as indicatorsTypes from "./types";
import * as indicatorsActions from "./actions";
import { default  as indicatorsSelectors } from "./selectors"

export {
    indicatorsTypes,
    indicatorsActions,
    indicatorsSelectors
}

export default reducer;