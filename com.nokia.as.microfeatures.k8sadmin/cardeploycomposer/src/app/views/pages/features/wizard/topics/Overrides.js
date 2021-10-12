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
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";
import CheckBox from "@nokia-csf-uxr/csfWidgets/CheckBox/CheckBox";
import ImportContentFromLocalFile from "../../../../commons/ImportContentFromLocalFile";

import {
  generateFormError,
  propertyfileToJson
} from "../../../../../utilities/utils";
import _ from "lodash";

class Overrides extends Component {
  constructor(props) {
    super(props);
    this.errorsByIdx = [];
    // Used to put validation messages which not concern a specific input.
    // In case of warning messages which must be live across navigation,
    // they should be read on mount and written on unmounting.( Not the case for overrides )
    this.globalErrors = {};
    // Types to distinguish override descriptor and its children ( properties )
    this.types = {
      overrides: "overrides",
      properties: "properties"
    };
  }

  //
  // LIFE CYCLE METHODS
  //

  componentWillMount() {
    console.log("Overrides componentWillMount props", this.props);
    // In according to the preset fields, initialize local error management
    this.errorsByIdx = [];

    this.props.elements.forEach(item => {
      this.prepareErrorResourcesForItem(item);
    });

    // Retrieves Overrides warning message from global state.
    //This allows to keep warnings while navigate accross wizard pages.
    this.globalErrors = {}; // { ...this.props.storageErrors };
  }

  componentDidMount() {
    console.log("Overrides did mount");
    if (this.props.onValidate) {
      this.props.onValidate(this.validate);
    }
  }

  componentWillUnmount() {
    console.log("Overrides componentWillUnmount");
    // Save the warnings before navigating to other pages
    // const { overridePidsError, ...warnings } = this.globalErrors;
    // this.props.updateStorageErrors(warnings);
    // Clean
    this.errorsByIdx = [];
  }

  //
  // Save the Overrides list in Redux and its complete status
  //
  saveData = (list, complete) => {
    this.props.updateElements(list, complete);
  };

  //
  // OVERRIDE DESCRIPTOR MANAGMENT
  //
  addItem = () => {
    // Generate an unic identifier for this new item
    const id =
      Date.now() +
      Math.random()
        .toString()
        .slice(2);

    const item = {
      id: id,
      replace: false,
      pid: "",
      props: []
    };

    // Add error resources for errors management
    this.prepareErrorResourcesForItem(item);

    // Add a property to start
    const itemWithProp = this.addOverrideProp(item);

    // Update the list of pod label descriptor in the global state
    const newList = this.props.elements.concat(itemWithProp);
    this.saveData([...newList], false);
  };

  deleteItem = e => {
    const { id } = e.data;
    const pruneds = this.props.elements.filter(item => item.id !== id);

    // Check the complete status of the form and update it
    const complete = this.isCompleteItems(this.types.overrides, pruneds);
    this.saveData([...pruneds], complete);
  };

  updateItem = item => {
    const index = this.props.elements.findIndex(elem => elem.id === item.id);
    const newList = Object.assign([], this.props.elements);
    newList[index] = item;
    // Check the complete status of the form and update it
    const complete = this.isCompleteItems(this.types.overrides, newList);
    this.saveData([...newList], complete);
  };

  loadOverrideProp = load => {
    console.log("files uploded", load);
    const { id, /*filename,*/ content } = load;
    const index = this.props.elements.findIndex(elem => elem.id === id);
    const newItem = { ...this.props.elements[index] };

    const jprops = propertyfileToJson(content);
    console.log("loadOverrideProp propertyfileToJson", jprops);
    // add for each property an override property
    const overridesProps = [];
    for (let key in jprops) {
      const id =
        Date.now() +
        Math.random()
          .toString()
          .slice(2);
      if (jprops.hasOwnProperty(key)) {
        overridesProps.push({ id: id, name: key, value: jprops[key] });
        this.prepareErrorResourcesForProp(newItem.id, id);
      }
    }
    newItem.props = overridesProps;

    console.log("loadOverrideProp newitem", newItem);
    this.updateItem(newItem);
  };

  // OVERRIDE PROPERTY MANAGMENT
  addOverridePropOnClick = e => {
    const { parentItem } = e.data;
    const newParentIem = this.addOverrideProp(parentItem);
    this.updateItem(newParentIem);
  };

  addOverrideProp = parentItem => {
    // Generate an unic identifier for this new item
    const id =
      Date.now() +
      Math.random()
        .toString()
        .slice(2);

    // Add error resources for errors management
    console.log("parentItem", parentItem, id);
    this.prepareErrorResourcesForProp(parentItem.id, id);

    const item = {
      id: id,
      name: "",
      value: ""
    };

    // Update the list of override props descriptor in the parent
    const { props } = parentItem;
    parentItem.props = props.concat(item);
    return parentItem;
  };

  deleteOverrideProp = e => {
    const { parentItem, id } = e.data;
    const { props } = parentItem;
    const pruneds = props.filter(item => item.id !== id);
    parentItem.props = pruneds;

    this.updateItem(parentItem);
  };

  updateOverrideProp = (parentItem, item) => {
    const { props } = parentItem;
    const index = props.findIndex(elem => elem.id === item.id);
    const newProps = Object.assign([], props);
    newProps[index] = item;
    parentItem.props = newProps;

    this.updateItem(parentItem);
  };

  //
  // VALIDATION - ERROR MANAGMENT
  //

  /**
   * Create / reset error resources for a override descriptor
   */
  prepareErrorResourcesForItem = item => {
    const { props } = item;
    const propsError = [];
    props.forEach(prop => {
      propsError[prop.id] = { nameError: "", valueError: "" };
    });
    this.errorsByIdx[item.id] = {
      pidError: "",
      propsError: propsError,
      propNamesError: ""
    };
  };

  prepareErrorResourcesForProp = (parentId, id) => {
    this.errorsByIdx[parentId].propsError[id] = {
      nameError: "",
      valueError: ""
    };

    console.log(
      "prepareErrorResourcesForProp",
      parentId,
      id,
      this.errorsByIdx[parentId],
      this.errorsByIdx[parentId].propsError[id]
    );
  };

  /**
   * Called by Wizard when the user is moving to the next step ( Button Continue clicked )
   */
  validate = () => {
    console.log("<Overrides validate()");
    const validation = {
      errors: [],
      warnings: []
    };

    //
    // Perform a validation on each override descriptor
    //
    this.props.elements.forEach(item => {
      // Re-initalise Error management for this item
      this.prepareErrorResourcesForItem(item);
      // After checking, item will be updated for trimable fields
      const newItem = { ...item };

      const { pid } = item;

      const pidTrimed = pid.trim();
      newItem.pid = pidTrimed;
      if (!pidTrimed) {
        this.errorsByIdx[item.id].pidError = "Override pid is required";
        validation.errors.push(this.errorsByIdx[item.id].pidError);
      } else if (/^[a-zA-Z](\.?[a-zA-Z0-9])*$/.test(pidTrimed) === false) {
        this.errorsByIdx[item.id].pidError =
          "Should start by letter, no space. Letter/Number/Dot allowed.";
        validation.errors.push(this.errorsByIdx[item.id].pidError);
      }

      // Validate properties list for this override descriptor
      const newProps = [];
      newItem.props.forEach(prop => {
        const newProp = { ...prop };
        const { name, value } = newProp;

        // this.errorsByIdx[parentId].propsError[id]

        const nameTrimed = name.trim();
        newProp.name = nameTrimed;
        if (!nameTrimed) {
          this.errorsByIdx[item.id].propsError[newProp.id].nameError =
            "Property name is required";
          validation.errors.push(
            this.errorsByIdx[item.id].propsError[newProp.id].nameError
          );
        } else if (/^[a-zA-Z](\.?[a-zA-Z0-9])*$/.test(nameTrimed) === false) {
          this.errorsByIdx[item.id].propsError[newProp.id].nameError =
            "Should start by letter, no space. Letter/Number/Dot allowed.";
          validation.errors.push(
            this.errorsByIdx[item.id].propsError[newProp.id].nameError
          );
        }

        const valueTrimed = value.trim();
        newProp.value = valueTrimed;
        if (!valueTrimed) {
          this.errorsByIdx[item.id].propsError[newProp.id].valueError =
            "Property value is required";
          validation.errors.push(
            this.errorsByIdx[item.id].propsError[newProp.id].valueError
          );
        } else if (/^[^\s=]*$/.test(valueTrimed) === false) {
          this.errorsByIdx[item.id].propsError[newProp.id].valueError =
            "No space, no equal character allowed.";
          validation.errors.push(
            this.errorsByIdx[item.id].propsError[newProp.id].valueError
          );
        }

        newProps.push(newProp);
      });

      // Check property name against all properties
      const propNames = newProps.map(prop => prop.name);
      const uniqPropsNames = uniq(propNames);
      if (propNames.length > uniqPropsNames.length) {
        this.errorsByIdx[item.id].propNamesError =
          "Some property names are identicals";
        validation.errors.push(this.errorsByIdx[item.id].propNamesError);
      }

      // Update the global state for this item
      newItem.props = newProps;
      this.updateItem(newItem);
    });

    // Check pid against all override descriptors
    const pids = this.props.elements.map(item => item.pid.trim());
    const uniqPids = uniq(pids);
    if (pids.length > uniqPids.length) {
      this.globalErrors.overridePidsError = "Some Override pid are identicals";
      validation.errors.push(this.globalErrors.overridePidsError);
    } else {
      this.globalErrors.overridePidsError = "";
    }

    console.log(
      "<Overrides validate() return validation",
      this.errorsByIdx,
      this.globalErrors,
      validation
    );
    return validation;
  };

  /**
   * Check the complete status for an item list
   */
  isCompleteItems = (type, items) => {
    for (let index = 0; index < items.length; index++) {
      if (this.isCompleteItem(type, items[index]) === false) return false;
    }
    return true;
  };

  /**
   * Check a pod label descriptor against its fields.
   * @type type of item
   * @item The item to check
   * @param True means the item is considered as complete
   */
  isCompleteItem = (type, item) => {
    let complete = false;
    switch (type) {
      case this.types.overrides:
        complete =
          item.pid.length > 0 &&
          item.props.length > 0 &&
          this.isCompleteItems(this.types.properties, item.props);
        break;

      case this.types.properties:
        complete = item.name.length > 0 && item.value.length > 0;
        break;

      default:
        console.error(
          `isCompleteItem  unkwown type (return false) : ${type}`,
          item
        );
    }
    console.log("isCompleteItem", type, item, complete);
    return complete;
  };

  //
  // RENDERING METHODS
  //
  renderItem = item => {
    const { pidError, propNamesError } = this.errorsByIdx[item.id];

    return (
      <div className={"no-footer"} style={{ paddingTop: "12px" }}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deleteOvBtn${item.id}`}
              tooltip={{ text: "Delete this override descriptor" }}
              icon={"ic_delete"}
              offset={{ top: "5px" }}
              eventData={{ id: item.id }}
              onClick={this.deleteItem}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`ovPid${item.id}`}
              required
              focus
              autoComplete={'off'}
              label="Component identity"
              text={item.pid}
              errorMsg={pidError}
              error={!!pidError}
              onChange={data => {
                const newItem = { ...item };
                newItem.pid = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
          <div style={{ position: "relative", top: "9px", float: "left" }}>
            <CheckBox
              id={`ovRep${item.id}`}
              value={item.replace}
              label="Replace completely"
              onChange={data => {
                const newItem = { ...item };
                newItem.replace = data.value;
                this.updateItem(newItem);
              }}
            />
          </div>
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <ImportContentFromLocalFile
              id={item.id}
              fileTypes={".properties"}
              onLoad={this.loadOverrideProp}
            >
              <Button
                id={`loadOvPBtn${item.id}`}
                tooltip={{
                  text: "Populate properties from a property file (.properties)"
                }}
                icon={"ic_import"}
                offset={{ top: "5px" }}
              />
            </ImportContentFromLocalFile>
          </div>
        </div>
        <div className={"no-footer"}>
          {this.renderSubItemList(item, item.props, this.renderOverrideProp)}
        </div>
        <div className={"colorErrorList"}>
          {generateFormError([propNamesError])}
        </div>
        <div className="row">
          <ul>
            <Button
              id={`addOvPropsBtn${item.id}`}
              tooltip={{ text: "Add Property" }}
              icon={"ic_add"}
              offset={{ top: "5px" }}
              eventData={{ parentItem: { ...item } }}
              onClick={this.addOverridePropOnClick}
            />
          </ul>
        </div>
      </div>
    );
  };

  renderOverrideProp = (parent, item) => {
    const newParent = { ...parent };
    const { nameError, valueError } = this.errorsByIdx[newParent.id].propsError[
      item.id
    ];
    const disabledDeleteBtn = newParent.props.length < 2;
    return (
      <div style={{}}>
        <div className="row">
          <div style={{ position: "relative", top: "13px", float: "left" }}>
            <Button
              id={`deleteOvPBtn${item.id}`}
              disabled={disabledDeleteBtn}
              tooltip={{ text: "Delete this property" }}
              icon={"ic_delete"}
              offset={{ top: "5px" }}
              eventData={{ parentItem: newParent, id: item.id }}
              onClick={this.deleteOverrideProp}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`ovpname${item.id}`}
              required
              focus
              autoComplete={'off'}
              label="Property name"
              text={item.name}
              errorMsg={nameError}
              error={!!nameError}
              onChange={data => {
                const newItem = { ...item };
                newItem.name = data.value;
                this.updateOverrideProp(newParent, newItem);
              }}
            />
          </div>
          <div className="col-sm-3">
            <TextInput
              id={`ovpvalue${item.id}`}
              required
              autoComplete={'off'}
              label="Property value"
              text={item.value}
              errorMsg={valueError}
              error={!!valueError}
              onChange={data => {
                const newItem = { ...item };
                newItem.value = data.value;
                this.updateOverrideProp(newParent, newItem);
              }}
            />
          </div>
        </div>
      </div>
    );
  };

  renderItemList(list, renderfun) {
    const listItems = list.map(item => (
      <li key={item.id} style={{ borderBottom: "1px solid #ccc" }}>
        {renderfun(item)}
      </li>
    ));
    return (
      <ul style={{ listStyleType: "none", marginTop: "0px" }}>{listItems}</ul>
    );
  }

  renderSubItemList(parent, subList, renderfun) {
    const listSubItems = subList.map(subItem => (
      <li key={subItem.id} style={{}}>
        {renderfun(parent, subItem)}
      </li>
    ));
    return (
      <ul style={{ listStyleType: "none", marginTop: "0px" }}>
        {listSubItems}
      </ul>
    );
  }

  render() {
    console.log("Overrides render() props", this.props, this.state);
    const { overridePidsError } = this.globalErrors;

    return (
      <div>
        <div>{this.renderItemList(this.props.elements, this.renderItem)}</div>
        <div className={"colorErrorList"}>
          {generateFormError([overridePidsError])}
        </div>
        <div>
          <Button
            id="addOvBtn"
            text="ADD PROPERTY"
            isCallToAction
            disabled={false}
            onClick={this.addItem}
          />
          <div className={"inlinelabel"}>
            <Label text="Override properties files (.cfg) [optional]" />
          </div>
        </div>
      </div>
    );
  }
}

Overrides.propTypes = {
  onValidate: PropTypes.func,
  updateElements: PropTypes.func.isRequired,
  elements: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      replace: PropTypes.bool.isRequired,
      pid: PropTypes.string.isRequired,
      props: PropTypes.arrayOf(
        PropTypes.shape({
          id: PropTypes.string.isRequired,
          name: PropTypes.string.isRequired,
          value: PropTypes.string.isRequired
        })
      ).isRequired
    })
  ).isRequired,
  storageErrors : PropTypes.object.isRequired,
  updateStorageErrors: PropTypes.func.isRequired
};

Overrides.defaultProps = {
  onValidate: _.noop,
  updateStorageErrors: _.noop,
  storageErrors: {}
};

export default Overrides;
