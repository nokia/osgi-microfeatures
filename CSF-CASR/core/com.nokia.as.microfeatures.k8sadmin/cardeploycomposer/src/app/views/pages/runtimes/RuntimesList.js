import React, { Component } from "react";
import DataGrid from "@nokia-csf-uxr/csfWidgets/DataGrid";
import StatefulSearchwChips from "../../commons/StatefulSearchwChips";
import Button from "@nokia-csf-uxr/csfWidgets/Button";
import detailicon from "@nokia-csf-uxr/csfWidgets/images/ic_info_outline.svg";
import deleteicon from "@nokia-csf-uxr/csfWidgets/images/ic_delete.svg";

const Pod = props => {
  console.log("<Pod props", props);
  const { fid, startTerminal, name, url, status } = props;

  const compatibleDomFid = fid.replace("@", "-");
  const btnId = `${compatibleDomFid}${name}`;

  const openTerminal = () => startTerminal(fid, name, url);

  return (
    <tr>
      <td>
        <Button
          id={btnId}
          isCallToAction
          disableRipple
          icon="ic_admin_up"
          onClick={openTerminal}
          tooltip={{
            balloon: false,
            text: "Launch Terminal",
            displayOnFocus: false
          }}
          aria-label="Launch Terminal"
        />
      </td>
      <td>{name}</td>
      <td>{status}</td>
    </tr>
  );
};

class RowRenderer extends Component {
  constructor(props) {
    super(props);
    this.nbPods = `Pod Count: ${this.props.data.podsUrls.length}`;
  }
  render() {
    console.log("<RowRenderer /> props", this.props);
    const { fid, podsUrls } = this.props.data;
    const startTerminal = this.props.context.componentParent.props.startTerminal;
    const props = {
      fid: fid,
      startTerminal: startTerminal
    };

    return (
      <div style={{ width: "100%", marginLeft: "34px" }}>
        <h4>{this.nbPods}</h4>
        <ul>
          <table className={"extendedRuntime"}>
            <tbody>
              {podsUrls.map(pod => (
                <Pod key={pod.name} {...pod} {...props} />
              ))}
            </tbody>
          </table>
        </ul>
      </div>
    );
  }
}

class RuntimesList extends Component {
  constructor(props) {
    super(props);

    // Columns definition
    this.columnDefs = [
      {
        headerName: "Runtime Name",
        field: "name",
        width: 150,
        cellRenderer: "agGroupCellRenderer"
      },
      { headerName: "Runtime Namespace", field: "namespace", width: 150 },
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
        types: [
          { name: "See details", icon: detailicon },
          { name: "Undeploy", icon: deleteicon }
        ],
        callback: this.onAction,
        disable: params => false
      },
      animateRows: true,
      doesDataFlower: () => true,
      isFullWidthCell: rowNode => rowNode.level === 1,
      getRowHeight: params =>
        params.node.level === 1 ? 50 + 40 * params.data.podsUrls.length : 46,
      fullWidthCellRendererFramework: RowRenderer,
      isExternalFilterPresent: this.isExternalFilterPresent,
      doesExternalFilterPass: this.doesExternalFilterPass
    };
  }

  /**
   * gridOptions Callback on actions ( see gridOptions.rowAction.types )
   */
  onAction = event => {
    const params = event.value; // from 17.1.0
    const runtime = params.items[0].data;
    if (params.name === "Undeploy" && params.items.length > 0) {
        this.props.undeployRuntime({
          name: runtime.name,
          namespace: runtime.namespace
        });
      return;
    }
    if (params.name === "See details" && params.items.length > 0) {
      this.props.openDetails(runtime.fid);
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
   * The search on each runtime is performed also on its features
   */
  doesExternalFilterPass = node => {
    const { data } = node;
    let found = this.doesFilterPassOnData(data);
    if (!found) {
      for (let i = 0; i < data.features.length; i += 1) {
        let feature = data.features[i];
        found = this.doesFilterPassOnData(feature);
        if (found) break;
      }
    }
    return found;
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
    console.log("<RuntimesList /> render() props", this.props);
    console.log("window.innerWidth", window.innerWidth - 200);
    return (
      <DataGrid
        id={"datagridRuntimes"}
        gridOptions={this.gridOptions}
        onGridReady={this._onGridReady()}
        rowData={this.props.runtimesListData}
        context={{ componentParent: this }}
      >
        <StatefulSearchwChips
          id={"searchwChipsRuntimes"}
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

export default RuntimesList;
