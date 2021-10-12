/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';

import AppToolbarNew from '@nokia-csf-uxr/csfWidgets/AppToolbarNew';
import AboutProduct from '@nokia-csf-uxr/csfWidgets/AboutProduct';
import { version as appVersion } from '../../../package.json'
import { version } from '@nokia-csf-uxr/csfWidgets/package.json';
import * as Labels from '../../labels';


class ToolBar extends Component {
    state = { open: false };
    _handleClick = (event) => {
        console.log('AppToolbarNew', event)
        this.setState({ open: true })
    }

    _handleCloseAbout = () => this.setState({ open: false });

    componentWillMount() {
        console.info("<ToolBar/> -MOUNTED-")
    }

    componentWillUnmount() {
        console.info("<ToolBar/> UNMOUNTED! ")
    }



    render() {
        console.log("<ToolBar props/>", this.props);
        const title = { pageTitle: this.props.pageTitle }
        if (this.props.subTitle)
            title.subTitle = this.props.subTitle;

        let iconButtons = [{
            icon: 'ic_info',
            tooltip: { text: "About" },
            eventData: { value: "About" },
            onClick: this._handleClick
        }];
        if (this.props.iconButtons) {
            iconButtons = this.props.iconButtons.concat(iconButtons);
            console.log("iconButtons",iconButtons)
        }

        const UXRVersion = "CCFK -NPM: " + version;

        return (
            <div>
                <AppToolbarNew {...this.props}
                    title={title}
                    iconButtons={iconButtons}
                />

                {this.state.open === true &&
                    <AboutProduct
                        id="aboutProduct"
                        title="About"
                        productName={Labels.LABEL_PRODUCT_NAME}
                        releaseNumber={appVersion}
                        subReleaseNumber={UXRVersion}
                        copyrightYear={'2018'}
                        termsAndConditionsText="Terms and Conditions"
                        termsAndConditionsTextLink="https://nokia.com"
                        onClose={this._handleCloseAbout}
                        trapFocus={false}
                        theme="black"
                    />
                }
            </div>
        )
    }
}

export default ToolBar;
