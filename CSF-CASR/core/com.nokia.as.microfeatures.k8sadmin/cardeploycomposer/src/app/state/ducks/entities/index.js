import reducer from "./reducers";
import * as entitiesTypes from "./types";
import * as entitiesOperations from "./sagas";
import { default as entitiesSelectors } from "./selectors"



export {
    entitiesTypes,
    entitiesSelectors,
    entitiesOperations
}

export default reducer;