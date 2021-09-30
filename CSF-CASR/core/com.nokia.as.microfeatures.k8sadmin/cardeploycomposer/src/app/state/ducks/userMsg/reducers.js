import * as types from "./types";

// Updates error message to notify about the failed fetches.
function userMessage(state = null, action) {
    const { type, error, id } = action
    if (error) {
        const title = (id) ? id + ' error' : 'Operation error';
        const message = new Date().toString();
        const details = action.error;
        return {
            type: 'error',
            title: title,
            message: message,
            details: details
        }
    }
    switch (type) {
        case types.RESET_USER_MESSAGE:
            return null;
        case types.SET_INFO_MESSAGE:
        case types.SET_WARN_MESSAGE:
        case types.SET_ERROR_MESSAGE:
            return {...action.usermsg}

        default:
            return state
    }
}

export default userMessage
/*
With combine, this reducer becomes a child of the parent reducer named in state/ducks/index.js
export default combineReducers( {
    userMessage
} );
*/