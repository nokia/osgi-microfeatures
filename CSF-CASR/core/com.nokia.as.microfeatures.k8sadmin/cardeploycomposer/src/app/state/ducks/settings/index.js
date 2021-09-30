import reducer from "./reducers";
import * as settingsTypes from "./types";
import * as settingsActions from "./actions";
import * as settingsConst from "./constants";
import { default as settingsSelectors } from "./selectors"

export {
    settingsConst,
    settingsTypes,
    settingsActions,
    settingsSelectors
}

export default reducer;