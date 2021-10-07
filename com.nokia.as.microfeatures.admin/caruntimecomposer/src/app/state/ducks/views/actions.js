import { utils } from "../../../utilities"
import * as types from "./types";

const { action } = utils;

export const setView = (view) => action(types.SET_VIEW,{view})