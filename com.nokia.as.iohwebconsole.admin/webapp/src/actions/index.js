import { default as utils } from '../tools/utils'
import * as types from "./actionTypes"

const { action } = utils;

// Start / stop activity
export const start2dActivity = () => action(types.START_2D);
export const stop2dActivity = () => action( types.STOP_2D);

// Display configuration
export const updateView = (view) => action(types.SET_VIEW, {view} );
export const updateFlux = (flux) => action(types.SET_FLUX,{flux});

// User message management
export const setInfoMessage = (title,message) => {
  const usermsg = { type:'info', title:title, message:message, details:null }
  return action(types.SET_INFO_MESSAGE,{usermsg})
}
export const setWarnMessage = (title,warn1,warn2) => {
  const usermsg = { type:'warn', title:title, message:warn1, details:warn2 }
  return action(types.SET_WARN_MESSAGE,{usermsg})
}
export const setErrorMessage = (title,error,details) => {
  const message = (error)?error:new Date().toString();
  const usermsg = { type:'error', title:title, message:message, details:details }
  return action(types.SET_ERROR_MESSAGE,{usermsg})
}
export const resetUserMessage = () => action(types.RESET_USER_MESSAGE)
