/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import { connect } from "react-redux";

// CCFK components / images
import AppBanner from '@nokia-csf-uxr/csfWidgets/AppBanner';
import icon_dashboard from '@nokia-csf-uxr/csfWidgets/images/ic_dashboard.svg';
import icon_features from '@nokia-csf-uxr/csfWidgets/images/ic_matrix.svg';
import icon_runtimes from '@nokia-csf-uxr/csfWidgets/images/ic_rack.svg';
import icon_routes from '@nokia-csf-uxr/csfWidgets/images/ic_managed_routes.svg';
import icon_functions from '@nokia-csf-uxr/csfWidgets/images/ic_health.svg';
import icon_browseobrs from '@nokia-csf-uxr/csfWidgets/images/ic_Constraints.svg';
import icon_settings from '@nokia-csf-uxr/csfWidgets/images/ic_settings.svg';
import icon_help from '@nokia-csf-uxr/csfWidgets/images/ic_help.svg';

import { viewsConst } from '../../state/ducks/views'
import { settingsSelectors } from "../../state/ducks/settings";
import { userOperations } from "../../state/ducks/user";
import { viewsActions } from "../../state/ducks/views";

// configure 'Side Drawer Navigation' actions, clicking on an action uses react-routing to go to that particular view
const sideDrawerItemGroups = [
  {
    group: [
      {
        name: 'Dashboard',
        icon: icon_dashboard,
        action: viewsConst.views.DASHBOARD,
      }
    ]
  },
  {
    group: [
      {
        name: 'Features',
        icon: icon_features,
        action: viewsConst.views.FEATURES,
      },
      {
        name: 'Runtimes',
        icon: icon_runtimes,
        action: viewsConst.views.RUNTIMES,
      }
    ]
  },
  {
    group: [
      {
        name: 'Routes',
        icon: icon_routes,
        action: viewsConst.views.ROUTES,
      },
      {
        name: 'Functions',
        icon: icon_functions,
        action: viewsConst.views.FUNCTIONS,
      }
    ]
  },
  {
    group: [
      {
        name: 'Browse Obrs',
        icon: icon_browseobrs,
        action: viewsConst.views.OBRS,
      }
    ]
  },
];

const sideDrawerUniversalItems = [
  {
    name: 'Preferences',
    icon: icon_settings,
    action: viewsConst.views.SETTINGS
  },
  {
    name: 'Help',
    icon: icon_help,
    action: viewsConst.views.HELPS
  }
];

class NavBanner extends Component {
  static defaultProps = {
    banId: "MyAppBanner"
  }

  state = {
    roleLabel: (this.props.isAdministrator)?"Administrator" : "Viewer"
  }


  onSideDrawerItemClick = (props) => {
    const { menuItem } = props
    if( menuItem === undefined || menuItem.action === undefined ) {
      console.warn("onSideDrawerItemClick no action! (internal csfWidgets bug)");
      return;
    }
    // trigger a route-change with react-router
    const pathname = `/${menuItem.action}`
    console.log('onSideDrawerItemClick menuItem', menuItem, pathname, props)
    this.props.setView(menuItem.action)
    this.props.changePath({
      pathname: pathname
    });
  }

  render() {
    console.log('<NavBanner/> props', this.props)
      const { preferredUsername, name } = this.props;
      const { roleLabel } = this.state;
      return (<AppBanner
        id={this.props.banId}
        productFamily="CASR"
        productName="Composer"
        acctSettings=''
        userAccountSummaryUsername={ preferredUsername || name}
        userAccountSummaryUserRole={roleLabel}
        userAccountSummaryShowAccountSettingsLink={false}
        userAccountSummaryOnLogoutButtonClick={this._handleLogout}

        bannerColor={this.props.themeColor}
        logo={this.props.themeLogo}
        textAndIconStyle={this.props.themeStyle}

        sideDrawerItemGroups={sideDrawerItemGroups}
        sideDrawerUniversalItems={sideDrawerUniversalItems}
        sideDrawerType={this.props.menuType}
        sideDrawerButtonVisible={true}
        sideDrawerOnItemClick={this.onSideDrawerItemClick}
      >
        {this.props.children}
      </AppBanner>)
  }


  /**
     * Handles click on logout button.
     */
    _handleLogout = () => {
      console.log("_handleLogout", this.props, this.state);
      this.props.logout();

  }

}

//export default (NavBanner);
const mapStateToProps = function(state) {
  //  console.log("App state=", state);
    return {
      menuType: settingsSelectors.getMenuType(state),
      themeName: settingsSelectors.getThemeName(state),
      themeLogo: settingsSelectors.getThemeLogo(state),
      themeColor: settingsSelectors.getThemeColor(state),
      themeStyle: settingsSelectors.getThemeStyle(state),
    };
  };
  
  const mapDispatchToProps = {
    logout: userOperations.logout,
    ...viewsActions
  };
  
  export default connect(
    mapStateToProps,
    mapDispatchToProps
  )(NavBanner);
  