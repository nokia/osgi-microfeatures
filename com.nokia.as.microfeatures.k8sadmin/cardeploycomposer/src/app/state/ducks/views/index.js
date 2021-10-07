import reducer from "./reducers";
import * as viewsTypes from "./types";
import * as viewsActions from "./actions";
import * as viewsConst from "./constants";
import { default as viewsSelectors } from "./selectors"

export {
    viewsConst,
    viewsTypes,
    viewsActions,
    viewsSelectors
}

export default reducer;