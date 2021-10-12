/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';

const styles = {
    button: {
        fontWeight: 'bold',
        fontSize: '15px'
    },
    div: {
        textAlign: 'center'
    },
    message: {
        fontSize: '18px'
    }
};

/**
 * Page where new user can request access to tenants.
 */
export default class AccessDenied extends Component {

    //  #region Lifecycle

    render () {
        return (
            <div style={styles.div}>
                <h1>Access denied</h1>
                <p style={styles.message}>You do not have the required permissions to perform this operation.</p>
            </div>
        );
    }

    //  #endregion

}