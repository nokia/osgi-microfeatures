import React, { Component } from 'react';
import DataGrid from '@nokia-csf-uxr/csfWidgets/DataGrid';
import StatefulSearchwChips from '../../commons/StatefulSearchwChips';
import runtimeicon from '@nokia-csf-uxr/csfWidgets/images/ic_file_download.svg';
import deleteicon from '@nokia-csf-uxr/csfWidgets/images/ic_delete.svg';

const Assembly = (props) => (
    <tr>
{ /*        <td>{props.name}</td> */ }
        <td>{props.bsn}</td>
        <td>{props.version}</td>
    </tr>
)

class RowRenderer extends Component {

    render() {
        console.log('<RowRenderer /> props', this.props)
        const assembly = { bsn :this.props.data.asmbbsn, version :this.props.data.version }
        return (
            <div style={{ width: '100%', marginLeft: '34px' }}>
                <h4>{'Assembly'}</h4>
                <ul>
                    <table className={'extendedAssembly'}>
                        <tbody>
                            <Assembly {...assembly} />
                        </tbody>
                    </table>
                </ul>
            </div>
        )
    }
}

class SnapshotsList extends Component {
    constructor(props) {
        super(props);
        // Columns definition
        this.columnDefs = [
            { headerName: "Snapshot Name", field: "name", width: 150, cellRenderer: 'agGroupCellRenderer' },
            { headerName: "Bundle Symbolic Name", field: "bsn", width: 150 },
            { headerName: "Version", field: "version", width: 90 }
        ]
        // Hold current queries for Search
        this.filterQueries = [];
        // Grid options for <DataGrid />
        this.gridOptions = {
            columnDefs: this.columnDefs,
            deltaRowDataMode: true,
            getRowNodeId: (data) => data.fid,
            rowAction: {
                types: [
                    { name: 'Create runtime', icon: runtimeicon },
                    { name: 'Delete', icon: deleteicon }
                ],
                callback: this.onAction,
                disable: (params) => false
            },
            animateRows: true,
            doesDataFlower: () => true,
            isFullWidthCell: (rowNode) => (rowNode.level === 1),
            getRowHeight: (params) => (params.node.level === 1 ? 50 + (22 * 1) : 46),
            fullWidthCellRendererFramework: RowRenderer,
            isExternalFilterPresent: this.isExternalFilterPresent,
            doesExternalFilterPass: this.doesExternalFilterPass
        }
    }

    /**
     * gridOptions Callback on actions ( see gridOptions.rowAction.types )
     */
    onAction = (event) => {
        const params = event.value; // from 17.1.0
        const getPayload = (assembly) => ({
            name: assembly.name,
            version: assembly.version,
            bsn: assembly.bsn
        })

        if (params.name === 'Delete' && params.items.length > 0) {
            this.props.deleteSnapshot(getPayload(params.items[0].data));
            return;
        }
        if (params.name === 'Create runtime' && params.items.length > 0) {
            this.props.createRuntime(getPayload(params.items[0].data));
            return;
        }
    }

    /**
     * gridOptions Callback allows to use external filter
     * When isExternalFilterPresent callback has queries, then it triggs
     * the doesExternalFilterPass callback to perform filtering on each row data
     */
    isExternalFilterPresent = () => (this.filterQueries && this.filterQueries.length > 0);

    /**
     * gridOptions Callback to perform filtering on each row data
     * The search on each assembly is performed also on its features
     */
    doesExternalFilterPass = (node) => {
        const { data } = node;
        let found = this.doesFilterPassOnData(data);
        if( !found ) {
            const assembly = { bsn :data.asmbbsn, version :data.version }
            found = this.doesFilterPassOnData(assembly);
        }
        return found;
    }

    /**
     * Applies filtering on any data those attributes are also defined in
     * columnDefs
     */
    doesFilterPassOnData = (data) => {
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
        return false
    }



    /**
     * SearchwChips callback: Trigged when the user enter a new keyword in the search input
     */
    onUpdate = (data) => {
        this.filterQueries = data;
        this._gridApi.onFilterChanged();
    }

    render() {
        console.log("<SnapshotsList /> render() props", this.props)
        return (
            <DataGrid
                id={'datagridSnapshots'}
                gridOptions={this.gridOptions}
                onGridReady={this._onGridReady()}
                rowData={this.props.snapshotsListData}
            >
                <StatefulSearchwChips
                    id={"searchwChipsSnapshots"}
                    placeHolder={"Search.."}
                    onUpdate={this.onUpdate}
                />
            </DataGrid>
        )
    }

    /**
     * Callback that gets called when grid becomes ready.
     * @returns {Function} which makes gridApi available.
     */
    _onGridReady = () => {
        return (event) => {
            const params = event.value;
            this._gridApi = params.api;
            this._gridColumnApi = params.columnApi;
            this._gridApi.sizeColumnsToFit();
        };
    }
}

export default SnapshotsList