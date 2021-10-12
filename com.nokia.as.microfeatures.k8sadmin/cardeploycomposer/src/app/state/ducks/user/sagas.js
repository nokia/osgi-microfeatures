/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/* eslint-disable no-constant-condition */
import { call, select } from 'redux-saga/effects'
import { api } from '../../../services'
import {
    requestUserInfoData,
    userinfodata,
    logout,
    disconnectdata

} from './actions'
import { utils } from "../../../utilities"
import { default as selectors } from './selectors'

const { fetchEntity } = utils;

const fetchUserInfoData = fetchEntity.bind(null, userinfodata, api.fetchUserInfoData);
const fetchDisconnectData = fetchEntity.bind(null, disconnectdata, api.fetchDisconnectData);

//
// GENERATORS
//

// load user date unless it is cached
export function* loadUserInfoData() {
    const userData = yield select(selectors.getCurrentUser)
    console.log("loadUserInfoData",userData);
    if (!userData || (!userData.preferredUsername && !userData.name )) {
        console.log("loadUserInfoData calling fetchUserInfoData");
        yield call(fetchUserInfoData, 'User Info Session load')
    }
    // Returns the user info
    return yield select(selectors.getCurrentUser);
}


export function* doDisconnect() {
    yield call(fetchDisconnectData, 'Session disconnecting')
}


export {
    requestUserInfoData,
    logout
}
