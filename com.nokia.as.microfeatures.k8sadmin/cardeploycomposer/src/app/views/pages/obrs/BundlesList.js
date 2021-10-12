/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import DataGrid from '@nokia-csf-uxr/csfWidgets/DataGrid';

import findDepsIcon from '@nokia-csf-uxr/csfWidgets/images/ic_find_in_page.svg';
import resolveIcon from '@nokia-csf-uxr/csfWidgets/images/ic_maintenance.svg';
import StatefulSearchwChips from '../../commons/StatefulSearchwChips';

class BundlesList extends Component {

    constructor(props) {
        super(props);
        // Columns definition
        this.columnDefs = [
            { headerName: "Bundle Symbolic Name", field: "bsn", width: 100 },
            { headerName: "Version", field: "version", width: 90 }
        ]
        // Hold current queries for Search
        this.filterQueries = [];
        // Grid options for <DataGrid />
        this.gridOptions = {
            columnDefs: this.columnDefs,
            deltaRowDataMode: true,
            getRowNodeId: (data) => data.fid,
            isExternalFilterPresent: this.isExternalFilterPresent,
            doesExternalFilterPass: this.doesExternalFilterPass
        }

        // rowAction is only pertinent for remote obr ( not local )
        if (this.props.local !== true) {
            this.gridOptions['rowAction'] = {
                types: [
                    { name: 'Resolve', icon: resolveIcon },
                    { name: 'Find dependent', icon: findDepsIcon }
                ],
                callback: this.onAction,
                disable: (params) => false
            }
        }
    }

    componentDidUpdate(prevProps, prevState) {
        if( prevProps.bundles.length === 0 && this.props.bundles.length > 0 ) {
            this._gridApi.sizeColumnsToFit();
        }
    }

    /**
     * gridOptions Callback on actions ( see gridOptions.rowAction.types )
     */
    onAction = (event) => {
        const params = event.value; // from 17.1.0
        console.log('-->' + params.name + ' ACTION', params);
        if (params.items.length < 1) return;
        const data = params.items[0].data;
        if (params.name === 'Resolve') {
            console.log("Resolve", data);
            this.props.onResolve(data.fid)
            return;
        } else {
            console.log("Find dependent", data);
            this.props.onFindDeps(data.fid)
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
        return this.doesFilterPassOnData(data);
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
        console.log("<BundlesList /> render() props", this.props)
        let scid = (this.props.local) ? '-local' : '-current'
        scid = 'searchwChipsBundles' + scid
        return (
            <DataGrid
                //                id={'bundlesList'}
                gridOptions={this.gridOptions}
                onGridReady={this._onGridReady()}
                rowData={this.props.bundles}
            >
                <StatefulSearchwChips
                    id={scid}
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
            console.log("sizeColumnsToFit ...................................................++++++++++++++++++");
            this._gridApi.sizeColumnsToFit();
        };
    }
}

export default BundlesList;