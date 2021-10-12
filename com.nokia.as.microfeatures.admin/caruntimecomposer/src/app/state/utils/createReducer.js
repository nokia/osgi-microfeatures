/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export default ( initialState ) => ( reducerMap ) => ( state = initialState, action ) => {
    const reducer = reducerMap[ action.type ];
    return reducer ? reducer( state, action ) : state;
};
