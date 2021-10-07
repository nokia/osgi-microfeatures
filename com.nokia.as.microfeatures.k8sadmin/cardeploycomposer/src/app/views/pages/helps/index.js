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
      <div style={{ height: this.props.height, position: 'relative', overflow: 'hidden' }} >
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
              <li>Features: Displays the number of available features present in the current obr</li>
              <li>Runtimes: Displays the number of deployed runtime</li>
            </ul>
          <h1>Features view:</h1>
          This is the view to deploy any runtime.
          The 'DEPLOY' button becomes enable once a feature typed 'runtime' is chosen from the 'Start from' selector and a feature in selected at least.

          <h1>Runtimes view:</h1>
          This is the view to undeploy any runtime, and allows to open a gogo shell to a runtime.

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