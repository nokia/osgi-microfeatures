import React, { Component } from 'react';

import AppToolbarNew from '@nokia-csf-uxr/csfWidgets/AppToolbarNew';
import AboutProduct from '@nokia-csf-uxr/csfWidgets/AboutProduct';
import { version as appVersion } from '../../../../package.json'
import { version } from '@nokia-csf-uxr/csfWidgets/package.json';


class ToolBar extends Component {
    state = { open: false };
    _handleClick = (event) => {
        console.log('AppToolbarNew', event)
        this.setState({ open: true })
    }

    render() {
        const title = { pageTitle: this.props.pageTitle }
        const UXRVersion = "CCFK -NPM: " + version;

        return (
            <div>
                <AppToolbarNew
                    title={title}
                    iconButtons={[
                        {
                            icon: 'ic_info',
                            value: 'info',
                            toolTip: { text: 'About' },
                            eventData: { value: "About" },
                            onClick: this._handleClick
                        }
                    ]}
                />

                {this.state.open === true &&
                    <AboutProduct
                        id="aboutProduct"
                        title="About"
                        productName={'Application Runtime Composer'}
                        releaseNumber={appVersion}
                        subReleaseNumber={UXRVersion}
                        copyrightYear={'2019'}
                        termsAndConditionsText="Terms and Conditions"
                        termsAndConditionsTextLink="https://nokia.com"
                        onClose={() => this.setState({ open: false })}
                        trapFocus={false}
                        theme="black"
                    />
                }
            </div>
        )
    }
}

export default ToolBar;
