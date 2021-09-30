import { combineReducers } from "redux";
import * as types from "./types";
import { createReducer } from "../../utils";
import { utils } from "../../../utilities";
import { merge } from 'lodash'

const { msgTypes, parseBundles } = utils;

const initialStates = {
    op: null,
    state: null
}

const { SUCCESS, FAILURE } = msgTypes

const local = createReducer({})({
    [types.GETBUNDLES[SUCCESS]]: (state, action) => {
        const { local, url } = action.params;
        if (local !== true) return state;
        let bundles = {}
        try {
            bundles = parseBundles(action.response)
        } catch (e) {
            console.log("parseBundles failed", url, e)
        }
        return bundles;
    }
});

const current = createReducer({})({
    [types.GETBUNDLES[SUCCESS]]: (state, action) => {
        const { local, url } = action.params;
        if (local === true) return state;
        let bundles = {}
        try {
            bundles = parseBundles(action.response)
        } catch (e) {
            console.log("parseBundles failed", url, e)
        }
        return bundles;
    }
});

const doNotStoreOnCancelledOperation = function (state, status) {
    // When the bundle panel has been closed before the end of the bundle operation,
    // the result should not be stored.
    if (state.op !== null && state.state != null)
        return { ...state, state: status };

    return initialStates;
}

export const states = createReducer(initialStates)({
    [types.RESOLVE_BUNDLE]: (state, action) => ({ op: 'RESOLVE', state: 'started' }),
    [types.BUNDLERESOLUTION[SUCCESS]]: (state, action) => doNotStoreOnCancelledOperation(state, 'ok'),
    [types.BUNDLERESOLUTION[FAILURE]]: (state, action) => doNotStoreOnCancelledOperation(state, 'error'),
    [types.FIND_DEPS_BUNDLE]: (state, action) => ({ op: 'FIND', state: 'started' }),
    [types.DEPENDENTBUNDLE[SUCCESS]]: (state, action) => doNotStoreOnCancelledOperation(state, 'ok'),
    [types.DEPENDENTBUNDLE[FAILURE]]: (state, action) => doNotStoreOnCancelledOperation(state, 'error'),
    [types.CLOSE_BUNDLE_PANEL]: (state, action) => (initialStates)
});

const bundleId = createReducer(null)({
    [types.RESOLVE_BUNDLE]: (state, action) => action.id,
    [types.FIND_DEPS_BUNDLE]: (state, action) => action.id,
    [types.CLOSE_BUNDLE_PANEL]: (state, action) => null
});

const resolveds = createReducer([])({
    [types.BUNDLERESOLUTION[SUCCESS]]: (state, action) => {
        console.log("resolveds action", action)
        return merge([], action.response.entities.resources);
    },
    [types.CLOSE_BUNDLE_PANEL]: (state, action) => []
});

const dependents = createReducer([])({
    [types.DEPENDENTBUNDLE[SUCCESS]]: (state, action) => {
        console.log("dependents action", action)
        return merge([], action.response.entities.resources);
    },
    [types.CLOSE_BUNDLE_PANEL]: (state, action) => []
});

export default combineReducers({
    local,
    current,
    states,
    bundleId,
    resolveds,
    dependents
});



/*

THIS PART WORKS WELL. The shape is :
bundles : {
    obrs : {
        http://.... : {
            state : 'ok',
            url : http://....
            bundles : {
                com.file@1.0.0 : { bsn: 'com.file', version: '1.0.0'}
            }
        }
    }
}
Remark: the obrs object hold keys as url , not notion of local and current.

const obrs = createReducer({})({
    [types.GETBUNDLES[REQUEST]]: (state, action) => {
//        console.log("types.GETBUNDLES[REQUEST]", action)
        const url = action.url;
        let newState = Object.assign({},state)
        newState[url] = { state : 'started' , url :url};
        return newState;

    },
    [types.GETBUNDLES[SUCCESS]]: (state, action) => {
        const { local, url } = action.params;
        let newState = Object.assign({},state)
        const bundles = parseBundles(action.response)
        newState[url] = Object.assign({},newState[url], { bundles: bundles ,state: 'ok' })
        return newState;
    },
    [types.GETBUNDLES[FAILURE]]: (state, action) => {
        const { url } = action.params;
        let newState = Object.assign({},state)
        newState[url] = Object.assign({},newState[url], { state: 'error' })
        return newState;
    }

});

export default combineReducers({
    obrs
});

*/