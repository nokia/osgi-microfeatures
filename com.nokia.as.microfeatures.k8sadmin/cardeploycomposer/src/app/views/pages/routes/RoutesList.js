import React, { Component } from "react";
import DataGrid from "@nokia-csf-uxr/csfWidgets/DataGrid";
import deleteicon from "@nokia-csf-uxr/csfWidgets/images/ic_delete.svg";
import StatefulSearchwChips from "../../commons/StatefulSearchwChips";

const Param = props => (
  <tr>
    <td className={'shift'}></td>
    <td>{props.name}</td>
    <td>{props.value}</td>
  </tr>
);

class RowRenderer extends Component {
  constructor(props) {
    super(props);
    console.log("RowRenderer props", this.props);
    this.nbParams = `Route Params Count: ${this.props.data.paramsList.length}`;
    this.nbFuncParams = `Function params Count: ${
      this.props.data.functionParamsList.length
    }`;
  }
  render() {
    console.log("<RowRenderer /> props", this.props);
    const { paramsList, functionParamsList } = this.props.data;

    return (
      <div style={{ width: "50%" }}>
      <ul>
      <table className={'extendedRoute'} >
              <tbody>
                <tr><td colSpan='3'><h4>{this.nbParams}</h4></td></tr>
                {paramsList.map(p => (
                  <Param key={p.name} {...p} />
                ))}
                <tr><td colSpan='3'><h4>{this.nbFuncParams}</h4></td></tr>
                {functionParamsList.map(p => (
                    <Param key={p.name} {...p} />
                ))}
              </tbody>
      </table>
      </ul>
      </div>
    );
  }
}

class RoutesList extends Component {
  constructor(props) {
    super(props);
    // Columns definition
    this.columnDefs = [
      {
        headerName: "Name",
        field: "name",
        width: 100,
        cellRenderer: "agGroupCellRenderer"
      },
      { headerName: "Type", field: "type", width: 80 },
      { headerName: "Path", field: "path", width: 90 },
      {
        headerName: "Function",
        field: "functionId",
        width: 90,
        cellClass: this.getFunctionCssClass
      },
      {
        headerName: "Runtime",
        field: "rid",
        valueGetter: this.getRuntimeValue,
        width: 90,
        cellClass: this.getRuntimeCssClass
      },
      {
        headerName: 'Execution timeout',
        field: 'ttl',
        valueFormatter: params => params.value + ' ms',
        width: 80
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
              22 * params.data.functionParamsList.length
          : 46;
      },
      fullWidthCellRendererFramework: RowRenderer,
      isExternalFilterPresent: this.isExternalFilterPresent,
      doesExternalFilterPass: this.doesExternalFilterPass
    };
  }

  getFunctionCssClass = params => {
    return this.props.functionIds.includes(params.value) ? "" : "redcolor";
  };

  getRuntimeCssClass = params => {
    const runtimes = params.data.runtimesList;
    if (!runtimes || runtimes.length === 0) return "";
    return this.props.runtimeIds.includes(runtimes[0].name) ? "" : "redcolor";
  };

  getRuntimeValue = params => {
    console.log("getRuntimeValue", params);
    const runtimes = params.data.runtimesList;
    if (!runtimes || runtimes.length === 0) return "all runtimes";
    return runtimes[0].name;
  };

  /**
   * gridOptions Callback on actions ( see gridOptions.rowAction.types )
   */
  onAction = event => {
    const params = event.value; // from 17.1.0
    const route = params.items[0].data;

    if (params.name === "Delete" && params.items.length > 0) {
      this.props.deleteRoute({
        name: route.name,
        type: route.type
      });
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
        console.log("doesExternalFilterPass param", param, found);
        if (found) break;
      }
    }
    /*
    if (!found) {
      for (let i = 0; i < data.runtimesList.length; i += 1) {
        let runtime = data.runtimesList[i];
        found = this.doesFilterPassOnData(runtime);
        console.log("doesExternalFilterPass runtime",runtime,found);
        if (found) break;
      }
    }
*/
    return found;
  };

  /**
   * Applies filtering on any data those attributes are also defined in
   * columnDefs
   */
  doesFilterPassOnData = (data, coldef) => {
    const columnDefs = coldef ? coldef : this.columnDefs;
    console.log();
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
    console.log("<RoutesList /> render() props", this.props);
    return (
      <DataGrid
        gridOptions={this.gridOptions}
        onGridReady={this._onGridReady()}
        rowData={this.props.routesListData}
      >
        <StatefulSearchwChips
          id={"searchwChipsRoutes"}
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

export default RoutesList;
