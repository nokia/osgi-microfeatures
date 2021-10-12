/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React, { Component } from "react";
import PropTypes from "prop-types";

class ImportContentFromLocalFile extends Component {
  constructor(props) {
    super(props);
    this.fileInput = null;
    this.fileReader = null;
    this.filename = null;
  }

  onClick = () => {
      const elem = this.fileInput;
      elem.value=''
      elem.click();
  }

  handleFileRead = e => {
//      console.log("handleFileRead e",e);
    const content = this.fileReader.result;
//    console.log(content);
    this.props.onLoad({ id: this.props.id, filename: this.filename, content:content });
  };

  handleFileChosen = file => {
//      console.log("handleFileChosen", file)
    this.filename = file.name;
    this.fileReader = new FileReader();
    this.fileReader.onloadend = this.handleFileRead;
    this.fileReader.readAsText(file);
  };

  render() {
      const { id } = this.props;
    return (
      <div>
        <input
          type="file"
          ref={elem => {
            this.fileInput = elem;
          }}
          id={`importinputfile${id}`}
          style={{ width: "0px", opacity: "0", position: "fixed" }}
          className="input-file"
          accept={Array.isArray(this.props.fileTypes) ? this.props.fileTypes.join(',') : this.props.fileTypes}
          onChange={e => this.handleFileChosen(e.target.files[0])}
        />
        <div onClick={this.onClick}>{this.props.children}</div>
      </div>
    );
  }
}

ImportContentFromLocalFile.propTypes = {
    id : PropTypes.string.isRequired,
    fileTypes: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.array
    ]),
    onLoad: PropTypes.func.isRequired,
    children: PropTypes.element.isRequired
  };

  ImportContentFromLocalFile.defaultProps = {
    fileTypes: [".txt",".json",".properties"]
  };

export default ImportContentFromLocalFile;
