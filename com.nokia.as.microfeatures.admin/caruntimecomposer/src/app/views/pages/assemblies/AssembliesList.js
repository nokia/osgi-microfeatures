/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import DataGrid from '@nokia-csf-uxr/csfWidgets/DataGrid';
import StatefulSearchwChips from '../../commons/StatefulSearchwChips';
import snapshotsicon from '@nokia-csf-uxr/csfWidgets/images/ic_snapshot.svg';
import deleteicon from '@nokia-csf-uxr/csfWidgets/images/ic_delete.svg';

const Feature = (props) => (
    <tr>
        <td>{props.name}</td>
        <td>{props.bsn}</td>
        <td>{props.version}</td>
    </tr>
)

class RowRenderer extends Component {
    constructor(props) {
        super(props);
        this.nbFeatures = `Features Count: ${this.props.data.features.length}`;
    }
    render() {
        console.log('<RowRenderer /> props', this.props)
        const features = this.props.data.features;
        return (
            <div style={{ width: '100%', marginLeft: '34px' }}>
                <h4>{this.nbFeatures}</h4>
                <ul>
                    <table className={'extendedAssembly'}>
                        <tbody>
                            {features.map(f =>
                                <Feature key={f.fid} {...f} />
                            )}
                        </tbody>
                    </table>
                </ul>
            </div>
        )
    }
}

class AssembliesList extends Component {
    constructor(props) {
        super(props);
        // Columns definition
        this.columnDefs = [
            { headerName: "Assembly Name", field: "name", width: 150, cellRenderer: 'agGroupCellRenderer' },
            { headerName: "Bundle Symbolic Name", field: "bsn", width: 150 },
            { headerName: "Version", field: "version", width: 90 },
            { headerName: "Nb of Features", width: 50, valueGetter: params => params.data.features.length }
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
                    { name: 'Snapshot', icon: snapshotsicon },
                    { name: 'Delete', icon: deleteicon }
                ],
                callback: this.onAction,
                disable: (params) => false
            },
            animateRows: true,
            doesDataFlower: () => true,
            isFullWidthCell: (rowNode) => (rowNode.level === 1),
            getRowHeight: (params) => (params.node.level === 1 ? 50 + (22 * params.data.features.length) : 46),
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
            this.props.deleteAssembly(getPayload(params.items[0].data));
            return;
        }
        if (params.name === 'Snapshot' && params.items.length > 0) {
            this.props.createSnapshot(getPayload(params.items[0].data));
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
            for (let i = 0; i < data.features.length; i += 1) {
                let feature = data.features[i];
                found = this.doesFilterPassOnData(feature);
                if( found )
                    break;
            }
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
        console.log("<AssembliesList /> render() props", this.props)
        console.log("window.innerWidth",window.innerWidth - 200)
        return (
            <DataGrid
                id={'datagridAssemblies'}
                gridOptions={this.gridOptions}
                onGridReady={this._onGridReady()}
                rowData={this.props.assembliesListData}
            >
                <StatefulSearchwChips
                    id={"searchwChipsAssemblies"}
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

export default AssembliesList