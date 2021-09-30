import { utils } from "../../../utilities"
import * as types from "./types";

const { action } = utils;

export const startLoading = (msg) => action(types.START_LOADING,{msg})
export const stopLoading = () => action(types.STOP_LOADING)
export const hideWelcome = () => action(types.HIDE_WELCOME)