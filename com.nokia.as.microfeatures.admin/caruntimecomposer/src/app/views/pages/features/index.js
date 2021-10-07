import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { createSelector } from 'reselect'

import { assemblySelectors, assemblyOperations } from '../../../state/ducks/assembly'
import { entitiesSelectors } from '../../../state/ducks/entities'

import ToolBar from '../../commons/ToolBar';
import StartFrom from './StartFrom'
import FeaturesList from './FeaturesList'
import CreateAsmb from './CreateAsmb'
import QuickSel from './QuickSel'
import RcStatusIndicator from '../../commons/RcStatusIndicator';


class Features extends Component {
    constructor(props) {
        super(props)
        this.toolbarh = 64;
        this.quickSelh = 67;
    }

    getListHeight = () => this.props.height - (this.toolbarh + this.quickSelh + 20);

    render() {
        console.log("<Features /> props ", this.props)
        return (
            <div style={{ height: this.props.height, position: 'relative'}} >
                <ToolBar pageTitle={'Features'} />
                { /* this.props.requestStatus === 'started' &&
                    <div id='createassemblyoverlay' className={'csfWidgets overlay active black'} />
             */   }
                {this.props.isPanelAssemblyOpen && <div className="overlayAsmb"></div>}
                <RcStatusIndicator
                    id={'assemblyIndicator'}
                    clearText={'ASSEMBLY SUCCESSFULLY CREATED'}
                    requestStatus={this.props.requestStatus}
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
                            setRuntime={this.props.setRuntime}
                            runtimeId={this.props.runtimeId}
                        />
                    </div>
                    <div style={{ verticalAlign: 'top' }}>
                        <CreateAsmb
                            disabled={!this.props.canCreateAssembly}
                            form={this.props.assemblyForm}
                            assemblyFeatures={this.props.assemblyFeatures}
                            createAssembly={this.props.createAssembly}
                            setAsmbPanelStatus={this.props.setAsmbPanelStatus}
                        />
                    </div>
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
    assemblySelectors.getRuntimeId,
    (allfeatures, runtimeId) => {
        if (runtimeId === null)
            return []
        return allfeatures.filter(f => (f.fid === runtimeId))
    }
)

// Returns a list of sorted features excepting whose category is 'runtime'
// with all unselected items
const featuresSelector = createSelector(
    entitiesSelectors.getAllFeatures,
    (allfeatures) => allfeatures.filter(f => f.categories[0] !== 'runtime')
)

// Returns a list of adapted features respecting the user selections
const getFeaturesList = createSelector(
    [assemblySelectors.getFeaturesIds, featuresSelector],
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

const getAllSelectedFeatures = createSelector(
    runtimeSelectedFeature,
    selectedFeatures,
    (runtime, selecteds) => runtime.concat(selecteds)
)

const getQuickSelData = createSelector(
    entitiesSelectors.getAssemblies,
    (assemblies) => {
        const data = []
        data.push({ id: 'NONE', label: 'None', value: 'NONE' })
        data.push({ id: 'ALL', label: 'All', value: 'ALL' })

        const comparator = (f1, f2) => {
            const a = f1.id.toUpperCase();
            const b = f2.id.toUpperCase();
            return (a > b) ? 1 : ((b > a) ? -1 : 0)
        }
        const assemblychoices = []
        for (let key in assemblies) {
            let asmb = assemblies[key]
            assemblychoices.push({ id: key, label: asmb.name + ' ' + asmb.version, value: key })
        }
        console.log("getQuickSelData has work!");
        return data.concat(assemblychoices.sort(comparator));
    }
)

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function (state) {
    return {
        assemblyForm: assemblySelectors.getForm(state),
        requestStatus: assemblySelectors.getRequestState(state),
        runtimeId: assemblySelectors.getRuntimeId(state),
        canCreateAssembly: assemblySelectors.canCreateAssembly(state),
        isPanelAssemblyOpen: assemblySelectors.isPanelAssemblyOpen(state),
        featuresListData: getFeaturesList(state),
        runtimeOptionsList: runtimeOptionsList(state),
        assemblyFeatures: getAllSelectedFeatures(state),
        quickSelOptions: getQuickSelData(state),
        selectedQuick: assemblySelectors.getSelectedQuick(state)
    }
}

const mapDispatchToProps = function (dispatch) {
    return bindActionCreators({
        toggleFeature: assemblyOperations.toggleFeature,
        setSelectedFeatures: assemblyOperations.setSelectedFeatures,
        createAssembly: assemblyOperations.createAssembly,
        setRuntime: assemblyOperations.setRuntime,
        setAsmbPanelStatus: assemblyOperations.setAsmbPanelStatus,
        setQuickSelOption: assemblyOperations.setQuickSelOption
    }, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Features);
