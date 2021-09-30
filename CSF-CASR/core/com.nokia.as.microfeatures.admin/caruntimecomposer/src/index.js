import React from 'react';
import { render } from "react-dom";
import { Provider as ReduxProvider } from "react-redux";

import configureStore from './app/state/store';
import rootSaga from './app/sagas'

import App from './app/views/layouts/App'
//import registerServiceWorker from './registerServiceWorker';

import './index.css';

const store = configureStore();
store.runSaga(rootSaga);

const RootHtml = () => {
    if( process.env.NODE_ENV !== 'production')
        console.log("STARTING IN DEVELOPMENT MODE")

    return (
        <ReduxProvider store={store}>
            <App />
        </ReduxProvider>
    )
};
render(<RootHtml />, document.getElementById('root'));

//registerServiceWorker();
