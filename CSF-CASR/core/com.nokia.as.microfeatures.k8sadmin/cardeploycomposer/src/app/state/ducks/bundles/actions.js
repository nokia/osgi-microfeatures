import { utils } from "../../../utilities"
import { GETBUNDLES,
    LOAD_BUNDLES,
    RESOLVE_BUNDLE,
    BUNDLERESOLUTION,
    FIND_DEPS_BUNDLE,
    DEPENDENTBUNDLE,
    CLOSE_BUNDLE_PANEL
 } from "./types";

const { msgTypes, action } = utils;
const { REQUEST, SUCCESS, FAILURE } = msgTypes

// Basis actions
// Load bundles from an obr url , local = true means local obr
export const loadBundles = () => action(LOAD_BUNDLES)
// export const loadBundles = (url,local) => action(LOAD_BUNDLES,{url},{local})

export const resolveBundle = (id) => action(RESOLVE_BUNDLE,{id})
export const findDepsBundle = (id) => action(FIND_DEPS_BUNDLE,{ id })

export const closeBundlePanel = () => action(CLOSE_BUNDLE_PANEL)


// Specific actions for async request
export const bundles = {
    request: (id,payload) => action(GETBUNDLES[REQUEST], payload),
    success : (id,response,params) => action(GETBUNDLES[SUCCESS], {response},{params}),
    failure: (id,error,params) => action(GETBUNDLES[FAILURE], {error},{id})
}

export const bresolution = {
    request: (id,payload) => action(BUNDLERESOLUTION[REQUEST], payload),
    success : (id,response) => action(BUNDLERESOLUTION[SUCCESS], {response}),
    failure: (id,error)=> action(BUNDLERESOLUTION[FAILURE], {error},{id})
}

export const bdependent = {
    request: (id,payload) => action(DEPENDENTBUNDLE[REQUEST], payload),
    success : (id,response) => action(DEPENDENTBUNDLE[SUCCESS], {response}),
    failure: (id,error)=> action(DEPENDENTBUNDLE[FAILURE], {error},{id})
}
