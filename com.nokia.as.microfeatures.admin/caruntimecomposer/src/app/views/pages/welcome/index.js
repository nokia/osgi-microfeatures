/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';

import Label from '@nokia-csf-uxr/csfWidgets/Label';

import { InitialView as defaultView } from '../../../state/ducks/views/constants'

class Welcome extends Component {

    state = { error: false }

    componentWillUpdate(nextProps) {
        if( this.props.isWelcome === true && nextProps.isWelcome === false) {
            // trigger a route-change with react-router
            this.props.history.push({
                pathname: `/${defaultView}`
              });
        } else {
            if( this.props.userMessage === null && nextProps.userMessage !== null && nextProps.userMessage.type === 'error') {
                this.setState({error: true})
            } 
        }

    }

    render() {
        console.log("<Welcome /> props", this.props)
        return (
            <div style={{ textAlign: 'center' }}>
                <h1>Welcome to the Runtime Composer</h1>
                {this.props.isLoading === true &&
                    <div>
                        <Label text={'Please wait...'} />
                    </div>
                }
                { this.state.error === true &&
                    <h2>Please retry later...</h2>
                }
            </div>
        )
    }
}

export default Welcome;
