/**
 * Copyright 2000-2021 Nokia
 *
 * Licensed under the Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/**
 * COMPONENT PATCHED ( CLIWindow UX release npm 17.40.2 )
 * On unmount, the Terminal is not disposed and the connection still exist
 * This patched component solves this issue
 */

import React from "react";
import PropTypes from "prop-types";
import { Terminal } from "xterm";
import * as attach from "xterm/lib/addons/attach/attach";
import * as fit from "xterm/lib/addons/fit/fit";
import classNames from "classnames";
import Button from "@nokia-csf-uxr/csfWidgets/Button";

/** CLIWindow provides a command line interface that can be used to control remote devices or servers. */
export default class CLIWindow extends React.Component {
  state = {
    fullscreen: this.props.fullscreen,
    exitAnimation: null
  };

  componentWillReceiveProps(props) {
    if (props.initialData) {
      // if the intialData has been updated then send it to the server to process that
      this.sendToSocket(props.initialData);
    }
  }

  componentWillUnmount() {
    /* BEGIN PATCH */
    //    this.onClose();
    if (!this.isTerminalDisposed) {
      this.terminal.dispose();
      this.props.closeBackendString &&
        this.sendToSocket(this.props.closeBackendString);
    }
    /* END PATCH */

    window.removeEventListener("resize", this.resizeFrontend);
  }

  /** Callbacks */
  onOpen = e => {
    this.props.onOpen && this.props.onOpen(e);
  };

  onClose = () => {
    // remove the terminal after the exit animation completes
    this.setState({ exitAnimation: true });
  };

  onConnectionOpen = e => {
    this.props.onConnectionOpen && this.props.onConnectionOpen(e);
    if (this.props.initialData) {
      this.sendToSocket(this.props.initialData);
    }
  };

  onConnectionClose = e => {
    this.props.onConnectionClose && this.props.onConnectionClose(e);
  };

  onError = e => {
    this.props.onError && this.props.onError(e);
  };

  onSend = e => {
    this.props.onSend && this.props.onSend(e);
  };

  onReceive = e => {
    this.props.onReceive && this.props.onReceive(e);
  };

  initialize = () => {
    // this function is called by this.animationHandler() after entry animation ends
    this.terminal = new Terminal({
      cursorBlink: this.props.cursorBlink,
      theme: { background: "#222" },
      fontFamily:
        "Courier New,Courier,Lucida Sans Typewriter,Lucida Typewriter,monospace",
      fontSize: 14
    });

    Terminal.applyAddon(attach); // Load the `attach` addon (helps attach sockets)
    Terminal.applyAddon(fit); // Load resizing addon

    this.terminal.open(this.terminalContainer); // open the terminal (xtermjs)
    this.onOpen();
    this.terminal.focus();

    window.addEventListener("resize", this.resizeFrontend); // resize to fit container

    this.terminal.prompt = () => {
      // initialize the prompt
      this.terminal.write("\r\n$ ");
    };

    if (!("WebSocket" in window)) {
      // check if websockets is even supported
      throw new Error(this.props.textString.websocketUnsupported);
    }

    if (!this.props.socket) {
      // check if there is a valid socket connection
      this.terminal.writeln(
        `\u001b[31m${this.props.textString.invalidSocketConn}\u001b[39m`
      );
      return;
    }

    this.websocket = new WebSocket(this.props.socket); // connect to the socket

    this.websocket.onopen = e => {
      // when websocket connection has been established
      this.terminal.attach(this.websocket, true, true);
      this.onConnectionOpen(e);
      this.resizeBackend();
    };

    this.websocket.onclose = e => {
      if (e.code === 1006) {
        // any abnormal connection/disconnection
        this.terminal.writeln(
          `\u001b[31m${this.props.textString.abnormalConnError}\u001b[39m`
        );
      } else if (e.code === 1000) {
        // server closed the connection normally
        this.terminal.writeln(
          `\u001b[31m${this.props.textString.normalClose}\u001b[39m`
        );
      } else {
        this.terminal.writeln(
          `\u001b[31m${this.props.textString.genericError}  ${
            e.code
          }.\u001b[39m`
        );
      }
      this.onConnectionClose(e);
    };

    this.websocket.onerror = e => {
      this.onError(e);
    };

    this.websocket.onmessage = e => {
      // terminal recieves a message from socket
      this.onReceive(e); // callback
      this.resizeFrontend(); // resize again to get the scrollbar
    };

    this.terminal.on("data", d => {
      // terminal sends a message out to socket
      this.onSend(d);
    });

    this.terminal.on("resize", () => {
      // resize backend terminal (only triggered if resizeBackend prop is set to true)
      this.resizeBackend();
    });
  };

  /** Main Functions */
  animationHandler = e => {
    // this function is called by onAnimationEnd (synthetic event attached to this component)
    if (
      e.animationName === "csfWidgets-cli-entry" ||
      e.animationName === "csfWidgets-cli-entry-fullscreen"
    ) {
      // start the terminal only after the initial entry animation, as it effects xtermjs terminal size  otherwise
      if (!this.terminal) {
        this.initialize();
      }
      // now that animation ended, resize and scroll to bottom
      this.resizeFrontend();
      this.terminal.scrollToBottom();
    }

    if (e.animationName === "csfWidgets-cli-exit") {
      // after animation completes then start onClose callbacks
      this.terminal.dispose();
      this.props.onClose && this.props.onClose();
      // on close, send this string to backend
      this.props.closeBackendString &&
        this.sendToSocket(this.props.closeBackendString);

      // PATCH TO SOLVE THE ISSUE ON UNMOUNT
      this.isTerminalDisposed = true;

    }
  };

  updatePosition = () => {
    this.setState(
      prevState => ({ fullscreen: !prevState.fullscreen }),
      () => {
        this.resizeFrontend();
      }
    );
  };

  sendToSocket = data => {
    // send to the socket
    // PATCH ( Try /catch useful when using -Dstandalone=true is used )
    try {
      this.websocket && this.websocket.send(data);
    } catch(e) {
      console.error(e);
    }
  };

  resizeFrontend = () => {
    // resize the front end terminal (xterm) using 'fit' addon
    this.terminal.fit();
  };

  resizeBackend = () => {
    // send the updated cols, rows to backend server to resize the terminal to fit
    if (this.props.resizeBackend && this.websocket.readyState === 1) {
      // check if socket is fully connected first
      this.sendToSocket(
        `${this.props.resizeBackendString},${this.terminal.cols},${
          this.terminal.rows
        }`
      );
    }
  };

  render() {
    const classes = classNames({
      csfWidgets: true,
      cli: true,
      fullscreen: this.state.fullscreen,
      docked: !this.state.fullscreen,
      "exit-animation": this.state.exitAnimation
    });

    return (
      <div
        className={classes}
        onAnimationEnd={this.animationHandler}
        data-test="ccfk-cliwindow"
      >
        <div className="header">
          <span className="title">{this.props.title}</span>
          <div className="window-options">
            <Button
              id="fullScreen"
              icon={
                this.state.fullscreen ? "ic_full_screen_exit" : "ic_full_screen"
              }
              onClick={this.updatePosition}
            />
            <Button id="close" icon="ic_close" onClick={this.onClose} />
          </div>
        </div>

        <div
          className="blackbox"
          ref={elm => {
            this.terminalContainer = elm;
          }}
        />
      </div>
    );
  }
}

CLIWindow.propTypes = {
  /** Title of the CLI window */
  title: PropTypes.string,
  /** WebSocket url to connect to (ws:// or wss:// for secure connections) eg. ws://localhost:8080 */
  socket: PropTypes.string.isRequired,
  /** String to be displayed in status area */
  // status: PropTypes.string,
  /** Docked or Fullscreen */
  fullscreen: PropTypes.bool,
  /** If enabled, a string will be sent to the WebSocket to provide the backend the frontend terminal size (column and row count)
   * so that the terminal in the backend can resize accordingly. The default size of the terminal is 80 cols / 24 rows.
   * The string sent is in this format: "backendstring,col,rows" for example: "NokiaCLI:resize,80,24". You may customize the start of the string with the
   * resizeBackendString prop. In your backend, you must listen for this string, and if available, take the info it provides
   * to resize your terminal.
   * Enable this to get the most optimal display, especially when dealing with in terminal editors like vi/vim/nano.
   */
  resizeBackend: PropTypes.bool,
  /** Customize the start of the backend string to be sent (to assist with backend terminal resizing) */
  resizeBackendString: PropTypes.string,
  /** String to be sent to backend after the (front-end) terminal has been closed */
  closeBackendString: PropTypes.string,
  /** Cursor blinking in terminal */
  cursorBlink: PropTypes.bool,
  /** onOpen callback, triggered when the terminal is initialized.
   */
  onOpen: PropTypes.func,
  /** onClose callback, decide what you would like to do with your terminal on the close event. Triggered by the close button.
   * Note: the exist animation does not remove the terminal component from the DOM.
   */
  onClose: PropTypes.func,
  /** onConnectionOpen callback, triggered after the websocket connection has been established. */
  onConnectionOpen: PropTypes.func,
  /** onConnectionClose callback, triggered after the websocket connection has been closed/disconnected. */
  onConnectionClose: PropTypes.func,
  /** onError callback, triggered after the websocket has experienced an error. */
  onError: PropTypes.func,
  /** onSend callback, triggered after the terminal sends input to the server. */
  onSend: PropTypes.func,
  /** onReceive callback, triggered after the terminal receives output from the server. */
  onReceive: PropTypes.func,
  /** initialData, send string to the terminal on when the socket is open (onConnectionOpen), for example to send info needed for SSH connections.
   */
  initialData: PropTypes.string,
  /** Define your own text strings, for this component (internationalization) */
  textString: PropTypes.objectOf(PropTypes.string)
};

CLIWindow.defaultProps = {
  title: "Nokia Command Line Interface",
  fullscreen: false,
  cursorBlink: true,
  resizeBackend: false,
  resizeBackendString: "NokiaCLI:resize",
  onConnectionOpen: null,
  onConnectionClose: null,
  onError: null,
  onSend: null,
  onReceive: null,
  onOpen: null,
  onClose: null,
  initialData: null,
  closeBackendString: null,
  textString: {
    websocketUnsupported:
      "Websockets unsupported - please use a browser that supports Websockets",
    invalidSocketConn:
      "Warning: Not Connected! Please provide a valid websocket to connect to!",
    abnormalConnError:
      "Terminal connection terminated or unestablished. Check your client/server configuration.",
    normalClose: "Terminal connection was closed by server.",
    genericError: "Terminal connection was closed by server with code:"
  }
};
