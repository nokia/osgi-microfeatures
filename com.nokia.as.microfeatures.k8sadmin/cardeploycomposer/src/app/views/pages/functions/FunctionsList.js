/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import DataGrid from "@nokia-csf-uxr/csfWidgets/DataGrid";
import deleteicon from "@nokia-csf-uxr/csfWidgets/images/ic_delete.svg";
import StatefulSearchwChips from "../../commons/StatefulSearchwChips";
import LazyRenderer from "./LazyRenderer";

const Param = props => (
  <tr>
    <td className={'shift'}></td>
    <td>{props.name}</td>
    <td>{props.value}</td>
  </tr>
);

const Location = props => (
  <tr>
    <td className={'shift'}></td>
    <td colSpan={2}>{props.value}</td>
  </tr>
);

class RowRenderer extends Component {
  constructor(props) {
    super(props);
    console.log("RowRenderer props", this.props);
    this.nbParams = `Params Count: ${this.props.data.paramsList.length}`;
    this.nbLocations = `Location Count: ${
      this.props.data.locationsList.length
    }`;
  }
  render() {
    console.log("<RowRenderer /> props", this.props);
    const { paramsList, locationsList } = this.props.data;

    return (
      <div style={{ width: "50%" }}>
        <ul>
          <table className={"extendedRoute"}>
            <tbody>
              <tr>
                <td colSpan="3">
                  <h4>{this.nbLocations}</h4>
                </td>
              </tr>
              {locationsList.map(l => (
                <Location key={l.id} {...l} />
              ))}
              <tr>
                <td colSpan="3">
                  <h4>{this.nbParams}</h4>
                </td>
              </tr>
              {paramsList.map(p => (
                <Param key={p.name} {...p} />
              ))}
            </tbody>
          </table>
        </ul>
      </div>
    );
  }
}

class FunctionsList extends Component {
  constructor(props) {
    super(props);
    // Columns definition
    this.columnDefs = [
      {
        headerName: "Name",
        field: "name",
        width: 90,
        cellRenderer: "agGroupCellRenderer"
      },
      {
        headerName: "Lazy / Inactivity Timeout (seconds)",
        field: "lazy",
        valueGetter: this.lazyGetter,
        cellRendererFramework: LazyRenderer,
        width: 50
      },
      { headerName: "Status", field: "status", width: 90 }
    ];
    // Hold current queries for Search
    this.filterQueries = [];
    // Grid options for <DataGrid />
    this.gridOptions = {
      columnDefs: this.columnDefs,
      deltaRowDataMode: true,
      getRowNodeId: data => data.fid,
      rowAction: {
        types: [{ name: "Delete", icon: deleteicon }],
        callback: this.onAction,
        disable: params => false
      },
      animateRows: true,
      doesDataFlower: () => true,
      isFullWidthCell: rowNode => rowNode.level === 1,
      getRowHeight: params => {
        console.log("getRowHeight", params.data);
        return params.node.level === 1
          ? 62 +
              22 * params.data.paramsList.length +
              22 * params.data.locationsList.length
          : 46;
      },
      fullWidthCellRendererFramework: RowRenderer,
      isExternalFilterPresent: this.isExternalFilterPresent,
      doesExternalFilterPass: this.doesExternalFilterPass
    };
  }

  lazyGetter = params => {
    const { lazy, timeout } = params.data;
    return { lazy, timeout };
  };

  /**
   * gridOptions Callback on actions ( see gridOptions.rowAction.types )
   */
  onAction = event => {
    const params = event.value; // from 17.1.0
    const funcTion = params.items[0].data;

    if (params.name === "Delete" && params.items.length > 0) {
      this.props.deleteFunction({ name: funcTion.name });
      return;
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
    let found = this.doesFilterPassOnData(data);
    if (!found) {
      for (let i = 0; i < data.paramsList.length; i += 1) {
        let param = data.paramsList[i];
        found = this.doesFilterPassOnData(param, [
          { field: "name" },
          { field: "value" }
        ]);
        if (found) break;
      }
    }
    if (!found) {
      for (let i = 0; i < data.locationsList.length; i += 1) {
        let location = data.locationsList[i];
        found = this.doesFilterPassOnData(location, [{ field: "value" }]);
        if (found) break;
      }
    }
    return found;
  };

  /**
   * Applies filtering on any data those attributes are also defined in
   * columnDefs
   */
  doesFilterPassOnData = (data, coldef) => {
    const columnDefs = coldef ? coldef : this.columnDefs;
    for (let iterm = 0; iterm < this.filterQueries.length; iterm += 1) {
      const term = this.filterQueries[iterm].queryTerm;
      for (let icol = 0; icol < columnDefs.length; icol += 1) {
        const col = columnDefs[icol];
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
    console.log("<FunctionsList /> render() props", this.props);
    return (
      <DataGrid
        gridOptions={this.gridOptions}
        onGridReady={this._onGridReady()}
        rowData={this.props.functionsListData}
      >
        <StatefulSearchwChips
          id={"searchwChipsFunctions"}
          placeHolder={"Search.."}
          onUpdate={this.onUpdate}
        />
      </DataGrid>
    );
  }

  /**
   * Callback that gets called when grid becomes ready.
   * @returns {Function} which makes gridApi available.
   */
  _onGridReady = () => {
    return event => {
      const params = event.value;
      this._gridApi = params.api;
      this._gridColumnApi = params.columnApi;
      this._gridApi.sizeColumnsToFit();
    };
  };
}

export default FunctionsList;
