/***************************************************************************
 *                                                                         *
 *                       Copyright (c) 2018, Nokia                         *
 *                                                                         *
 *                         All Rights Reserved                             *
 *                                                                         *
 *         This is unpublished proprietary source code of Nokia.           *
 *        The copyright notice above does not evidence any actual          *
 *              or intended publication of such source code.               *
 *                                                                         *
 ***************************************************************************/
import React, { PureComponent } from 'react';
import Tooltip from '@nokia-csf-uxr/csfWidgets/Tooltip';
import UniqueId from 'lodash/uniqueId';
import PropTypes from 'prop-types';


class LazyRenderer extends PureComponent {
    componentWillMount() {
        this.id = UniqueId("lazystatus-");
    }

    state = {
        tooltip: false
    }

    onMouseEnter = () => {
        this.setState({
            tooltip: true
        });
    }

    renderToolTip() {
        return (
            <Tooltip
                text={this.tooltip}
                target={`#${this.id}-${this.props.rowIndex}`}
                id={`tooltip${this.id}-${this.props.rowIndex}`}
                balloon
            />);
    }

    render() {
        console.log('<LazyRenderer props/>', this.props);
        const { lazy, timeout } = this.props.value;
        const sLazy = (lazy === true)?`Yes / ${timeout} s`:'No';
        this.tooltip = (lazy === true)?'Function Loading Strategy: Lazy':'';

        return (
            <div id={`${this.id}-${this.props.rowIndex}`} /* onMouseEnter={this.onMouseEnter} */ >
                <div>{sLazy}</div>
                { /* this.state.tooltip ? this.renderToolTip() : null */ }
            </div>
        )

    }

}

LazyRenderer.propTypes = {
    rowIndex: PropTypes.number.isRequired,
    value : PropTypes.shape({
        lazy : PropTypes.bool.isRequired,
        timeout : PropTypes.string.isRequired
    }).isRequired
};
export default LazyRenderer;