/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import { connect } from "react-redux";
import { indicatorsSelectors } from "../../state/ducks/indicators";

import Dialog from "@nokia-csf-uxr/csfWidgets/Dialog";
import Label from "@nokia-csf-uxr/csfWidgets/Label";
import ProgressIndicatorCircular from "@nokia-csf-uxr/csfWidgets/ProgressIndicatorCircular";

class Loading extends Component {
  render() {
    const { isLoading, loadingMsg } = this.props;
    const title = loadingMsg !== null ? loadingMsg : "Loading...";
    return (
      <div style={{ textAlign: "center" }}>
        {isLoading === true && (
          <Dialog id="reloading" title={"-"} width={300} height={150}>
            <Label text={title} />
            <ProgressIndicatorCircular css={{ xxlarge: true }} />
          </Dialog>
        )}
      </div>
    );
  }
}

const mapStateToProps = function(state) {
  return {
    isLoading: indicatorsSelectors.isLoading(state),
    loadingMsg: indicatorsSelectors.loadingMsg(state)
  };
};

export default connect(mapStateToProps)(Loading);
