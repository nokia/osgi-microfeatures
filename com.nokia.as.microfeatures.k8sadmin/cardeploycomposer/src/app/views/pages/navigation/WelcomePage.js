import React, { Component } from "react";
import $ from "jquery";
import PropTypes from "prop-types";

import AppBanner from "@nokia-csf-uxr/csfWidgets/AppBanner";
import Button from "@nokia-csf-uxr/csfWidgets/Button";
import Label from "@nokia-csf-uxr/csfWidgets/Label";

import { InitialView as defaultView } from "../../../state/ducks/views/constants";

class WelcomePage extends Component {
  static defaultProps = {
    banId: "MyAppBanner"
  };

  state = { error: false };

  navigateToDefaultView = () => {
    // trigger a route-change with react-router
    this.props.changePath({
      pathname: `/${defaultView}`
    });
  };

  componentWillMount() {
    console.log("componentWillMount props", this.props);
    const { fetchedUser, preferredUsername, name } = this.props;
    if (fetchedUser === true && (preferredUsername !== null || name !== null)) {
      // trigger a route-change with react-router
      this.navigateToDefaultView();
    }
  }

  componentWillReceiveProps(nextProps) {
    const { fetchedUser, preferredUsername, name } = this.props;
    if (fetchedUser === true || nextProps.fetchedUser === true) {
      // Time when user data must be consistent
      if (
        preferredUsername !== null ||
        name !== null ||
        nextProps.preferredUsername !== null ||
        nextProps.name !== null
      ) {
        // trigger a route-change with react-router
        setTimeout( this.navigateToDefaultView , 1000);
      }
      else {
        // No user data! Display an error message
        let newstate = { error: true };
        this.setState(newstate);
      }
    }
  }

  componentDidMount() {
    // Remove default right part of AppBanner that display user info
    const rightPartNode = $(".user-account-summary-button");
    rightPartNode.remove();
  }

  reload = () => {
    window.location.reload(true);
  };

  render() {
    console.log("<WelcomePage /> props", this.props);
    return (
      <AppBanner
        id={this.props.banId}
        productFamily="CASR"
        productName="Composer"
        acctSettings=""
        userAccountSummaryUsername=""
        bannerColor={this.props.themeColor}
        logo={this.props.themeLogo}
        textAndIconStyle={this.props.themeStyle}
      >
        <div style={{ textAlign: "center" }}>
          <h1>Welcome to the CASR Composer</h1>
          {this.state.error !== true && (
            <div>
              <Label text={"Please wait..."} />
            </div>
          )}

          {this.state.error === true && (
            <div>
              <h2>Sorry, unable to load user data. Please retry later...</h2>
              <Button
                id="reloadbtn"
                text="Reload"
                onClick={this.reload}
                isCallToAction
              />
            </div>
          )}
        </div>
      </AppBanner>
    );
  }
}

WelcomePage.propTypes = {
  //  currentUser: userType.isRequired,
  //  fetchedUser: PropTypes.bool.isRequired,
  changePath: PropTypes.func.isRequired,
  banId: PropTypes.string.isRequired
};

export default WelcomePage;
