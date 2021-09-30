import React from 'react';
import { render } from "react-dom";
import { Provider } from "react-redux";

import configureStore from './app/state/store';
import rootSaga from './app/sagas'

import App from './app/views/layouts/App'
import Loading from './app/views/layouts/Loading'
//import registerServiceWorker from './registerServiceWorker';

import './index.css';

const store = configureStore();
store.runSaga(rootSaga);

const RootHtml = () => {
    if( process.env.NODE_ENV !== 'production')
        console.log("STARTING IN DEVELOPMENT MODE")

    return (
        <Provider store={store}>
            <App />
        </Provider>
    )
};
render(<RootHtml />, document.getElementById('root'));
// Render for loading
render(<Provider store={store}><Loading/></Provider>, document.getElementById('loading'));

//registerServiceWorker();
