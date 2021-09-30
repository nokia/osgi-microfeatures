import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { entitiesSelectors } from '../../../state/ducks/entities'
import { snapshotSelectors, snapshotOperations } from '../../../state/ducks/snapshot'
import { runtimeTypes, runtimeSelectors, runtimeOperations } from '../../../state/ducks/runtime'
import AlertDialogConfirm from '@nokia-csf-uxr/csfWidgets/AlertDialogConfirm';
import RadioButtonGroup from '@nokia-csf-uxr/csfWidgets/RadioButton/RadioButtonGroup';
import RadioButton from '@nokia-csf-uxr/csfWidgets/RadioButton';

import ToolBar from '../../commons/ToolBar';
import RcStatusIndicator from '../../commons/RcStatusIndicator';
import Download from './Download'
import RuntimeLegacyPanel from './RuntimeLegacyPanel'

import SnapshotsList from './SnapshotsList'

class Snapshots extends Component {
    constructor(props) {
        super(props)
        this.toolbarh = 69;
    }

    getListHeight = () => this.props.height - (this.toolbarh + 70 + 60);

    onLayoutChange = (event) => {
        console.log('onLayoutChange', event.value)
        this.props.setLayout(event.value)
    }

    onCreateRuntime = (payload) => {
        if (this.props.runtimeLayout === runtimeTypes.LAYOUT_TYPES.LEGACY) {
            console.log("Create Legacy Runtime")
            this.props.openLegacyPanel(payload)
            return;
        }
        // Create Runtime
        this.props.createRuntime(payload)
    }

    render() {
        console.log("<Snapshots /> props ", this.props)
        const { CSF, LEGACY } = runtimeTypes.LAYOUT_TYPES
        const snapshot = this.props.deletePayload;
        const { name, bsn, version } = snapshot;
        const deleteInfo1 = "'" + name + "' v:" + version
        const deleteInfo2 = 'Bsn : ' + bsn
        const isDeleting = this.props.deleteSnapshotStatus === 'started'
        return (
            <div style={{ height: this.props.height, position: 'relative' }} >
                <ToolBar pageTitle={'Snapshots'} />
                {this.props.isOpenLegacyPanel && <div className="overlayRuntime"></div>}
                { this.props.createStatus === 'started' &&
                    <Download
                        payload={this.props.createPayload}
                        obrs={this.props.obrs}
                        onSuccess={this.props.createRuntimeSuccess}
                        onError={this.props.createRuntimeFailure}
                    />
                }
                <RcStatusIndicator
                    id={'runtimeIndicator'}
                    clearText={'RUNTIME SUCCESSFULLY CREATED'}
                    requestStatus={this.props.createStatus}
                />
                { isDeleting === true &&
                    <div id='delsnapoverlay' className={'csfWidgets overlay active black'} />
                }
                {this.props.openConfirmDelete &&
                    <AlertDialogConfirm
                        title={'Do you want to delete this snapshot?'}
                        confirmationText1={deleteInfo1}
                        confirmationText2={deleteInfo2}
                        confirmationButtonLabel={'DELETE'}
                        onClose={this.props.cancelDeleteSnapshot}
                        onConfirm={this.props.confirmDeleteSnapshot}
                    />
                }
                <div id="snapshots-row1">
                    <div>
                        <RadioButtonGroup
                            id={'runtimeLayout'}
                            name="runtimeLayout"
                            label="Runtime Layout structure"
                            selectedItem={this.props.runtimeLayout}
                            onChange={this.onLayoutChange}
                        >                        
                            <RadioButton
                                id={'CSF'}
                                label="CSF"
                                value={CSF}
                            />
                            <RadioButton
                                id={'LEGACY'}
                                label="Legacy"
                                value={LEGACY}
                            />                           
                        </RadioButtonGroup>
                    </div>
                    {this.props.isOpenLegacyPanel &&
                        <div style={{ verticalAlign: 'top' }}>
                            <RuntimeLegacyPanel
                                onCreate={this.props.createLegacyRuntime}
                                onCancel={this.props.cancelLegacyRuntime}
                            />
                        </div>
                    }
                </div>

                <div id="snapshots-rows" style={{ height: this.getListHeight(), position: 'relative' }} >
                    <SnapshotsList
                        snapshotsListData={this.props.snapshotsListData}
                        deleteSnapshot={this.props.deleteSnapshot}
                        confirmDelete={this.props.confirmDeleteSnapshot}
                        cancelDelete={this.props.cancelDeleteSnapshot}
                        createRuntime={this.onCreateRuntime}
                        width={this.props.width}
                    />
                </div>
            </div>
        );
    }
}

//
// CONNECT TO THE STATE AS ANY CONTAINER
//
const mapStateToProps = function (state) {
    return {
        snapshotsListData: entitiesSelectors.getAllSnapshots(state),
        createStatus: runtimeSelectors.getCreateStatus(state),
        createPayload: runtimeSelectors.getPayload(state),
        runtimeLayout: runtimeSelectors.getLayout(state),
        obrs: entitiesSelectors.getCurrentObrs(state),
        openConfirmDelete: snapshotSelectors.shouldConfirmDelete(state),
        deletePayload: snapshotSelectors.getPayload(state),
        isOpenLegacyPanel: runtimeSelectors.isOpenLegacyPanel(state),
        deleteSnapshotStatus: snapshotSelectors.getDeleteStatus(state)
    }
}

const mapDispatchToProps = function (dispatch) {
    return bindActionCreators({
        ...snapshotOperations,
        ...runtimeOperations
    }, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Snapshots);