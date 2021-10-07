import React, { Component } from "react";
import { connect } from "react-redux";

import { userSelectors } from "../../state/ducks/user";
import { viewsActions } from "../../state/ducks/views";
import { userMsgSelectors, userMsgActions } from "../../state/ducks/userMsg";
import { entitiesOperations } from "../../state/ducks/entities";

// react-router
import { MemoryRouter as Router, Switch, Route, Redirect } from "react-router-dom";

import UserDialog from "../commons/UserDialog";

import { utils } from "../../utilities";
import WelcomePage from '../pages/navigation/WelcomePage'
import PostLogoutPage from '../pages/navigation/PostLogoutPage'
import AccessDenied from '../pages/navigation/AccessDenied'
import NotFound from '../pages/navigation/NotFound'

import NavBanner from "./NavBanner";

// App: Views/Components
import Features from '../pages/features'
import DeployWizard from '../pages/features/DeployWizard'
import DashBoard from '../pages/dashboard'
import Runtimes from '../pages/runtimes'
import Routes from '../pages/routes'
import RouteWizard from '../pages/routes/RouteWizard'
import Functions from '../pages/functions'
import FunctionWizard from '../pages/functions/FunctionWizard'
import Obrs from '../pages/obrs'
import Settings from '../pages/settings'
import Helps from '../pages/helps'

// CCFK css
import "@nokia-csf-uxr/csfWidgets/csfWidgets.css";

// App: additional css
import "./App.css";

const PrivateRoute = ({ component: Component, user, anyRole, computedMatch, ...rest }) => {
  console.log("PrivateRoute rest", rest)
  const { configData } = rest;
  const { isLoggedIn, isAdministrator /* roles */} = configData;
  let showFeature = false;
  if( anyRole || isAdministrator ) {
    showFeature = true;
  }

  return (
      <Route {...rest} render={props => {
          console.log("PrivateRoute Route props/rest", props,rest)
          if (!isLoggedIn) {
              return (
                  <Redirect to={{
                      pathname: '/postlogout',
                      state: { from: props.location }
                  }} />
              );
          }
          if (!showFeature) {
            return (
                <Redirect to={{
                    pathname: '/accessDenied',
                    state: { from: props.location }
                }} />
            );
          }

          return (
              <NavBanner {...configData} changePath={props.history.push} >
                  <Component {...rest} history={props.history} />
              </NavBanner>
          );
      }} />
  );
};


const getWidthHeigth = () => {
  const pageSize = utils.getPageSize();
  const headerHeight = 48;
  const height = pageSize.height - headerHeight;
  const width = pageSize.width;
  return { height: height, width: width }
}


class App extends Component {
  constructor(props) {
    super(props);
    this.state = getWidthHeigth();
    this._isMounted = false;
  }

  componentWillMount() {
    // Start data loading...
    this.props.requestData();
  }

  componentDidMount() {
    this._isMounted = true;
    window.addEventListener("resize", this.handleResize);
    this.handleResize();
  }

  handleResize = () => {
    if (this._isMounted) {
      const { width, height} = getWidthHeigth();
      if (this.state.height !== height || this.state.width !== width) {
        this.setState({ height: height, width: width });
      }
    }
  };

  handleDismissClick = target => {
    console.log("target", target);
    this.props.resetUserMessage();
  };

  renderUserMessage = () => {
    const { userMessage } = this.props;
    if (!userMessage) {
      return null;
    }
    return <UserDialog {...userMessage} onClose={this.handleDismissClick} />;
  };

  render() {
    console.log("<App/> props", this.props, this.state);
    const configData = { ...this.props.user, isAdministrator: this.props.isAdministrator, ...this.state };

    return (
      <div>
        <Router>
          <div>
              <Switch>
                <Route exact path="/" render={(props) => (<WelcomePage changePath={props.history.push}  {...configData} />)} />
                <Route path="/postlogout" render={(props) => (<PostLogoutPage {...props} />)} />
                {/* React-Router: renders a Route depending on which path (specified in Route as a prop) is matched */}
                <PrivateRoute anyRole path={'/DASHBOARD'} component={DashBoard} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/FEATURES'} component={Features} configData={configData} {...this.state} />
                <PrivateRoute anyRole exact path={'/deploywizard'} component={DeployWizard} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/RUNTIMES'} component={Runtimes} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/ROUTES'} component={Routes} configData={configData} {...this.state} />
                <PrivateRoute anyRole exact path={'/routewizard'} component={RouteWizard} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/FUNCTIONS'} component={Functions} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/functionwizard'} component={FunctionWizard} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/OBRS'} component={Obrs} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/SETTINGS'} component={Settings} configData={configData} {...this.state} />
                <PrivateRoute anyRole path={'/HELPS'} component={Helps} configData={configData} {...this.state} />

                <PrivateRoute anyRole exact path="/accessDenied" component={AccessDenied} configData={configData} />
                <PrivateRoute anyRole path="*" component={NotFound} configData={configData} />
              </Switch>
          </div>
        </Router>
        {this.renderUserMessage()}
      </div>
    );
  }

}

const mapStateToProps = function(state) {
//  console.log("App state=", state);
  return {
    userMessage: userMsgSelectors.getUserMessage(state),
    user: userSelectors.getCurrentUser(state),
    isAdministrator : userSelectors.isAdministrator(state)
  };
};

const mapDispatchToProps = {
  ...viewsActions,
  requestData: entitiesOperations.requestData,
  ...userMsgActions
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
