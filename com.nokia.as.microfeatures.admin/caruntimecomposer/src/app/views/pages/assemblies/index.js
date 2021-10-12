/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { createSelector } from 'reselect'
import { merge } from 'lodash'
import { entitiesSelectors } from '../../../state/ducks/entities'
import { assemblySelectors, assemblyOperations } from '../../../state/ducks/assembly'
import { snapshotSelectors, snapshotOperations } from '../../../state/ducks/snapshot'

import AlertDialogConfirm from '@nokia-csf-uxr/csfWidgets/AlertDialogConfirm';

import ToolBar from '../../commons/ToolBar';
import RcStatusIndicator from '../../commons/RcStatusIndicator';

import AssembliesList from './AssembliesList'

class Assemblies extends Component {
    constructor(props) {
        super(props)
        this.toolbarh = 69;
    }

    getListHeight = () => this.props.height - (this.toolbarh + 40);

    render() {
        console.log("<Assemblies /> props ", this.props)
        const assembly = this.props.payloadDelete;
        const { name , bsn , version } = assembly;
        const deleteInfo1 = "'" +name + "' v:" + version
        const deleteInfo2 = 'Bsn : ' + bsn;
        const isDeleting = this.props.deleteAssemblyStatus === 'started'
        
        return (
            <div style={{ height: this.props.height, position: 'relative' }} >
                <ToolBar pageTitle={'Assemblies'} />
                { this.props.createStatus === 'started' &&
                    <div id='createsnapshotoverlay' className={'csfWidgets overlay active black'} />
                }
                <RcStatusIndicator
                    id={'snapshotIndicator'}
                    clearText={'SNAPSHOT SUCCESSFULLY CREATED'}
                    requestStatus={this.props.createStatus}
                />
                { isDeleting === true &&
                    <div id='delasmboverlay' className={'csfWidgets overlay active black'} />
                }
                {this.props.openConfirmDelete &&
                    <AlertDialogConfirm
                        title={'Do you want to delete this assembly?'}
                        confirmationText1={deleteInfo1}
                        confirmationText2={deleteInfo2}
                        confirmationButtonLabel={'DELETE'}
                        onClose={this.props.cancelDeleteAssembly}
                        onConfirm={this.props.confirmDeleteAssembly}
                    />
                }
                <div id="assemblies-rows" style={{ height: this.getListHeight(), position: 'relative' }} >
                    <AssembliesList
                        assembliesListData={this.props.assembliesListData}
                        deleteAssembly={this.props.deleteAssembly}
                        confirmDelete={this.props.confirmDeleteAssembly}
                        cancelDelete={this.props.cancelDeleteAssembly}
                        createSnapshot={this.props.createSnapshot}
                        width={this.props.width}
                    />
                </div>
            </div>
        );
    }
}

const getAssembliesList = (state) => createSelector(
    entitiesSelectors.getAllAssemblies,
    (assemblies) => {
        const asmbs = merge([], assemblies).map(asmb => {
            let features = entitiesSelectors.getAssemblyFeatures(state, asmb.fid);

            let newAsmb = Object.assign({}, asmb, { features: features });
            return newAsmb

        })

        console.log("getAssembliesList has worked!");
        return asmbs;
    }
)(state)


//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function (state) {
    return {
        assembliesListData: getAssembliesList(state),
        createStatus : snapshotSelectors.getCreateStatus(state),
        openConfirmDelete: assemblySelectors.doConfirmDeleteAssembly(state),
        payloadDelete: assemblySelectors.getDeletePayload(state),
        deleteAssemblyStatus: assemblySelectors.getDeleteStatus(state)
    }
}

const mapDispatchToProps = function (dispatch) {
    return bindActionCreators({
        ...assemblyOperations,
        ...snapshotOperations
    }, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Assemblies);
