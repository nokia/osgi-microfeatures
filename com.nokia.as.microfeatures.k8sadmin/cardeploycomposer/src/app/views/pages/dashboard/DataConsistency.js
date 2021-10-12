/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import PropTypes from "prop-types";
import { isEqual } from "lodash";
import PieChart from "@nokia-csf-uxr/csfWidgets/PieChart";
class DataConsistency extends Component {
  constructor(props) {
    super(props);
    this.WRONGROUTES = "Inconsistent";
    this.WRONGRUNTIMES = "Inconsistent";
    this.state = {
      info: this.buildInfo(this.props.nbRoutes, this.props.nbRuntimes, this.props.consistency)
    };
  }

  componentWillReceiveProps(nextProps) {
    if (
      this.props.nbRoutes !== nextProps.nbRoutes ||
      this.props.nbRuntimes !== nextProps.nbRuntimes ||
      !isEqual(this.props.consistency, nextProps.consistency)
    ) {
      this.setInfoData(nextProps.nbRoutes, nextProps.nbRuntimes, nextProps.consistency);
    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (JSON.stringify(this.state.info) !== JSON.stringify(nextState.info))
      return true;
    return false;
  }

  setInfoData = (nbRoutes, nbRuntimes, consistency) => {
    this.setState({ info: this.buildInfo(nbRoutes, nbRuntimes, consistency) });
  };

  buildInfo = (nbRoutes, nbRuntimes, consistency) => {
    const { brokenRoutes, unknownFunctions, unknownRuntimes, runtimesWithBrokenRoute } = consistency;
    const validRoutes = nbRoutes - brokenRoutes;
    const validRuntimes = nbRuntimes - runtimesWithBrokenRoute;
    return {
      valids: validRoutes,
      brokens: brokenRoutes,
      functions: unknownFunctions,
      runtimes: unknownRuntimes,
      validRuntimes: validRuntimes,
      wrongRuntimes: runtimesWithBrokenRoute
    };
  };

  renderWrongRouteDetails = (name, functions, runtimes) => {
    if (name !== this.WRONGROUTES) return null;
    return (
      <div>
        { functions !== 0 && <span className={"wrongRouteDetails"}>
          Nb of function(s) not found: {functions}
        </span>}
        { runtimes !== 0 && <span className={"wrongRouteDetails"}>
          Nb of runtime(s) not found: {runtimes}
        </span>}
      </div>
    );
  };

  renderRoutetooltip = data => {
    console.log("renderRoutetooltip", data);
    const { functions, runtimes } = this.state.info;
    const { name, fill } = data;
    let { percent } = data;
    percent *= 100;
    percent = Math.round(percent);
    const value = `Nb: ${data.value}`;
    // Get this pie segments fill color, which will be the color of the value.
    const colorStyle = {
      color: fill
    };
    console.log("renderRoutetooltip functions/runtimes", functions, runtimes);
    return (
      <div id="pieDetails" className="popup" style={{ right: 10, top: 40 }}>
        <div className="popupContainer">
          <span className="name">{name}</span>
          <span className="percent" style={colorStyle}>
            {percent}
          </span>
          <span className="value">{value}</span>
          {this.renderWrongRouteDetails(name, functions, runtimes)}
        </div>
      </div>
    );
  };

  render() {
    console.log("<DataConsistency props />", this.props, this.state);
    if( this.props.nbRoutes === 0) {
      return <div>No route</div>
    }


    const { valids, brokens, functions, runtimes /*, validRuntimes, wrongRuntimes */} = this.state.info;
    console.log("valids brokens", valids, brokens, functions, runtimes);
    const pieRouteData = [
      { name: this.WRONGROUTES, value: brokens, fill: "#D9070A" },
      { name: "Valid", value: valids, fill: "#62AC00" }
    ];

    return (
      <div  style={{ display: "flex", justifyContent: 'center' }}>
        <div>
          <PieChart
            id={"routeConsistency"}
            title={"Status"}
            width={300}
            height={300}
            animate={true}
            outerRadius={80}
            //                activeIndex={1}
            data={pieRouteData}
            renderTooltip={this.renderRoutetooltip}
            valueLabel={"Nb"}
            dataKey="value"
            nameKey="name"
          />
        </div>
      </div>
    );
  }
}

DataConsistency.propTypes = {
  consistency: PropTypes.shape({
    brokenRoutes: PropTypes.number.isRequired,
    unknownFunctions: PropTypes.number.isRequired,
    unknownRuntimes: PropTypes.number.isRequired,
    runtimesWithBrokenRoute: PropTypes.number.isRequired
  }).isRequired,
  nbRoutes: PropTypes.number.isRequired,
  nbRuntimes: PropTypes.number.isRequired
};
export default DataConsistency;
