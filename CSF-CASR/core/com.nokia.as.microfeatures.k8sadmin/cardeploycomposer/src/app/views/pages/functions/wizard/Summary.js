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

  renderValues = list => {
    if (list.length < 1) return " none";
    return (
      <ul>
        {list.map(item => {
          return <li key={item.id}>{item.value}</li>;
        })}
      </ul>
    );
  };

  render() {
    console.log("<Summary props", this.props);
    const {
      name,
      lazy,
      timeout,
      locationsList,
      paramsList
    } = this.props.functionForm;
    const layzyLabel =
      lazy === true ? ` ON  Timeout: ${timeout} second(s)` : "OFF";

    return (
      <div style={{ margin: "10px" }}>
        <h1>Summary</h1>
        <ul>
          <li>{`Name: ${name}`}</li>
          <li>
            Handling parameters:
            <ul>
              <li>Lazy: {layzyLabel}</li>
            </ul>
          </li>
          <li>
            Locations:
            {this.renderValues(locationsList)}
          </li>
          <li>
            Parameters:
            {this.renderKeyValues(paramsList)}
          </li>
        </ul>
      </div>
    );
  }
}

export default Summary;
