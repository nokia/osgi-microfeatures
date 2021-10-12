/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import { bindActionCreators } from "redux";
import { connect } from "react-redux";
import {
  entitiesSelectors,
  entitiesOperations
} from "../../../state/ducks/entities";
import CardCollection from "@nokia-csf-uxr/csfWidgets/Card/CardCollection";
import SelectItemNew from "@nokia-csf-uxr/csfWidgets/SelectItemNew";
import Card from "@nokia-csf-uxr/csfWidgets/Card";
import Label from "@nokia-csf-uxr/csfWidgets/Label";
import Image from "@nokia-csf-uxr/csfWidgets/Image";

import ToolBar from "../../commons/ToolBar";
import DataConsistency from "./DataConsistency";
//import $ from "jquery";

const OBR_REGEXP = /.*\/((\w+\.)*(\w)+-.*)\.xml$/;
class DashBoard extends Component {
  constructor(props) {
    super(props);
    this.userSuffix = " (User)";
    this.maxLabelLength = 65;
    this.remainingLength = this.maxLabelLength -10;
    this.state = {
      selectorItems: this.getSelectorItems(this.props.selectedObr)
    };
  }

  componentDidMount = () => {
    //        this.resizeCards()
  };

  componentWillReceiveProps(nextProps) {
    console.log("componentWillReceiveProps", this.props, nextProps);
    // Update the selector to take into an account a new user url
    if (
      (this.props.selectedObr === null && nextProps.selectedObr !== null) ||
      (nextProps.selectedObr !== null &&
      nextProps.selectedObr !== this.props.selectedObr &&
      nextProps.remoteObrs.includes(nextProps.selectedObr) === false)
    ) {
      console.log(
        "componentWillReceiveProps, UPDATE state",
        this.props,
        nextProps
      );
      this.setState({ selectorItems: this.getSelectorItems(nextProps.selectedObr) });
    }

  }

  componentDidUpdate = () => {
//        this.resizeCards()
  };

 /* 

  resizeCards = () => {
    // CSS tricks to fill all reserved height by CardCollection layout configuration
    $("#cardObr")
      .parent()
      .css("height", "100%");
    $("#cardStatus")
      .parent()
      .css("height", "100%");
    $("#cardContact")
      .parent()
      .css("height", "100%");
    $("#cardConsistency")
      .parent()
      .css("height", "100%");
  };

*/

  getSelectorItems = selectedObr => {
    const items = [];
    let urls = this.props.remoteObrs;
    for (let i = 0; i < urls.length; i++) {
      let item = { label: this.getUrlLabel(urls[i]), value: urls[i] };
      items.push(item);
    }
    // Take into an account a new user entry if needed
    if (selectedObr !== null) {
      if (urls.includes(selectedObr) === false) {
        let urlLabel = selectedObr;
        if( urlLabel.length <= (this.maxLabelLength - this.userSuffix.length) ) {
          urlLabel += this.userSuffix;
        } else {
          const prefix = urlLabel.substring(0,5);
          const rest = urlLabel.substring(5);
          const index = (rest.length > this.remainingLength)?(rest.length - this.remainingLength):0;
          urlLabel = prefix + '...' + rest.substring(index) + this.userSuffix;
        }
//        const userLabelItem = this.getUrlLabel(selectedObr) + " (User)";
        items.push({ label: urlLabel, value: selectedObr });
      }
    }
    return items;
  };

  getUrlLabel = url => {
    // Check if remote obr correspond to the .m2 obr ( case of -Dm2 option when the jar is launched )
    if (url.startsWith("file:")) {
      return url.substring(7);
    }
    // Reset global regexp index to parse properly
    OBR_REGEXP.lastIndex = 0;
    const match = OBR_REGEXP.exec(url);
    if (!match || match.length < 3) {
      console.warn("getUrlLabel no matching", url, match);
      return undefined;
    }
    return match[1];
  };

  onSelectObr = event => {
    let newUrl = event.value; // 17.1.0
    if (event.type === "onAdd") {
       newUrl = 'http://repo.lab.pl.alcatel-lucent.com/sandbox-mvn-candidates/' + event.value.value;
    }
    console.log("newUrl", newUrl);
    if( newUrl !== null)
      this.props.selectObr(newUrl);
  };

  checkNewReleaseOption = newoption => {
 //       http://repo.lab.pl.alcatel-lucent.com/sandbox-mvn-candidates/com/nokia/casr-obr/com.nokia.casr.obr/0.0.1/com.nokia.casr.obr-0.0.1.xml
//    return /^(http):\/\/.*\.xml$/.test(newoption);
    return /.*\.xml$/.test(newoption);
  };

  renderObrSelector = () => {
    return (
      <SelectItemNew
        id={"cardObrSelect"}
        dynamicHeight={false}
        maxHeight={'80px'}
        allowCreate
        searchable
        hasOutline={false}
        placeholder={"Select a remote OBR"}
        options={this.state.selectorItems}
        selectedItem={this.props.selectedObr}
        onChange={this.onSelectObr}
        maxNumOfOptions={6}
        isValidNewOption={this.checkNewReleaseOption}
        labelHasHelpIcon
        labelHelpIconTooltipProps={{ text: 'Select an official OBR release or enter a relative OBR path of the sandbox-mvn-candidates repository'}}
        label={'Current OBR'}

      />
    );
  };

  render() {
    console.log("<DashBoard /> props ", this.props);
    const featureText = this.props.nbFeatures > 1 ? "Features" : "Feature";
    const runtimeText = this.props.nbRuntimes > 1 ? "Runtimes" : "Runtime";
    const routeText = this.props.nbRoutes > 1 ? "Routes" : "Route";
    const functionText = this.props.nbFunctions > 1 ? "Functions" : "Function";
    const toolbarh = 64;
    const cardcolh = this.props.height - toolbarh;
    let rowHeight = Math.floor((cardcolh - 13 * 12) / 11); // 12px as row separator and 11 rows
    rowHeight = Math.max(48, rowHeight);
    //       console.log("rowHeight+++++++++++++++ ", rowHeight)
    return (
      <div
        style={{
          height: this.props.height,
          width: "auto",
          position: "relative",
          minWidth: "588px"
        }}
      >
        <ToolBar pageTitle={"Dashboard"} />
        <div style={{}}>
          <CardCollection
            height={cardcolh}
            rowHeight={rowHeight}
            dynamicWidth={true}
            margin={[16, 12]}
            padding={[12, 12]}
            layout={[
              { x: 0, y: 0, w: 1, h: 4, i: "0" },
              { x: 0, y: 2, w: 1, h: 4, i: "1" },
              { x: 0, y: 6, w: 1, h: 5, i: "2" },
              { x: 0, y: 6, w: 1, h: 5, i: "3" }
            ]}
            cols={2}
          >
            <Card
              id={"cardObr"}
              className="card"
              autoResize={true}
              css={{ height: "100%", width: "auto" }}
            >
              {this.renderObrSelector()}
            </Card>
            <Card
              id={"cardFeaturesUsed"}
              className="card"
              autoResize={true}
              css={{ height: "100%", width: "auto" }}
            >
              <Label id={"cardFeaturesUsedLabel"} text={"Contact us"} />
              <ul style={{ listStyleType: "none" }}>
                <li style={{ paddingBottom: "20px" }}>
                  <p>
                    <span className={"dashboard-contact"}>{"Support :"}</span>
                  </p>
                  <p>
                    <a
                      className={"dashboard-contact"}
                      href={
                        "https://greenhopper.app.alcatel-lucent.com/browse/CSFS"
                      }
                      target={"_blank"}
                    >
                      {"CSFS Support Request"}
                    </a>
                  </p>
                </li>
                <li>
                  <p>
                    <span className={"dashboard-contact"}>
                      {"Confluence :"}
                    </span>
                  </p>
                  <p>
                    <a
                      className={"dashboard-contact"}
                      href={
                        "https://confluence.app.alcatel-lucent.com/display/plateng/CASR+-+MicroFeatures"
                      }
                      target={"_blank"}
                    >
                      {"CASR - MicroFeatures"}
                    </a>
                  </p>
                </li>
              </ul>
            </Card>

            <Card
              id={"cardStatus"}
              className="card"
              autoResize={true}
              css={{ height: "100%", width: "auto" }}
            >
              <Label id={"cardStatusLabel"} text={"Current status"} />
              <table>
                <tbody>
                  <tr>
                    <td>
                      <div style={{ textAlign: "center" }}>
                        <Image
                          internalSrc={"ic_matrix.svg"}
                          alt={"features"}
                          width={"80px"}
                        />
                        <p>
                          <span className={"dashboard-nb-entities"}>
                            {this.props.nbFeatures}
                          </span>
                        </p>
                        <p>
                          <span className={"dashboard-entity-name"}>
                            {featureText}
                          </span>
                        </p>
                      </div>
                    </td>
                    <td>
                      <div style={{ textAlign: "center" }}>
                        <Image
                          internalSrc={"ic_rack.svg"}
                          alt={"runtimes"}
                          width={"80px"}
                        />
                        <p>
                          <span className={"dashboard-nb-entities"}>
                            {this.props.nbRuntimes}
                          </span>
                        </p>
                        <p>
                          <span className={"dashboard-entity-name"}>
                            {runtimeText}
                          </span>
                        </p>
                      </div>
                    </td>
                    <td>
                      <div style={{ textAlign: "center" }}>
                        <Image
                          internalSrc={"ic_managed_routes.svg"}
                          alt={"routes"}
                          width={"80px"}
                        />
                        <p>
                          <span className={"dashboard-nb-entities"}>
                            {this.props.nbRoutes}
                          </span>
                        </p>
                        <p>
                          <span className={"dashboard-entity-name"}>
                            {routeText}
                          </span>
                        </p>
                      </div>
                    </td>
                    <td>
                      <div style={{ textAlign: "center" }}>
                        <Image
                          internalSrc={"ic_health.svg"}
                          alt={"functions"}
                          width={"80px"}
                        />
                        <p>
                          <span className={"dashboard-nb-entities"}>
                            {this.props.nbFunctions}
                          </span>
                        </p>
                        <p>
                          <span className={"dashboard-entity-name"}>
                            {functionText}
                          </span>
                        </p>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </Card>
            <Card
              id={"cardConsistency"}
              className="card"
              autoResize={true}
              css={{ height: "100%", width: "auto" }}
            >
              <Label id={"cardConsistencyLabel"} text={"Routes consistency"} />
              <DataConsistency
                nbRoutes={this.props.nbRoutes}
                nbRuntimes={this.props.nbRuntimes}
                consistency={this.props.dataConsistency}
              />
            </Card>
          </CardCollection>
        </div>
      </div>
    );
  }
}

//
// CONNECTED TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function(state) {
  return {
    selectedObr: entitiesSelectors.getSelectedObr(state),
    remoteObrs: entitiesSelectors.getRemoteObrs(state),
    nbFeatures: Object.keys(entitiesSelectors.getFeatures(state)).length,
    nbRuntimes: entitiesSelectors.getRuntimeIds(state).length,
    nbRoutes: entitiesSelectors.getRouteIds(state).length,
    nbFunctions: entitiesSelectors.getFunctionIds(state).length,
    dataConsistency: entitiesSelectors.getDataConsistency(state)
  };
};

const mapDispatchToProps = function(dispatch) {
  return bindActionCreators(
    {
      selectObr: entitiesOperations.selectObr
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DashBoard);
