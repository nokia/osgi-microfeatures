/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import PropTypes from "prop-types";
import uniq from "lodash/uniq";

import Button from "@nokia-csf-uxr/csfWidgets/Button";
import TextInput from "@nokia-csf-uxr/csfWidgets/TextInput";
import SelectItem from "@nokia-csf-uxr/csfWidgets/SelectItemNew/SelectItemNew";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";

import { generateFormError } from "../../../../../utilities/utils";
import _ from "lodash";

class Ports extends Component {
  constructor(props) {
    super(props);
    this.errorsByIdx = [];
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.
    this.globalErrors = {};
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Ports componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errorsByIdx = [];
    this.props.elements.forEach(item => {
      this.prepareErrorResourcesForItem(item.id);
    });

    // Retrieves port warning message from global state.
    // This allows to keep warnings while navigate accross wizard pages.
    this.globalErrors = { ...this.props.storageErrors };
    console.log("componentWillMount globalErrors", this.globalErrors);
  }

  componentDidMount() {
    console.log("Ports did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("Ports componentWillUnmount");
    // Save only the warnings messages before navigating to other form
    const { namesError,replicasExternalError, ...warnings } = this.globalErrors;
    this.props.updateStorageErrors(warnings);
    // Clean
    this.errorsByIdx = [];
  }

  //
  // Save the Ports list in Redux and its complete status
  //
  saveData = (list, complete) => {
    this.props.updateElements(list, complete);
  };

  //
  // PORT DESCRIPTOR MANAGMENT
  //
  addItem = () => {
    // Generate an unic identifier for this new item
    const id =
      Date.now() +
      Math.random()
        .toString()
        .slice(2);

    // Add error resources for errors management
    this.prepareErrorResourcesForItem(id);

    const item = {
      id: id,
      name: "",
      port: "",
      protocol: "TCP",
      external: false,
      ingress: false,
      ingressPath: ""
    };

    // Update the list of port descriptor in the global state
    const newList = this.props.elements.concat(item);
    this.saveData([...newList], false);
  };

  deleteItem = e => {
    const { id } = e.data;
    const pruneds = this.props.elements.filter(item => item.id !== id);

    // Check the complete status of the list and update it
    const complete = this.isCompleteItems(pruneds);
    this.saveData([...pruneds], complete);
  };

  updateItem = item => {
    const index = this.props.elements.findIndex(elem => elem.id === item.id);
    const newList = Object.assign([], this.props.elements);
    newList[index] = item;
    // Check the complete status of the list and update it
    const complete = this.isCompleteItems(newList);
    this.saveData([...newList], complete);
  };

  //
  // VALIDATION - ERROR MANAGMENT
  //

  /**
   * Create / reset error resources for a port descriptor
   */
  prepareErrorResourcesForItem = id =>
    (this.errorsByIdx[id] = { nameError: "", portError: "", ingressError: "" });

  /**
   * Called by Wizard when the user is moving to the next step ( Button Continue clicked )
   */
  validate = () => {
    console.log("<Ports validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    // Perform a validation on each port descriptor
    this.props.elements.forEach(item => {
      // Re-initalise Error management for this item
      this.prepareErrorResourcesForItem(item.id);
      // After checking, item will be updated for trimable fields
      const newItem = { ...item };

      const { name, port, ingress, ingressPath } = item;

      const nameTrimed = name.trim();
      newItem.name = nameTrimed;
      if (!nameTrimed) {
        this.errorsByIdx[item.id].nameError = "Port name is required";
        validation.errors.push(this.errorsByIdx[item.id].nameError);
      } else if (/^[a-z](-?[a-z0-9])*$/.test(nameTrimed) === false) {
        this.errorsByIdx[item.id].nameError =
          "Should start by letter, no space, no uppercase. Letter/Number/Hyphen allowed.";
        validation.errors.push(this.errorsByIdx[item.id].nameError);
      }

      const portTrimed = port.trim();
      newItem.port = portTrimed;
      if (!portTrimed) {
        this.errorsByIdx[item.id].portError = "Port number is required";
        validation.errors.push(this.errorsByIdx[item.id].portError);
      } else if (/^([0-9])+$/.test(portTrimed) === false) {
        this.errorsByIdx[item.id].portError = "Only digits";
        validation.errors.push(this.errorsByIdx[item.id].portError);
      }

      if (ingress === true) {
        const ingressPathTrimed = ingressPath.trim();
        newItem.ingressPath = ingressPathTrimed;
        if (!ingressPathTrimed) {
          this.errorsByIdx[item.id].ingressError = "Ingress path is required";
          validation.errors.push(this.errorsByIdx[item.id].ingressError);
        } else if (
          /^\/[a-zA-Z0-9]((-|\/)?[a-zA-Z0-9])*$/.test(ingressPathTrimed) ===
          false
        ) {
          this.errorsByIdx[item.id].ingressError =
            "No space, no special character, ends with no '/'";
          validation.errors.push(this.errorsByIdx[item.id].ingressError);
        }
      }

      // Update the golbal state for this item
      this.updateItem(newItem);
    });

    // Check configuration variable name against all descriptors
    const names = this.props.elements.map(item => item.name.trim());
    const uniqNames = uniq(names);
    if (names.length > uniqNames.length) {
      this.globalErrors.namesError = "Some port names are identicals";
      validation.errors.push(this.globalErrors.namesError);
    } else {
      this.globalErrors.namesError = "";
    }

    // Check port numbers against all descriptors
    const portnumbers = this.props.elements.map(item => item.port.trim());
    const uniqPortNumbers = uniq(portnumbers);
    if (portnumbers.length > uniqPortNumbers.length) {
      this.globalErrors.portNbrError = "Some port numbers are identicals";
      validation.warnings.push(this.globalErrors.portNbrError);
    } else {
      this.globalErrors.portNbrError = "";
    }

    // Check external ports against number of replicats
    // replicas = 0 => a external port must be defined
    // replicas > 0 => no external port must be defined
    let msg;
    let error = false;
    if( this.props.replicas === 0 && this.props.hasExternalPort === false) {
      error = true;
      msg = "A port must be defined as 'external' when the number of replicas has been set to 0."
    } else if( this.props.replicas > 0 && this.props.hasExternalPort === true ) {
      error = true;
      msg = "No port should be defined as 'external' when the number of replicas has been set."
    }
    if( error === true) {
      this.globalErrors.replicasExternalError = msg;
      validation.errors.push(this.globalErrors.replicasExternalError);
    } else {
      this.globalErrors.replicasExternalError = '';
    }


    console.log(
      "<Ports validate() return validation",
      this.errorsByIdx,
      this.globalErrors,
      validation
    );
    return validation;
  };

  /**
   * Check the complete status for an item list
   */
  isCompleteItems = items => {
    for (let index = 0; index < items.length; index++) {
      if (this.isCompleteItem(items[index]) === false) return false;
    }
    return true;
  };

  /**
   * Check a port descriptor against its fields.
   * @item The item to check
   * @param True means the item is considered as complete
   */
  isCompleteItem = item => {
    return !(
      item.name.length < 1 ||
      item.port.length < 1 ||
      (item.ingress === true && item.ingressPath.length < 1)
    );
  };

  //
  // RENDERING METHODS
  //

  renderListItem = item => {
    const { nameError, portError, ingressError } = this.errorsByIdx[item.id];
    const protocols = [
      { label: "TCP", value: "TCP" },
      { label: "UDP", value: "UDP" }
    ];
    const optionals = [
      { label: "none", value: "none" },
      { label: "external=true", value: "external" },
      { label: "ingress", value: "ingress" }
    ];
    const currentOption =
      !!!item.external && !!!item.ingress
        ? "none"
        : item.external
        ? "external"
        : "ingress";

    return (
      <div style={{ paddingTop: "12px" }}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deleteBtn${item.id}`}
              tooltip={{ text: "Delete this port descriptor" }}
              icon={"ic_delete_inactive_ports"}
              offset={{ top: "5px" }}
              eventData={{ id: item.id }}
              onClick={this.deleteItem}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`name${item.id}`}
              required
              focus
              autoComplete={'off'}
              label="Name"
              text={item.name}
              errorMsg={nameError}
              error={!!nameError}
              onChange={data => {
                const newItem = { ...item };
                newItem.name = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
          <div className="col-sm-2">
            <TextInput
              id={`port${item.id}`}
              required
              autoComplete={'off'}
              label="Port number"
              text={item.port}
              errorMsg={portError}
              error={!!portError}
              onChange={data => {
                const newItem = { ...item };
                newItem.port = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
          <div className="col-sm-2">
            <SelectItem
              id={`protocol${item.id}`}
              options={protocols}
              selectedItem={item.protocol}
              name={`protocol${item.id}`}
              onChange={data => {
                const newItem = { ...item };
                newItem.protocol = data.value;
                this.updateItem(newItem);
              }}
              label={"Protocol"}
            />
          </div>
          <div className="col-sm-2">
            <SelectItem
              id={`option${item.id}`}
              options={optionals}
              selectedItem={currentOption}
              name={`option${item.id}`}
              onChange={data => {
                const newItem = { ...item };
                newItem.external = false;
                newItem.ingress = false;
                switch (data.value) {
                  case "external":
                    newItem.external = true;
                    newItem.ingressPath = "";
                    break;
                  case "ingress":
                    newItem.ingress = true;
                    break;
                  case "none":
                  /* falls through */
                  default:
                    newItem.ingressPath = "";
                }
                this.updateItem(newItem);
              }}
              label={"Options"}
            />
          </div>
          {item.ingress === true && (
            <div className="col-sm-2">
              <TextInput
                id={`ingresspath${item.id}`}
                required
                autoComplete={'off'}
                label="Ingress path"
                text={item.ingressPath}
                errorMsg={ingressError}
                error={!!ingressError}
                onChange={data => {
                  const newItem = { ...item };
                  newItem.ingressPath = data.value;
                  this.updateItem(newItem);
                }}
              />
            </div>
          )}
        </div>
      </div>
    );
  };

  renderPortItemList(list) {
    const listItems = list.map(item => (
      <li key={item.id} style={{ borderBottom: "1px solid #ccc" }}>
        {this.renderListItem(item)}
      </li>
    ));
    return (
      <ul style={{ listStyleType: "none", marginTop: "0px" }}>{listItems}</ul>
    );
  }

  render() {
    console.log("Ports render() props", this.props, this.state);
    const { portNbrError, namesError, replicasExternalError } = this.globalErrors;
    console.log("render namesError", namesError);

    return (
      <div>
        <div>{this.renderPortItemList(this.props.elements)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([namesError,replicasExternalError])}
        </div>
        <div className={"colorWarningList"}>
          {generateFormError([portNbrError])}
        </div>
        <div>
          <Button
            id="addBtn"
            text="ADD PORT"
            isCallToAction
            disabled={false}
            onClick={this.addItem}
          />
          <div className={"inlinelabel"}>
            <Label text="Add ports to expose [optional]" />
          </div>
          <div style={{ paddingTop: "5px" }}>
            <li style={{ listStyleType: "none" }}>Port options:</li>
            <ul>
              <li>
                external=true : if the port is meant to be exposed outside of
                the cluster
              </li>
              <li>
                ingress: generates an ingress resource for this port, path to be
                used in the ingress resource
              </li>
            </ul>
          </div>
        </div>
      </div>
    );
  }
}

Ports.propTypes = {
  onValidate: PropTypes.func,
  updateElements: PropTypes.func.isRequired,
  elements: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
      port: PropTypes.string.isRequired,
      protocol: PropTypes.oneOf(["TCP", "UDP"]),
      external: PropTypes.bool.isRequired,
      ingress: PropTypes.bool.isRequired,
      ingressPath: PropTypes.string.isRequired
    })
  ).isRequired,
  hasExternalPort: PropTypes.bool.isRequired,
  replicas : PropTypes.number.isRequired,
  storageErrors : PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

Ports.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Ports;
