import { combineReducers } from 'redux';
import isStarted2d from './d2Reducer'
import config from './configReducer'
import userMessage from './userMsgReducer'

const rootReducer = combineReducers({
    isStarted2d,
    config,
    userMessage
});

export default rootReducer;