/***************************************************************************
 *                                                                         *
 *                       Copyright (c) 2018, Nokia                         *
 *                                                                         *
 *                         All Rights Reserved                             *
 *                                                                         *
 *         This is unpublished proprietary source code of Nokia.           *
 *        The copyright notice above does not evidence any actual          *
 *              or intended publication of such source code.               *
 *                                                                         *
 ***************************************************************************/
import React, { PureComponent } from 'react';
import $ from "jquery"

import AppBanner from '@nokia-csf-uxr/csfWidgets/AppBanner';
import Button from '@nokia-csf-uxr/csfWidgets/Button';

import PropTypes from 'prop-types';

class PostLogoutPage extends PureComponent {
    static defaultProps = {
        banId: "PostLogoutBanner"
    }

    reload = () => {
        console.log( "reloading....window.location", window.location);
        window.location = window.location.href;       
    }

    componentDidMount() {
        // Remove default right part of AppBanner that display user info
        const rightPartNode = $('.user-account-summary-button');
        rightPartNode.remove();
    }

    render() {
        console.log("<PostLogoutPage /> props", this.props)
        return (
            <AppBanner
                id={this.props.banId}
                productFamily="CASR"
                productName="Composer"
                acctSettings=''
                userAccountSummaryUsername=''
            >
                <div style={{ textAlign: 'center' }}>
                    <h1>You have logged out of your account</h1>
                    <h2>Sign in again?</h2>
                    <Button id="reloadbtn" text="Login" onClick={this.reload} isCallToAction />
                </div>
            </AppBanner>
        )
    }
}

PostLogoutPage.propTypes = {
    banId : PropTypes.string.isRequired
}

export default PostLogoutPage
