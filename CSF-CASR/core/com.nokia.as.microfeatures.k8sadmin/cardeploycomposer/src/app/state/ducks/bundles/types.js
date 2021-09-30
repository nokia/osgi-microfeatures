import { utils } from "../../../utilities"
const { createRequestTypes } = utils;

// Basis actions types
export const LOAD_BUNDLES = 'bundles/LOAD_BUNDLES'

export const RESOLVE_BUNDLE = 'bundles/RESOLVE_BUNDLE'
export const FIND_DEPS_BUNDLE = 'bundles/FIND_DEPS_BUNDLE'

export const CLOSE_BUNDLE_PANEL = 'bundles/CLOSE_BUNDLE_PANEL'

// Specifics actions types for requests
export const GETBUNDLES = createRequestTypes('bundles/GET_BUNDLES');
export const BUNDLERESOLUTION = createRequestTypes('bundles/BUNDLE_RESOLUTION');
export const DEPENDENTBUNDLE = createRequestTypes('bundles/DEPENDENT_BUNDLE');