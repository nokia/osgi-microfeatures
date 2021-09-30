/***************************************************************************
 *                                                                         *
 *                       Copyright (c) 2017, Nokia                         *
 *                                                                         *
 *                         All Rights Reserved                             *
 *                                                                         *
 *         This is unpublished proprietary source code of Nokia.           *
 *        The copyright notice above does not evidence any actual          *
 *              or intended publication of such source code.               *
 *                                                                         *
 ***************************************************************************/
import React, { Component } from 'react';
import { Link } from 'react-router-dom';

const styles = {
    container: {
        display: 'flex',
        flexDirection: 'column',
        flex: '1',
        justifyContent: 'center'
    },
    titleStyle: {
        marginTop: '0',
        textAlign: 'center',
        fontSize: '200px',
        fontWeight: 'bold',
        lineHeight: '100%'
    },
    descriptionStyle: {
        textAlign: 'center',
        fontSize: '32px',
        fontWeight: 'bold',
        margin: '0 auto'
    },
    linkStyle: {
        margin: '20px 0',
        alignSelf: 'center'
    }
};

/**
 * Not Found page
 */
export default class NotFound extends Component {

    //  #region Lifecycle

    render () {
        return (
            <div style={styles.container}>
                <h1 style={styles.titleStyle}>404</h1>
                <div style={styles.descriptionStyle}>{'Oops! We couldn\'t find this page...'}</div>
                <Link to="/" style={styles.linkStyle} className="btn btn-lg btn-info" role="button">{'Back to home'}</Link>
            </div>
        );
    }

    //  #endregion
}