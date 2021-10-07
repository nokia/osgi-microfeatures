import { combineReducers } from "redux";
import * as types from "./types";
import { InitialMenu, InitialTheme } from "./constants"
import { createReducer } from "../../utils";
import Cookies from 'universal-cookie';
const cookies = new Cookies();

const getDefaultMenuOption = () => {
    var cookieVal = cookies.get('menuOptions');
    const result = { ...InitialMenu, ...cookieVal }
    console.log('getDefaultMenuOption',cookieVal,result)
    return result;
}

const getDefaultThemeOption = () => {
    var cookieVal = cookies.get('themeOptions');
    const result = { ...InitialTheme, ...cookieVal }
    console.log('getDefaultThemeOption',cookieVal,result)
    return result;
}

const menu = createReducer(getDefaultMenuOption())({
    [types.SET_MENU_OPTIONS]: (state, action) => {
        const result = { ...state, ...action.options }
        cookies.set('menuOptions', result);
        return result;
    }
});
const theme = createReducer(getDefaultThemeOption())({
    [types.SET_THEME_OPTIONS]: (state, action) => {
        const result = { ...state, ...action.options }
        cookies.set('themeOptions', result);
        return result;
    }
});

export default combineReducers({
    menu,
    theme
});