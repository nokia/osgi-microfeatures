/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from 'react';

import ToolBar from '../../commons/ToolBar';

class Helps extends Component {
  constructor(props) {
    super(props)
    this.toolbarh = 69;
  }

  getTabContentHeight = () => this.props.height - (this.toolbarh);

  render() {
    console.log("<Helps /> props ", this.props)
    return (
      <div style={{ height: this.props.height, position: 'relative'}} >
        <ToolBar pageTitle={'Help'} />
        <div className={'csfWidgets'} style={{ height: this.getTabContentHeight(), position: 'relative',overflow: 'auto', paddingLeft: '10px' }} >
          <h1>Dashboard view:</h1>
          This view displays quick information splitted in several cards.
          <h2>Current Obr</h2>
          This field displays the obr url being in use.
          To work with an other obr, click on the field to open a list of available obrs and select one.
          <h2>Current status</h2>
          Displays a summary of entities, such as: 
            <ul>
              <li>Features: Displays the number of available features present in the current obr,</li>
              <li>Assembly: Displays the number of created assemblies stored in a local obr,</li>
              <li>Snapshot: Displays the number of created snapshots stored in a local obr.</li>
            </ul>
          <h2>Features referenced by assemblies</h2>
          A pie displays the number of features referenced by the whole of created assemblies.
          When assemblies used features which are not present in the current obr, a red slice is then displayed.
          Typically, this case can occurs when any assembly has been created with an Obr, while another is being in use. 
          <h1>Features view:</h1>
          This is the view to create any assembly.
          The 'CREATE ASSEMBLY' button becomes enable once a runtime is chosen from the 'Start from' selector and a feature in selected at least.
          <div>
            <ul>
              <li><b>Quick selector :</b> This allows to select / unselect all features or apply a selection depending of an existing assembly.
              <br/>In this last case, after clicking on the 'CREATE ASSEMBLY' button, the form fields is automatically filled,
              </li>
              <li><b>Start from :</b> This allows to select a runtime that will be appended to the assembly,</li>
              <li><b>Feature documentation:</b> When a documentation is available, a dedicated icon is then present on the right side of a feature item.
                Press on the icon should open the documentation in a new browser tab.</li>
            </ul>
          </div>
          <h1>Assemblies view:</h1>
          This is the view to create any snapshot from an assembly. From the list, an assembly can be deleted too.<br/>
          These operations can be done from dedicated icons located on the right side of an feature item.

          <h1>Snapshots:</h1>
          This is the view to download any runtime from an assembly. From the list, an snapshot can be deleted too.<br/>
          These operations can be done from dedicated icons located on the right side of an snapshot item.
          <p>
            To download a legacy runtime, select the 'runtime layout structure' choice as 'Legacy', and then press on the download icon.
            This open a form where fields must be filled.
          </p>

          <h1>Browse Obrs view:</h1>
          This is the view to browse local and current obrs. Each obr is displayed in a separate tab.
          Each tab displays a list of bundle.
          <p>
          To resolve a bundle or find other bundles that depend on it, uses the dedicated icons.
          </p>
          <h1>Preferences view:</h1>
          The view where the user can set some preferences, such as theme color and the mode to open/close the left menu.
          <p></p>
        </div>
      </div>
    );
  }
}
export default Helps