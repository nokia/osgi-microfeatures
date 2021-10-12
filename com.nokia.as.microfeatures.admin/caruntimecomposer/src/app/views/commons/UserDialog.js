/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import PropTypes from 'prop-types';

import AlertDialogInfo from '@nokia-csf-uxr/csfWidgets/AlertDialogInfo';
import AlertDialogWarning from '@nokia-csf-uxr/csfWidgets/AlertDialogWarning';
import AlertDialogError from '@nokia-csf-uxr/csfWidgets/AlertDialogError';

export default class UserDialog extends Component {
    static propTypes = {
        id: PropTypes.string,
        type: PropTypes.oneOf(['info', 'warn', 'error']).isRequired,
        title: PropTypes.string.isRequired,
        message: PropTypes.string.isRequired,
        details: PropTypes.string,
        onClose: PropTypes.func.isRequired,
    };

    static defaultProps = {
        id: 'userMsg',
    };

    constructor(props) {
        super(props);
        console.log("<UserDialog /> props", props)
    }

    renderInfo = () => {
        if (this.props.type !== 'info') return null;
        return (
            <AlertDialogInfo
                id={this.props.id}
                title={this.props.title}
                infoText={this.props.message}
                onClose={this.props.onClose}
                trapFocus={false}
//                theme={'white'}
            />
        )
    }

    renderWarn = () => {
        if (this.props.type !== 'warn') return null;
        return (
            <AlertDialogWarning
                id={this.props.id}
                title={this.props.title}
                warningText1={this.props.message}
                warningText2={this.props.details}
                onClose={this.props.onClose}
                trapFocus={false}
            />
        )
    }

    renderError() {
        if (this.props.type !== 'error') return null;
        return (
            <AlertDialogError
                id={this.props.id}
                title={this.props.title}
                errorText={this.props.message}
                detailsText={this.props.details}
                showDetailsLabel={'Show details'}
                details={true}
                onClose={this.props.onClose}
            />
        )
    }

    render() {
        return (
            <div>
                {this.renderInfo()}
                {this.renderWarn()}
                {this.renderError()}
            </div>
        )
    }
}