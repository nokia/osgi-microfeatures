import React, { PureComponent } from "react";
import RcOverlayPanel from "../../commons/RcoverlayPanel";
import $ from "jquery";

export default class DetailsPanel extends PureComponent {

  componentWillMount() {

    this.props.stopPolling();

  }
  componentDidMount() {
    const toggleButton = $("#overlay-runtimedetails-openPanel-button");
    console.log("toggleButton", toggleButton);
    toggleButton.click();
  }

  componentWillUnmount() {
    console.log("componentWillUnmount");
    // Close the panel, restart runtimes polling
    this.props.startPolling();
  }

  renderPorts = list => {
    if (list.length < 1) return " none";
    return (
      <ul>
        {list.map(item => {
          let options = "";
          if (item.external === true) options = "EXTERNAL";
          else if (item.ingress === true)
            options = `INGRESS path :${item.ingressPath}`;

          return (
            <li key={item.id}>
              {item.name} {item.port} {item.protocol} {options}
            </li>
          );
        })}
      </ul>
    );
  };

  renderPodLabels = list => {
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

  renderOverrides = list => {
    if (list.length < 1) return " none";
    return (
      <ul>
        {list.map(item => {
          let options = "";
          if (item.replace === true) options = "REPLACE";
          return (
            <li key={item.id}>
              {item.pid} {options}
              <ul>
                Properties:
                <ul>
                {item.props.map(prop => {
                  return (
                    <li key={prop.id}>
                      {prop.name} = {prop.value}
                    </li>
                  );
                })}
                </ul>
              </ul>
            </li>
          );
        })}
      </ul>
    );
  };

  renderFiles = list => {
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

  renderSecret = list => this.renderFiles(list);

  renderContent = () => {
    const {
      name,
      namespace,
      replicas,
      features,
      portslist,
      podLabelsList,
      overridesList,
      envsList,
      filesList,
      configMap,
      prometheusList,
      secretsList,
      podsUrls
    } = this.props.runtime;

    const featuresList = features.map(f => <li key={f}>{f}</li>);

    const renderPodsUrls = list => {
      if (list.length < 1) return " none";
      return (
        <ul>
          <table className={"extendedRuntime"}>
            <tbody>
              {list.map(p => (
                <Pod key={p.name} {...p} />
              ))}
            </tbody>
          </table>
        </ul>
      );
    };

    const Pod = props => (
      <tr>
        <td>{props.name}</td>
        <td>{props.status}</td>
      </tr>
    );

    return (
      <div style={{ margin: "10px" }}>
        <h2>Configuration</h2>
        <ul>
          <li>{`Name: ${name}`}</li>
          <li>{`Namespace: ${namespace}`}</li>
          <li>{`Replicas: ${replicas}`}</li>
          <li>
            Features: {featuresList.length}
            <ul>{featuresList}</ul>
          </li>
          <li>
            Ports:
            {this.renderPorts(portslist)}
          </li>
          <li>
            CASR runtime configuration
            <ul>
              <li>
                Pod Labels:
                {this.renderPodLabels(podLabelsList)}
              </li>
              <li>
                Overrides (.cfg):
                {this.renderOverrides(overridesList)}
              </li>
              <li>
                Environment variables:
                {this.renderPodLabels(envsList)}
              </li>
              <li>
                Files:
                {this.renderFiles(filesList)}
              </li>
              <li>
                ConfigMap:
                {this.renderPodLabels(configMap)}
              </li>
              <li>
                Prometheus:
                {this.renderPrometheus(prometheusList)}
              </li>
              <li>
                Secrets:
                {this.renderSecret(secretsList)}
              </li>
            </ul>
          </li>
        </ul>
        <h2>Pod(s)</h2>
        {renderPodsUrls(podsUrls)}
      </div>
    );
  };

  handlePanelAction = json => {
    console.log("##### onPanelAction ######", json);
    const isClosed = json.state === "closed";
    if (isClosed) this.props.onCancel();
  };

  renderTitle = () => "Runtime details";

  render() {
    console.log("<DetailsPanel /> props", this.props);

    return (
      <RcOverlayPanel
        id={"overlay-runtimedetails"}
        title={this.renderTitle()}
        content={this.renderContent()}
        onPanelAction={o => this.handlePanelAction(o)}
        hideFooter={true}
      />
    );
  }
}
