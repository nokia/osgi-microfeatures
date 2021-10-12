/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import DataGrid from "@nokia-csf-uxr/csfWidgets/DataGrid";
import docicon from "@nokia-csf-uxr/csfWidgets/images/ic_file.svg";
import { utils } from "../../../utilities";
import StatefulSearchwChips from "../../commons/StatefulSearchwChips";

class FeaturesList extends Component {
  constructor(props) {
    super(props);
    this.dopreselect = false;
    // Columns definition
    this.columnDefs = [
      {
        headerName: "Feature Name",
        field: "name",
        width: 150,
        headerCheckboxSelection: true,
        headerCheckboxSelectionFilteredOnly: true,
        checkboxSelection: true
      },
      { headerName: "Category", field: "categories", width: 120 },
      { headerName: "Version", field: "version", width: 90 }
    ];
    // Hold current queries for Search
    this.filterQueries = [];
    // Grid options for <DataGrid />
    this.gridOptions = {
      columnDefs: this.columnDefs,
      deltaRowDataMode: true,
      getRowNodeId: data => data.fid,
      rowAction: {
        types: [{ name: "Doc", icon: docicon }],
        callback: this.onAction,
        disable: params => {
          const action = params.name;
          const data = params.data;
          if (action === "Doc" && (data.url === undefined || data.url === "")) {
            return true;
          }
          return false;
        }
      },
      /*
      showToggleActionColumn: true,
      toggleAction: {
        icon: toggleicon,
        headerName: "Assembly selection",
        width: 60,
        minWidth: 50,
        index: 0, // column index where this column has to appear in datagrid.
        field: "selected", // this should be same as rowData field.
        toggleCallback: this.onToggleCallback,
        suppressMenu: false,
        suppressFilter: false,
        floatingFilter: false,
        menuTabs: ["generalMenuTab"]
      },
      multiactiontoolbarToggleIcon: toggleicon,
      multiactiontoolbarCallback: function(params) {
        console.log("MultiActionToolbar Toggle State is ", params.state);
      },
*/
      isExternalFilterPresent: this.isExternalFilterPresent,
      doesExternalFilterPass: this.doesExternalFilterPass
    };
  }


  componentDidUpdate(prevProps, prevState) {
    this.presetSelected();
  }

  /**
   * gridOptions Callback on actions ( see gridOptions.rowAction.types )
   */
  onAction = event => {
    const params = event.value; // from 17.1.0
    console.log("-->" + params.name + " ACTION", params);
    if (params.name === "Doc" && params.items.length > 0) {
      let url = params.items[0].data.url;
      this.openOnce(url);
    }
  };

  openOnce = url => {
    let target = utils.hashCode(url);
    let winref = window.open("", target, "", true);
    try {
      if (winref.location.href === "about:blank") {
        winref.location.href = url;
      }
    } catch (e) {
      console.log("openOnce", e);
    }
    return winref;
  };
  /*
  onToggleCallback = event => {
    const params = event.value; // from 17.1.0
    console.log("toggle state", params);
    console.log(
      "row: " + params.rowIndex + ", data: " + JSON.stringify(params.rowData)
    );
    this.props.toggleFeature(params.rowData.fid);
  };
*/

  onSelectionChanged = event => {
    const fids = this._gridApi.getSelectedNodes().map(n => n.id);
    console.log("onSelectionChanged Selection changed fids", fids);
    this.props.setSelectedFeatures(fids, null);
  };

  presetSelected = () => {
    if (this._gridApi !== undefined) {
      this._gridApi.forEachNode(node => {
        if (node.data.selected !== node.selected) {
          console.log("presetSelected node id", node.id, node.data.selected);
          node.setSelected(!!node.data.selected);
        }
      });
    } else {
      console.log("presetSelected CANT PRESELECT FEATURES, GRIDAPI NOT READY!");
      this.dopreselect = true;
    }
  };

  /**
   * gridOptions Callback allows to use external filter
   * When isExternalFilterPresent callback has queries, then it triggs
   * the doesExternalFilterPass callback to perform filtering on each row data
   */
  isExternalFilterPresent = () =>
    this.filterQueries && this.filterQueries.length > 0;

  /**
   * gridOptions Callback to perform filtering on each row data
   * The search on each assembly is performed also on its features
   */
  doesExternalFilterPass = node => {
    const { data } = node;
    return this.doesFilterPassOnData(data);
  };

  /**
   * Applies filtering on any data those attributes are also defined in
   * columnDefs
   */
  doesFilterPassOnData = data => {
    for (let iterm = 0; iterm < this.filterQueries.length; iterm += 1) {
      const term = this.filterQueries[iterm].queryTerm;
      for (let icol = 0; icol < this.columnDefs.length; icol += 1) {
        const col = this.columnDefs[icol];
        let value = data[col.field];
        if (value) {
          value = value.toString().toLowerCase();
          if (value.indexOf(term.toLowerCase()) > -1) {
            //                        console.log("doesFilterPassOnData return true", value, term);
            return true;
          }
        }
      }
    }
    return false;
  };

  /**
   * SearchwChips callback: Trigged when the user enter a new keyword in the search input
   */
  onUpdate = data => {
    this.filterQueries = data;
    this._gridApi.onFilterChanged();
  };

  render() {
    console.log("<FeaturesList /> render() props", this.props);
    return (
      <DataGrid
        id={'myfeatureslist'}
        gridOptions={this.gridOptions}
        onGridReady={this._onGridReady}
        rowData={this.props.featuresListData}
        onSelectionChanged={this.onSelectionChanged}
      >
        <StatefulSearchwChips
          id={"searchwChipsFeatures"}
          placeHolder={"Search.."}
          onUpdate={this.onUpdate}
        />
      </DataGrid>
    );
  }


  _onGridReady = (params) => {
      this._gridApi = params.value.api;
      console.log("_onGridReady this._gridApi SETTED ==========");
      this._gridColumnApi = params.value.columnApi;
      this._gridApi.sizeColumnsToFit();
      if( this.dopreselect ) {
        console.log("_onGridReady DO PRESELECT NOW!");
        this.presetSelected();
      }
    };
}

export default FeaturesList;
