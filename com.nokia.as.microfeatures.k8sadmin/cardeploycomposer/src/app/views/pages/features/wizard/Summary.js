import React, { Component } from "react";
import { getObrVersion, getObrRepository } from "../../../../utilities/utils";

class Summary extends Component {
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
                  {item.props.map(prop => {
                    return (
                      <li key={prop.id}>
                        {prop.name} = {prop.value}
                      </li>
                    );
                  })}
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
  
    render() {
      console.log("<Summary props", this.props);
      const {
        name,
        namespace,
        replicas,
        portslist,
        podLabelsList,
        overridesList,
        envsList,
        filesList,
        configMap,
        prometheusList,
        secretsList
      } = this.props.deployForm;

      const featuresList = this.props.allSelectedFeatures.map(f => (
        <li key={f.fid}>{f.fid}</li>
      ));
      const repo = getObrRepository(this.props.obr);
      const obrVersion = getObrVersion(this.props.obr);
      return (
        <div style={{ margin: "10px" }}>
          <h1>Summary</h1>
          <ul>
            <li>{`Name: ${name}`}</li>
            <li>{`Namespace: ${namespace}`}</li>
            <li>{`Replicas: ${replicas}`}</li>
            <li>
              Features from repository: {repo} Release: {obrVersion}
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
        </div>
      );
    }
  }

export default Summary;
  