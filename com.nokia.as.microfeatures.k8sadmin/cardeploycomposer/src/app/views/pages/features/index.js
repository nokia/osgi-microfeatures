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
import { withRouter } from 'react-router-dom';
import { createSelector } from 'reselect'

import { runtimeSelectors, runtimeOperations } from '../../../state/ducks/runtime'
import { entitiesSelectors } from '../../../state/ducks/entities'

import { getObrVersion } from "../../../utilities/utils";

import ToolBar from '../../commons/ToolBar';
import StartFrom from './StartFrom'
import FeaturesList from './FeaturesList'
import QuickSel from './QuickSel'
import RcStatusIndicator from '../../commons/RcStatusIndicator';
import Button from "@nokia-csf-uxr/csfWidgets/Button";
import Label from "@nokia-csf-uxr/csfWidgets/Label/Label";

class Features extends Component {
    constructor(props) {
        super(props)
        this.toolbarh = 64;
        this.quickSelh = 67;
    }

    getListHeight = () => this.props.height - (this.toolbarh + this.quickSelh + 20);

    /**
     * Move to the deployment wizard page.
     */
    onDeployWizard = () => {
        const { isAdministrator } = this.props.configData;
        const newRoute = (isAdministrator === true)?`/deploywizard`:`/accessDenied`;
        this.props.history.push(newRoute);
    }

    render() {
        console.log("<Features /> props ", this.props)
        const obrVersion = `OBR release : ${getObrVersion(this.props.currentObr)}`
        return (
            <div style={{ height: this.props.height, position: 'relative'}} >
                <ToolBar pageTitle={'Features'} />
                <RcStatusIndicator
                    id={'deployIndicator'}
                    clearText={'DEPLOYING RUNTIME...'}
                    requestStatus={this.props.requestDeployStatus}
                />
                <div id="features-row1" >
                    <div>{'Quick selector:'}</div>
                    <div style={{ verticalAlign: 'middle' }}>
                        <QuickSel
                            quickSelOptions={this.props.quickSelOptions}
                            setQuickSelOption={this.props.setQuickSelOption}
                            quickSel={this.props.selectedQuick}
                        />
                    </div>
                    <div>{'Start from:'}</div>
                    <div style={{ verticalAlign: 'middle' }}>
                        <StartFrom
                            startFromData={this.props.runtimeOptionsList}
                            setRuntimeFeature={this.props.setRuntimeFeature}
                            runtimeFeatureId={this.props.runtimeFeatureId}
                        />
                    </div>
                    <div style={{ verticalAlign: 'middle' }}>
                        <Button
                            id="deployBtn"
                            text="DEPLOY RUNTIME"
                            isCallToAction
                            disabled={!this.props.canDeployRuntime}
                            onClick={this.onDeployWizard}
                        />
                    </div>
                    <div style={{ verticalAlign: 'middle', marginLeft: '5px' }}>
                        <Label id="featureCurObr" text={obrVersion} />
                    </div>
                    { this.props.isReleaseAdapted === false &&
                        <div style={{ verticalAlign: 'middle', marginLeft: '5px'}}>
                        <Label id="releaseWarnLabel" text="Not compliant to deploy runtimes." />
                    </div>
                    }
                </div>
                <div id="features-row2" style={{ height: this.getListHeight()}} >
                    <FeaturesList
                        featuresListData={this.props.featuresListData}
                        toggleFeature={this.props.toggleFeature}
                        setSelectedFeatures={this.props.setSelectedFeatures}
                        width={this.props.width}
                    />
                </div>
            </div>
        );
    }
}

//
// Specifics Selector for features view
//


// Returns list of runtimes={label=name,value=fid} 
const runtimeOptionsList = createSelector(
    entitiesSelectors.getAllFeatures,
    (features) => {
        const runtimes = features.filter(feature => feature.categories[0] === 'runtime')
        const data = []
        for (let i = 0; i < runtimes.length; i++) {
            data.push({ label: runtimes[i].name, value: runtimes[i].fid })
        }
        console.log("runtimeOptionsList has worked!");
        return data;
    }
)

const runtimeSelectedFeature = createSelector(
    entitiesSelectors.getAllFeatures,
    runtimeSelectors.getRuntimeFeatureId,
    (allfeatures, runtimeFeatureId) => {
        if (runtimeFeatureId === null)
            return []
        return allfeatures.filter(f => (f.fid === runtimeFeatureId))
    }
)

// Returns a list of sorted features excepting whose category is 'runtime'
// with all unselected items
// Remove runtime features considered as requirements (which will be automatically added when
// a runtime is deploying ).
const featuresSelector = createSelector(
    entitiesSelectors.getAllFeatures,
    runtimeSelectors.getRequiredFeatureIds,

    (allfeatures, reqFIds) => allfeatures.filter(f => f.categories[0] !== 'runtime' && reqFIds.includes(f.fid) !== true)
)

// Returns a list of adapted features respecting the user selections
const getFeaturesList = createSelector(
    [runtimeSelectors.getFeaturesIds, featuresSelector],
    (selectedIds, allfeatures) => {
        if (selectedIds.length < 1) {
            console.log("end-1 - getFeaturesList selector with no selected has worked!")
            return [...allfeatures];
        }
        // Applies the selection
        const newfeatures = [...allfeatures]

        const result = newfeatures.map(function (f) {
            return Object.assign({}, f, {
                selected: selectedIds.find(x => x === f.fid) !== undefined
            })
        })
        return result;
    }
)

const selectedFeatures = createSelector(
    getFeaturesList,
    (features) => features.filter(f => f.selected === true)
)

export const getAllSelectedFeatures = createSelector(
    runtimeSelectedFeature,
    selectedFeatures,
    (runtime, selecteds) => runtime.concat(selecteds)
)

const getQuickSelData = createSelector(
    entitiesSelectors.getRuntimes,
    (runtimes) => {
        const data = []
        data.push({ id: 'NONE', label: 'None', value: 'NONE' })
        data.push({ id: 'ALL', label: 'All', value: 'ALL' })

        const comparator = (f1, f2) => {
            const a = f1.id.toUpperCase();
            const b = f2.id.toUpperCase();
            return (a > b) ? 1 : ((b > a) ? -1 : 0)
        }
        const runtimechoices = []
        for (let key in runtimes) {
            let runtime = runtimes[key]
            runtimechoices.push({ id: key, label: runtime.name + '@' + runtime.namespace, value: key })
        }
        console.log("getQuickSelData has work!");
        return data.concat(runtimechoices.sort(comparator));
    }
)

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function (state) {
    return {
        isReleaseAdapted: runtimeSelectors.isDeployMetRequirements(state),
        runtimeFeatureId: runtimeSelectors.getRuntimeFeatureId(state),
        canDeployRuntime: runtimeSelectors.canDeployRuntime(state),
        featuresListData: getFeaturesList(state),
        runtimeOptionsList: runtimeOptionsList(state),
        allSelectedFeatures: getAllSelectedFeatures(state),
        quickSelOptions: getQuickSelData(state),
        selectedQuick: runtimeSelectors.getSelectedQuick(state),
        requestDeployStatus: runtimeSelectors.getDeployRequestState(state),
        currentObr: entitiesSelectors.getSelectedObr(state)
        
    }
}

const mapDispatchToProps = function (dispatch) {
    return bindActionCreators({
        toggleFeature: runtimeOperations.toggleFeature,
        setSelectedFeatures: runtimeOperations.setSelectedFeatures,
        setRuntimeFeature: runtimeOperations.setRuntimeFeature,
        setQuickSelOption: runtimeOperations.setQuickSelOption,

    }, dispatch);
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Features));
