/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React from 'react';
import { connect } from 'react-redux';

import FormLayout from '@nokia-csf-uxr/csfWidgets/FormLayout';

import { settingsConst, settingsActions, settingsSelectors } from '../../../state/ducks/settings'

import BrandingSettings from '../settings/BrandingSettings'
import MenuSettings from './MenuSettings'
import BrowseObrSettings from './BrowseObrSettings'
import ToolBar from '../../commons/ToolBar';

const { THEMES } = settingsConst

class Settings extends React.Component {

    selectPrimaryColor = (newColor) => {
        this.props.setThemeOptions({
            name: 'Custom',
            color: newColor
        });
    }

    selectLogo = (newLogo) => {
        this.props.setThemeOptions({
            name: 'Custom',
            logo: newLogo
        });
    }

    selectTheme = (newTheme) => {
        console.log("selectTheme",this.props.themeName,newTheme)
        this.props.setThemeOptions(THEMES[newTheme]);
    }

    selectTextAndIconStyle = (newStyle) => {
        this.props.setThemeOptions({
            name: 'Custom',
            style: newStyle
        });
    }

    selectMenuType = (type) => {
        this.props.setMenuOptions({ type: type })
    }

    updateEnabledLocalObrActions = (value) => {
        this.props.setBrowseObrOptions({ displayLocalActions: value })
    }

    render() {
        console.log("<Settings /> props", this.props)
        return (
            <div style={{ height: this.props.height, position: 'relative', minWidth: '588px' }} >
                <ToolBar pageTitle={'Preferences'} />
                <FormLayout>
                    <BrandingSettings
                        onPrimaryColorSelect={this.selectPrimaryColor}
                        onLogoSelect={this.selectLogo}
                        onTextAndIconStyleSelect={this.selectTextAndIconStyle}
                        onThemeSelect={this.selectTheme}
                        selectedLogo={this.props.themeLogo}
                        selectedPrimaryColor={this.props.themeColor}
                        selectedTextAndIconStyle={this.props.themeStyle}
                        selectedTheme={this.props.themeName}
                    />
                    <MenuSettings
                        onTypeSelect={this.selectMenuType}
                        selectedType={this.props.menuType}
                    />
                    <BrowseObrSettings
                        onChangeEnableLocalActions={this.updateEnabledLocalObrActions}
                        displayLocalActions={this.props.isEnabledLocalObrActions}
                    />
                </FormLayout>
            </div>

        )
    }
}

const mapStateToProps = function (state) {
    console.log("App state=", state);
    return {
        isEnabledLocalObrActions: settingsSelectors.isEnabledLocalObrActions(state)
    }
}


const mapDispatchToProps = {
    ...settingsActions
}

export default connect(mapStateToProps, mapDispatchToProps)(Settings);