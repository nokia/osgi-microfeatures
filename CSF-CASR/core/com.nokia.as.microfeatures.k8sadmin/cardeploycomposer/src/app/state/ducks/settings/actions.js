import { utils } from "../../../utilities"
import * as types from "./types";

const { action } = utils;

export const setMenuOptions = (options) => action(types.SET_MENU_OPTIONS,{options})
export const setThemeOptions = (options) => action(types.SET_THEME_OPTIONS,{options})
