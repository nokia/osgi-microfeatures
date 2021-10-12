/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import { connect } from 'react-redux';

import { viewsActions } from '../../state/ducks/views'
import { userMsgSelectors, userMsgActions } from '../../state/ducks/userMsg'
import { entitiesOperations } from '../../state/ducks/entities'
import { indicatorsSelectors } from '../../state/ducks/indicators'
import { settingsSelectors } from '../../state/ducks/settings'

import Dialog from '@nokia-csf-uxr/csfWidgets/Dialog';
import Label from '@nokia-csf-uxr/csfWidgets/Label';
import ProgressIndicatorCircular from '@nokia-csf-uxr/csfWidgets/ProgressIndicatorCircular';

// react-router
import { MemoryRouter as Router, Switch, Route } from 'react-router-dom';

import UserDialog from '../commons/UserDialog';

import { utils } from '../../utilities'
import { viewsMap } from './constants'
import Banner from './Banner'

// CCFK css
import '@nokia-csf-uxr/csfWidgets/csfWidgets.css';

// App: additional css
import './App.css';

class App extends Component {

  constructor(props) {
    super(props);
    this.state = { height: 0, width: 0 };
    this._isMounted = false;
  }

  componentWillMount() {
    // Start data loading...
    this.props.requestData()
  }

  componentDidMount() {
    this._isMounted = true;
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
  }

  handleResize = () => {
    if (this._isMounted) {
      const pageSize = utils.getPageSize();
      const headerHeight = 48;
      const height = pageSize.height - (headerHeight);
      const width = pageSize.width;

      if (this.state.height !== height || this.state.width !== width) {
        this.setState({ height: height, width: width });
      }
    }
  }

  handleDismissClick = (target) => {
    console.log("target", target);
    this.props.resetUserMessage()
  }

  renderUserMessage = () => {
    const { userMessage } = this.props;
    if (!userMessage) {
      return null
    }
    return (
      <UserDialog {...userMessage} onClose={this.handleDismissClick} />
    )
  }

  renderLoading = () => {
    const { isLoading, loadingMsg } = this.props
    const title = (loadingMsg !== null )?loadingMsg:'Loading...'
    return (
      <div style={{ textAlign: 'center' }}>
        {isLoading === true &&
          <Dialog id="reloading" title={"-"} width={300} height={150}>
          <Label text={title} />
            <ProgressIndicatorCircular
              spinner="right"
              css={{ xxlarge: true }}
            />
          </Dialog>
        }
      </div>
    )
  }

  render() {
    return (
      <Router>
        <div>
          <Banner {...this.props} {...this.state} >
            <Switch> {/* React-Router: renders a Route depending on which path (specified in Route as a prop) is matched */}
              {viewsMap.map((view, index) => {
                let $Component = view.component
                let path = (view.path) ? view.path : `/${view.id}`
                return (
                  <Route
                    key={index}
                    exact={view.exact}
                    //                      path={`${process.env.PUBLIC_URL}${path}`}
                    path={path}
                    // eslint-disable-next-line
                    render={(props) => (<$Component {...props} {...this.props} {...this.state} />)}
                  />
                )
              })}
            </Switch>
            {this.renderUserMessage()}
            {this.renderLoading()}
          </Banner>
        </div>
      </Router>
    );
  }
}

const mapStateToProps = function (state) {
  console.log("App state=", state);
  return {
    isLoading: indicatorsSelectors.isLoading(state),
    loadingMsg: indicatorsSelectors.loadingMsg(state),
    isWelcome: indicatorsSelectors.isWelcome(state),
    userMessage: userMsgSelectors.getUserMessage(state),
    menuType: settingsSelectors.getMenuType(state),
    themeName: settingsSelectors.getThemeName(state),
    themeLogo: settingsSelectors.getThemeLogo(state),
    themeColor: settingsSelectors.getThemeColor(state),
    themeStyle: settingsSelectors.getThemeStyle(state)
  }
}

const mapDispatchToProps = {
  ...viewsActions,
  requestData: entitiesOperations.requestData,
  ...userMsgActions
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
