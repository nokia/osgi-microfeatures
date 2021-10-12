/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import CLIWindow from "../../commons/RcCLIWindow";
import PropTypes from "prop-types";

class Terminal extends Component {
  state = { podChanged: false };

  componentWillReceiveProps(nextProps) {
    if (nextProps.pod !== this.props.pod) {
      this.setState({ podChanged: true });
    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (this.state.podChanged !== nextState.podChanged) return true;
    return false;
  }

  componentDidUpdate(prevProps, prevState ) {
    if( this.state.podChanged === true && prevState.podChanged === false) {
      this.setState({ podChanged: false });
    }
  }

  render() {
    console.log("<Terminal /> props", this.props, this.state);
    const { podChanged } = this.state;
    if (podChanged === true) {
      return <div>REFRESH POD</div>;
    }

    const title =
      "GOGO shell Runtime: " + this.props.id + " Pod: " + this.props.pod;

    var loc = window.location,
      new_uri;
    if (loc.protocol === "https:") {
      new_uri = "wss:";
    } else {
      new_uri = "ws:";
    }
    new_uri += "//" + loc.hostname + (loc.port ? ":" : "") + loc.port + loc.pathname + "/../../";
    new_uri += "gogo?" + this.props.pod;
    console.log("uri:", new_uri);

    return (
      <CLIWindow
        title={title}
        socket={new_uri}
        resizeBackend
        closeBackendString = "SERVER::CLOSE" 
        onError={() => {
          console.log("onError now");
        }}
      />
    );
  }
}

Terminal.propTypes = {
  id: PropTypes.string.isRequired,
  pod: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
  close: PropTypes.func.isRequired,
  width: PropTypes.number.isRequired
};

export default Terminal;
