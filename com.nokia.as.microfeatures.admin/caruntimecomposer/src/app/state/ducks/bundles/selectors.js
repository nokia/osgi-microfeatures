/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { createSelector } from 'reselect'
import { utils } from "../../../utilities"
const moduleKey = 'bundles'


export const getLocalBundles = state => state.local
export const getCurrentBundles = state => state.current
export const isResolveOperation = state => (state.states.op === 'RESOLVE')
export const isOpenPanel = state => (state.states.state === 'ok')
export const isBundleOpStarted = state => (state.states.state === 'started')
export const getBundleId = state => state.bundleId
export const getBundleById = (state, id) => (state.local[id] !== undefined) ? state.local[id] : state.current[id]
export const getResolveds = state => state.resolveds
export const getDependents = state => state.dependents



const comparator = (f1, f2) => ((f1.fid > f2.fid) ? 1 : ((f2.fid > f1.fid) ? -1 : 0))

export const getAllLocalBundles = createSelector(
    getLocalBundles,
    (bundlesMap) => Object.values(bundlesMap).sort(comparator)
)

export const getAllCurrentBundles = createSelector(
    getCurrentBundles,
    (bundlesMap) => Object.values(bundlesMap).sort(comparator)
)

export const getBundleForOp = (state) => getBundleById(state, getBundleId(state))

export const getOpRessources = createSelector(
    state => state.states.op,
    getResolveds,
    getDependents,
    (op, resolveds, dependents) => {
        if (op === null) return []
        if (op === 'RESOLVE') return resolveds
        return dependents
    }
)


export default utils.globalizeSelectors({
    getLocalBundles,
    getAllLocalBundles,
    getCurrentBundles,
    getAllCurrentBundles,
    getBundleForOp,
    isResolveOperation,
    isOpenPanel,
    isBundleOpStarted,
    //    getResolveds,
    //   getDependents
    getOpRessources
}, moduleKey)
