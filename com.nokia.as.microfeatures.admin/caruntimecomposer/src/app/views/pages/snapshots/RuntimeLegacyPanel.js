/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */


import React, { Component } from 'react';

import Button from '@nokia-csf-uxr/csfWidgets/Button';
import TextInput from '@nokia-csf-uxr/csfWidgets/TextInput';
import RcOverlayPanel from '../../commons/RcoverlayPanel'
import { merge } from 'lodash'
import $ from "jquery"

export default class RuntimeLegacyPanel extends Component {
    constructor(props) {
        super(props);
        this.initialState = {
            disablePanelBtn: true,
            // Platform properties
            platform: "",
            hitplatform: false,
            isValidplatform: true,
            // Group properties
            group: "",
            hitgroup: false,
            isValidgroup: true,
            // Component properties
            component: "",
            hitcomponent: false,
            isValidcomponent: true,
            // Instance properties
            instance: "",
            hitinstance: false,
            isValidinstance: true
        }

        // preset status on preset operation
        this.presetFieldStatus = {
            hitplatform: true,
            isValidplatform: true,
            hitgroup: true,
            isValidgroup: true,
            hitcomponent: true,
            isValidcomponent: true,
            hitinstance: true,
            isValidinstance: true
        }
        this.fieldsDesc = []
        this.fieldsDesc['platform'] = { isMandatory: true, regexp: /^\w+$/ }
        this.fieldsDesc['group'] = { isMandatory: true, regexp: /^\w+$/ }
        this.fieldsDesc['component'] = { isMandatory: true, regexp: /^\w+$/ }
        this.fieldsDesc['instance'] = { isMandatory: true, regexp: /^\w+$/ }

        this.comprefs = []; // Hold references on components

        this.state = merge({}, this.initialState)
    }

    componentWillReceiveProps(nextProps) {
        console.log("componentWillReceiveProps----------------", nextProps)
/*
        let newState = Object.assign({}, this.initialState, nextProps.form, { disablePanelBtn: (nextProps.form.name === '') })
        // When the state will be preset by pertinent value, we should preset status field too
        if( nextProps.form.name !== '' ) {
            newState = Object.assign(newState,this.presetFieldStatus)
        }
        console.log("newState", newState)
        this.setState(newState)
*/
    }

    componentDidMount() {
        console.log('this.comprefs[overlay]',this.comprefs['overlay'])
        const toggleButton = $('#overlay-createlegacyruntime-openPanel-button')
        console.log('toggleButton',toggleButton.length)
        toggleButton.click()
    }

    isValidFields = (field, isValid) => {
        if (isValid === false)
            return false;
        // Check the validity of all others fields
        const fieldnames = ['platform', 'group', 'component', 'instance'].filter(word => word !== field);
        let property;
        for (let i = 0; i < fieldnames.length; i++) {
            property = 'isValid' + fieldnames[i];
            if (this.state[property] !== true || this.state['hit' + fieldnames[i]] !== true)
                return false;
        }
        return true;
    }

    isValidField = (name, value) => {
        if (this.fieldsDesc[name].isMandatory === false && value.length === 0)
            return true;
        return this.fieldsDesc[name].regexp.test(value);
    }

    handleOnChange = (e) => {
        const name = e.nativeEvent.target.name;
        const value = e.value;
        let o = {}
        o[name] = value;
        const isValidField = this.isValidField(name, value);
        o['isValid' + name] = isValidField;
        o['hit' + name] = true;
        o['disablePanelBtn'] = !this.isValidFields(name, isValidField);
        //        console.log("onchange!", e, o)
        this.setState(o);
    }

    handlePanelAction = (json) => {
        console.log("##### onPanelAction ######", json)
        const isClosed = (json.state === "closed")
        this.setState({ disablePanelBtn : true })
        if( isClosed )
            this.props.onCancel();
    }

    createLegacyRuntime = (e) => {
        console.log('createLegacyRuntime');
        const pgci = {
            p: this.state.platform,
            g: this.state.group,
            c: this.state.component,
            i: this.state.instance
        };
        this.setState({ disablePanelBtn: true })
        this.comprefs['overlay'].closePanel();
        this.props.onCreate(pgci);
    }

    renderContent = () => {
        return (
            <div>
                <TextInput id={'slrplatform'}
                    name={'platform'}
                    placeholder={'Platform'}
                    text={this.state.platform}
                    errorMsg={'Input format: Platform name without space'}
                    spellcheck={false}
                    focus
                    label={'Platform'}
                    required={this.fieldsDesc['platform'].isMandatory}
                    error={!this.state.isValidplatform }
                    onChange={(e) => this.handleOnChange(e)}
                />
                <TextInput id={'slrgroup'}
                    name={'group'}
                    placeholder={'Group'}
                    text={this.state.group}
                    errorMsg={'Input format: Group name without space'}
                    spellcheck={false}
                    label={'Group'}
                    required={this.fieldsDesc['group'].isMandatory}
                    error={!this.state.isValidgroup }
                    onChange={(e) => this.handleOnChange(e)}
                />
                <TextInput id={'slrcomponent'}
                    name={'component'}
                    placeholder={'Component'}
                    text={this.state.component}
                    errorMsg={'Input format: Component name without space'}
                    spellcheck={false}
                    label={'Component'}
                    required={this.fieldsDesc['component'].isMandatory}
                    error={!this.state.isValidcomponent }
                    onChange={(e) => this.handleOnChange(e)}
                />
                <TextInput id={'crasmbinstance'}
                    name={'instance'}
                    placeholder={'Instance'}
                    text={this.state.instance}
                    errorMsg={'Input format: Instance name without space'}
                    spellcheck={false}
                    label={'Instance'}
                    required={this.fieldsDesc['instance'].isMandatory }
                    error={!this.state.isValidinstance }
                    onChange={(e) => this.handleOnChange(e)}
                />
            </div>
        )
    }

    /**
     * This function is not a callback function.
     * It will be executed by the RcOverlayPanel ( child of our component)in its own context,
     * and allows to overrides the original implementation of its toggle button
     */
    getToogleButton = (context) => (
        <Button
            ref={
                (toggleButton) => {
                    context.toggleButton = toggleButton
                }
            }
            text="CREATE LEGACY RUNTIME"
            isCallToAction
            disabled={context.props.disabled}

            id={context.props.id + "-openPanel"}
            onClick={context.onOpen}
            data-open-panel={context.state.buttonState === 'opened-panel'}
            iconColor={context.state.buttonState === 'opened-panel' ? 'rgba(18,65,145,1)' : 'rgba(0,0,1,.54)'}
        />
    )


    render() {
        console.log('<RuntimeLegacyPanel /> props', this.props)
        return (
            <RcOverlayPanel ref={(c) => this.comprefs['overlay'] = c}
                id={'overlay-createlegacyruntime'}
                title={'Create Legacy Runtime'}
//                disabled={this.props.disabled}
                content={this.renderContent()}
                buttonType={'action'}
                buttonText={'CREATE'}
                getToogleButton={this.getToogleButton}
                disablePanelBtn={this.state.disablePanelBtn}
                onClick={(e) => this.createLegacyRuntime(e)}
                onPanelAction={(o) => this.handlePanelAction(o)}
            //                onPanelOpenClose={()=> this.onPanelOpenClose()}
            />
        )


    }
}