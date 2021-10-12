/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React from 'react';
import ReactDOM from 'react-dom';
import configureStore from './store/configureStore';
import MainAppContainer from './containers/MainAppContainer';
import {Provider} from 'react-redux';
import * as serviceWorker from './serviceWorker';


import './index.css';
import '@nokia-csf-uxr/csfWidgets/csfWidgets.css';
//import 'vis/dist/vis-network.min.css'
import 'vis/dist/vis.min.css';

const store = configureStore();

ReactDOM.render(
    <Provider store={store}>
        <MainAppContainer />
    </Provider>,
    document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: http://bit.ly/CRA-PWA
serviceWorker.unregister();
