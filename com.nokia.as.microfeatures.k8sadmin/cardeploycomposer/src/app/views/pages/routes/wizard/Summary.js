/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";

class Summary extends Component {
  renderKeyValues = list => {
    if (list.length < 1) return " none";
    return (
      <ul>
        {list.map(item => {
          return (
            <li key={item.id}>
              {item.name} {item.value}
            </li>
          );
        })}
      </ul>
    );
  };

  renderNames = list => {
    if (list.length < 1) return " none";
    return (
      <ul>
        {list.map(item => {
          return <li key={item.id}>{item.name}</li>;
        })}
      </ul>
    );
  };

  renderPrometheus = list => {
    if (list.length < 1) return " none";
    return (
      <ul>
        {list.map(item => {
          return (
            <li key={item.id}>
              {item.port} {item.path}
            </li>
          );
        })}
      </ul>
    );
  };

  render() {
    console.log("<Summary props", this.props);
    const {
      name,
      type,
      path,
      functionId,
      paramsList,
      functionParamsList,
      runtimesList,
      ttl
    } = this.props.routeForm;

    return (
      <div style={{ margin: "10px" }}>
        <h1>Summary</h1>
        <ul>
          <li>{`Name: ${name}`}</li>
          <li>{`Type: ${type}`}</li>
          <li>{`Path: ${path}`}</li>
          <li>{`Function reference: ${functionId}`}</li>
          <li>
            Execution parameters:
            <ul>
              <li>
                Runtime:
                {this.renderNames(runtimesList)}
              </li>
              <li>Timeout: {ttl} millisecond(s)</li>
            </ul>
          </li>
          <li>
            Route parameters:
            {this.renderKeyValues(paramsList)}
          </li>
          <li>
            Function parameters:
            {this.renderKeyValues(functionParamsList)}
          </li>
        </ul>
      </div>
    );
  }
}

export default Summary;
