import * as types from "../actions/actionTypes";

const configReducer = (state=types.defaultConfig, action) => {
    switch (action.type) {
        case types.SET_VIEW:
            return { ...state, view : action.view };
        case types.SET_FLUX:
            return { ...state, flux : action.flux };
        default :
            return state;
    }
}

export default configReducer;