import { utils } from "../../../utilities"
import { USERINFO , DISCONNECT} from "./types";
import { InitialUserInfoData } from "./constants"
import { createReducer } from "../../utils";

const { msgTypes } = utils;
const { SUCCESS, FAILURE } = msgTypes

const userData = createReducer( InitialUserInfoData )( {
    [ USERINFO[SUCCESS] ] : ( state, action ) => {
        console.log(USERINFO[SUCCESS] + " 1)action.response.entities",action.response.entities)
        return { ...state, ...action.response.entities, fetchedUser: true };
    },
    [ USERINFO[FAILURE] ] : ( state, action ) => ({...state, fetchedUser: true} ),
    [ DISCONNECT[SUCCESS] ] : () => ({ ...InitialUserInfoData, isLoggedIn: false } ),
    [ DISCONNECT[FAILURE] ] : () => ({ ...InitialUserInfoData, isLoggedIn: false } )
});

export default userData;