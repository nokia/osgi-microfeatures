/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import reducer from "./reducers";
import * as userMsgTypes from "./types";
import * as userMsgActions from "./actions";
import * as userMsgSelectors from "./selectors"

export {
    userMsgTypes,
    userMsgActions,
    userMsgSelectors
}
export default reducer;