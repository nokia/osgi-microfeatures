/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import React from 'react';
import PropTypes from 'prop-types';
import uniqueId from 'lodash/uniqueId';
import classNames from 'classnames';

import Label from '@nokia-csf-uxr/csfWidgets/Label';
import StatefulSearchwChips from '../commons/StatefulSearchwChips';
import $ from "jquery"

const DEFAULT_MIN_WIDTH = 150; // 150

/** LightTree displays only a container once mounted.
 * To be efficient, the tree is made only once the component is up, and is built directly with the real DOM.
 * ( not through the virtual dom used by React )
 */
export default class LightTree extends React.Component {
  constructor(props) {
    super(props);
    this.filterQueries = [];
  }

  static propTypes = {
    /** Unique id of component. For accessibility requirements, there can be no two elements with the same ID on a given page. */
    id: PropTypes.string,
    /** Width of the component. */
    width: PropTypes.number,
    /** Height of the component. */
    height: PropTypes.number,
    /** Minimum width of the component. */
    minWidth: PropTypes.number,
    /** Minimum height of the component. */
    minHeight: PropTypes.number,

    /** Data to be displayed in the Tree. Data is to be provided as an array of objects, as shown below, where each object represents a node.
      * - isContainer : true means node should be considered as a folder for the presentation,
      * - [data.label]: The name of the node or the item (leaf).
      * - [data.collapsed]: True if the node is to be collapsed, False if it is to be expanded.
      * - [data.children]: An array of node objects that will show under this node when it is expanded.
      * - [data.toolTipText]: Tool tip for item/node.
      */
    data: PropTypes.arrayOf(PropTypes.object).isRequired,

    /** Title of the tree. */
    title: PropTypes.string
  };

  static defaultProps = {
    id: undefined,
    width: null,
    height: null,
    minWidth: null,
    minHeight: null,
    title: undefined,
  };

  componentDidMount() {
    console.log("componentDidMount Building dom...");
    this.buildTreeDom(this.container);
  }

  render() {
    console.log("<LightTree props", this.props);
    const csfTreeStyles = {
      width: this.props.width ? `${this.props.width}px` : '100%',
      height: this.props.height ? `${this.props.height}px` : '100%',
      minWidth: this.props.minWidth ? `${this.props.minWidth}px` : `${DEFAULT_MIN_WIDTH}px`,
      minHeight: this.props.minHeight ? `${this.props.minHeight}px` : 'auto'
    };
    return (
      <div className="csfWidgets" style={csfTreeStyles}>
        {
          this.props.title &&
          <div style={{position:'relative'}} >
            <Label text={this.props.title} htmlFor={this.props.id} />
            <div className={'filterresult'} ref={ (c) => {this.filterresult = c;}} />
            <StatefulSearchwChips
              id={"searchInBundleTree"}
              placeHolder={"Search.."}
              onUpdate={this.onUpdate}
            />
          </div>
        }
        <div
          id={this.props.id}
          className={classNames('xsf-tree node-container', { 'has-title': this.props.title })}
          ref={(c) => { this.container = c; }}
        />
      </div>
    )
  }

  //
  // Real DOM
  //

  buildTreeDom = (container) => {
    // First of all clear the container and add the light tree container
    $(container).empty();
    // Create a container for all elements
    const $lTc = $('<div>').addClass('lighttree').appendTo(container);
    // Loop on each element to build and display
    if (this.props.data.length === 0) {
      $('<div>').text(this.props.message).appendTo($lTc);
      return;
    }
    const self = this;
    const $ul = $('<ul>').appendTo($lTc);
    $.each(this.props.data, function () {
      self.appendElement($ul, this);
    });
    const nbNodes = $('ul',$lTc).length;
    const nbLeafs = $('li',$lTc).length;
    console.log("nbNodes",nbNodes,nbLeafs);
  }

  appendElement = ($parent, element) => {
    const isLeaf = ( element.isContainer === undefined || (!element.children || element.children.length === 0 ) );
    if( isLeaf ) {
      const $li = $('<li>').appendTo($parent);
      $('<span>').addClass('lighttree_label').text(element.label).appendTo($li);
      return;
    }

    // element has sub-elements
    const id = uniqueId('ltc');
    const $li = $('<li>').appendTo($parent);

    $('<input>', {
      type: 'checkbox',
      id: id,
      checked: (element.collapsed === false)
    }).appendTo($li);

    $('<label>', {
      for: id,
      class: 'lighttree_label',
      text: element.label
    }).appendTo($li);

    const self = this;
    const $ulc = $('<ul>').appendTo($li);
    $.each(element.children, function () {
      self.appendElement($ulc, this);
    });
  }

  /**
   * SearchwChips callback: Trigged when the user enter a new keyword in the search input
   */
  onUpdate = (data) => {
    this.filterQueries = data;
    // Update directly the dom to reflect the new filtering
    this.applyFilterOnDomElement();
  }

  applyFilterOnDomElement = () => {
    const self = this;

    // Check the presence of the light tree
    const $tree = $('.lighttree', this.container);
    if ($tree.length === 0)
      return;

    // 
    // Reinitialize the tree before a new search
    //

    //  close all open folders before applying a new search
    $(":checked", $tree).prop("checked", false);
    // Remove hidden folders applied on a previous search
    $(".hidefolder", $tree).removeClass("hidefolder");
    // Remove previous marker on found text
    $('.lighttree_label.found', $tree).removeClass('found');
    // Clear number of found occurrences
    $(this.filterresult).empty();

    //
    // Should we apply a filter?
    //
    if( this.filterQueries.length === 0 )
      return;

    //
    // Apply filtering
    //

    // Search all labels which contain query strings
    const $found = $('.lighttree_label', $tree).filter(
      function (index) {
        return self.doesFilterPassOnData($(this).text());
      }
    )
//    console.log("FOUNDS", $found.length);

    // Hide the whole tree when not text matches.
    if($found.length === 0 ) {
      $tree.children().first().addClass('hidefolder');
      $(this.filterresult).text('No occurrence found');
      return;
    }

    $(this.filterresult).text(`Occurrence found : ${$found.length}`);

    // add marker on each text
    $found.addClass('found');

    // Open all folder concerning by the filtering
    $found.parents("li").find(':checkbox:first').prop("checked", true);

    // Hide all other closed folder
    $(":checkbox:not(:checked)", $tree).parent().addClass('hidefolder');

    //
    // Fold the first level of open folders
    //
    $('.lighttree > ul > li').not($('.hidefolder')).children(':checkbox').prop("checked", false);
  }

  /**
     * Applies filtering on text
     */
  doesFilterPassOnData = (data) => {
    for (let iterm = 0; iterm < this.filterQueries.length; iterm += 1) {
      const term = this.filterQueries[iterm].queryTerm;
      if (data) {
        const datalc = data.toLowerCase();
        if (datalc.indexOf(term.toLowerCase()) > -1) {
          return true;
        }
      }
      
    }
    return false
  }


  /*
  render_dom() {
    const csfTreeStyles = {
      width: this.props.width ? `${this.props.width}px` : '100%',
      height: this.props.height ? `${this.props.height}px` : '100%',
      minWidth: this.props.minWidth ? `${this.props.minWidth}px` : `${DEFAULT_MIN_WIDTH}px`,
      minHeight: this.props.minHeight ? `${this.props.minHeight}px` : 'auto'
    };
    return (
      <div className="csfWidgets" style={csfTreeStyles}>
        {
          this.props.title &&
          <div>
            <Label text={this.props.title} htmlFor={this.props.id} />
          </div>
        }
        <div
          id={this.props.id}
          ref={(c) => { this.container = c; }}
          onScroll={this.onScroll}
        >
          <div class="lighttree" >
            <ul>
              <li>
                <input type="checkbox" id="2c1" />
                <label class="lighttree_label" for="2c1">Tree2 Level 0</label>
                <ul>
                  <li>
                    <input type="checkbox" id="2c2" />
                    <label for="2c2" class="lighttree_label">Tree2 Level 1</label>
                    <ul>
                      <li><span class="lighttree_label">Tree2 Level 2</span></li>
                      <li><span class="lighttree_label">Tree2 Level 2</span></li>
                    </ul>
                  </li>
                </ul>
              </li>
            </ul>
            <ul>
              <li>
                <input type="checkbox" id="20c1" />
                <label class="lighttree_label" for="20c1">Tree2 Level 0</label>
                <ul>
                  <li>
                    <input type="checkbox" id="20c2" />
                    <label for="20c2" class="lighttree_label">Tree2 Level 1</label>
                    <ul>
                      <li>
                        <input type="checkbox" id="20c3" />
                        <label for="20c3" class="lighttree_label">Tree2 Level 2</label>
                        <ul>
                          <li><span class="lighttree_label">Tree2 Level 3</span></li>
                          <li><span class="lighttree_label">Tree2 Level 3</span></li>
                        </ul>
                      </li>
                    </ul>
                    <ul>
                      <li>
                        <input type="checkbox" id="21c3" />
                        <label for="21c3" class="lighttree_label">Tree2 Level 2</label>
                        <ul>
                          <li><span class="lighttree_label">Tree2 Level 3</span></li>
                          <li><span class="lighttree_label">Tree2 Level 3</span></li>
                        </ul>
                      </li>
                    </ul>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </div>
    );
  }
  */
}
