import React, { Component } from 'react';
import $ from "jquery"

// CCFK components / images
import AppBanner from '@nokia-csf-uxr/csfWidgets/AppBanner';
import icon_dashboard from '@nokia-csf-uxr/csfWidgets/images/ic_dashboard.svg';
import icon_features from '@nokia-csf-uxr/csfWidgets/images/ic_matrix.svg';
import icon_assemblies from '@nokia-csf-uxr/csfWidgets/images/ic_subnets.svg';
import icon_snapshots from '@nokia-csf-uxr/csfWidgets/images/ic_snapshot.svg';
import icon_browseobrs from '@nokia-csf-uxr/csfWidgets/images/ic_Constraints.svg';
import icon_settings from '@nokia-csf-uxr/csfWidgets/images/ic_settings.svg';
import icon_help from '@nokia-csf-uxr/csfWidgets/images/ic_help.svg';

import { viewsConst } from '../../state/ducks/views'

/* withRouter HOC needed to give us to react-routing functions
   these are usually available if components are passed through a route */
import { withRouter } from 'react-router-dom';

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
        name: 'Assemblies',
        icon: icon_assemblies,
        action: viewsConst.views.ASSEMBLIES,
      },
      {
        name: 'Snapshots',
        icon: icon_snapshots,
        action: viewsConst.views.SNAPSHOTS,
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

class Banner extends Component {
  static defaultProps = {
    banId: "MyAppBanner"
  }
  
  componentDidMount() {
    // Remove default right part of AppBanner that display user info
    const rightPartNode = $('.user-account-summary-button');
    rightPartNode.remove();
/*
    // Remove default right part of AppBanner MyAppBanner-user-account-summary-button
    const rightBtnNode = document.getElementById(this.props.banId + "-user-account-summary-button");
    const rightSideContainer = rightBtnNode.parentNode;
    const parentNode = rightSideContainer.parentNode;
    parentNode.removeChild(rightSideContainer);
*/
  }

  componentWillUpdate(nextProps) {
    console.log("componentWillUpdate",nextProps)
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
    this.props.history.push({
      pathname: pathname
    });
  }

  render() {
    console.log('<Banner/> props', this.props)
    let banner = null;
    if (this.props.isWelcome === true) {
      banner = <AppBanner
        id={this.props.banId}
        productFamily="ASR"
        productName="Application Runtimes Composer"
        acctSettings=''
        userAccountSummaryUsername=''
        bannerColor={this.props.themeColor}
        logo={this.props.themeLogo}
        textAndIconStyle={this.props.themeStyle}
        >
        {this.props.children}
      </AppBanner>
    } else {
      banner = <AppBanner
        id={this.props.banId}
        //        logoImagePath={nokiaLogo}
        productFamily="ASR"
        productName="Application Runtimes Composer"
        acctSettings=''
        userAccountSummaryUsername=''

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
      </AppBanner>
    }

    return banner;
  }
}

export default withRouter(Banner);