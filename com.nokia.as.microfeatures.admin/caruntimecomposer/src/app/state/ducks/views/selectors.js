import { utils } from "../../../utilities"
const moduleKey = 'views'

export const getActiveView = (state) => state.activeView;

export default utils.globalizeSelectors({
    getActiveView
},moduleKey)