import React, { Component } from 'react';
import PropTypes from 'prop-types';

// third party libraries
import _ from 'lodash';

import GridLayout, { WidthProvider } from 'react-grid-layout';
//import Card from './Card';
import Card from '@nokia-csf-uxr/csfWidgets/Card'

/* Bring in the overridden methods from the 'react-grid'layout' library.
 * This is required for the desired behavior despite it not being accessed directly in this file. */
// import { moveElement, onDragStart, onDrag, onDragStop } from './ReactGridLayoutOverrides'; // eslint-disable-line
import { moveElement, onDragStart, onDrag, onDragStop } from '@nokia-csf-uxr/csfWidgets/Card/ReactGridLayoutOverrides'; // eslint-disable-line
// styles
//import './styles/CardCollection.styl';

const DynamicWidthReactGridLayout = WidthProvider(GridLayout);
const MIN_CARD_WIDTH = 272;

/** A group of Cards that can be used to arrange elements/ Cards and optionally can be reordered by the user. */
export default class CardCollection extends Component {
    static propTypes = {
      /**
       * Unique ID assigned to the component. For accessibility requirements, there can be
       * no two elements with the same ID on a given page.
       */
      id: PropTypes.string,
      /** A unique class name to override the default class name. */
      className: PropTypes.string,
      /** An array of objects defining the Card Collection grid; defaults to an empty array of objects. */
      layout: PropTypes.arrayOf(PropTypes.object),
      /** Sets whether the collection is center aligned (default) or aligned left. */
      alignCenter: PropTypes.bool,
      /** The height of the card collection in pixels. */
      height: PropTypes.number,
      /** The width of the card collection in pixels. */
      width: PropTypes.number,
      /** The number of columns in the card collection grid (x dimension in grid). */
      cols: PropTypes.number,
      /** The row height in pixels. */
      rowHeight: PropTypes.number,
      /** Whether Card item expands dynamically or not (default is false); set Card width to auto. */
      dynamicWidth: PropTypes.bool,
      /**
       * The margin between Cards.  Value is an array consisting of 2 numbers,
       * left/right margin, and top/bottom margin.
       * [ horizontal-margin, vertical-margin ]
       */
      margin: PropTypes.arrayOf(PropTypes.number),
      /**
       * The padding around the entire Card Collection. Value is an array
       * consisting of 2 numbers, left/right padding, and top/bottom padding.
       * [ horizontal-padding, vertical-padding ]
       */
      padding: PropTypes.arrayOf(PropTypes.number),
      /** For use with responsive/ dynamicWidth, layout will readjust to ensure that each card has a minimum width. */
      minCardWidth: PropTypes.number,
      /** The Cards to be displayed in the Card Collection. */
      children: PropTypes.arrayOf(PropTypes.object),
      /** Callback for when the layout changes. Returns object of shape ({type, value, data}):
       *   - type: 'onLayoutChange'
       *   - value: layout, where layout is an array of objects defining the Card Collection grid after dragging
       *   - data: this.props.eventData
      */
      onLayoutChange: PropTypes.func,
      /** Object that will be passed back with events. */
      eventData: PropTypes.objectOf(PropTypes.any),
    };

    static defaultProps = {
      id: undefined,
      className: 'cardCollection',
      layout: [],
      alignCenter: true,
      height: undefined,
      width: 800,
      cols: 8,
      rowHeight: 125,
      dynamicWidth: false,
      margin: [16, 13],
      padding: [24, 24],
      minCardWidth: MIN_CARD_WIDTH,
      children: [],
      onLayoutChange: undefined,
      eventData: null
    };

    constructor(props) {
      super(props);

      this.id = !props.id ? _.uniqueId('csfWidgets-card-collection-') : props.id;

      if (props.minCardWidth < MIN_CARD_WIDTH) {
        console.warn(`minCardWidth should be larger than or equal to ${MIN_CARD_WIDTH}`); // eslint-disable-line no-console
      }
    }

    // class props
    state = {
      layout: this.initLayout(this.props.layout, this.props.children),
      expandedCardId: ''
    };

    componentWillMount() {
      this.updateLayoutAsResponsive();
    }

    componentDidMount() {
      const parentAttributes = {
        padding: this.props.padding,
        margin: this.props.margin
      };

      // notify Card children who their parents are
      this.cards.map(cardObject => cardObject.card.setParentAttributes(parentAttributes));
      this.updateLayoutAsResponsive();
      window.addEventListener('resize', this.updateIfNeeded);
    }

    componentWillReceiveProps(newProps) {
      if (newProps.layout && !_.isEqual(newProps.layout, this.props.layout)) {
        this.setState({
          layout: this.initLayout(newProps.layout, newProps.children),
        });
      }
    }

    componentWillUpdate() {
      this.updateLayoutAsResponsive();
    }

    componentWillUnmount() {
      window.removeEventListener('resize', this.updateIfNeeded);
    }

    onCardExpansion = (cardId, expanded) => {
      // record which card is presently open, if any
      const currentExpandedCardId = this.state.expandedCardId;
      // get a handle to the card that is expanded presently so we can close it
      const theCardObject = this.getCardObjectById(currentExpandedCardId);

      // need a deep clone of the layout so that ReactGridLayout detects the change
      const layoutClone = _.cloneDeep(this.state.layout);
      const newLayout = layoutClone.map((layout) => {
        const tempLayout = layout;
        // only one expanded card at a time so close any expanded card
        if (layout.i === currentExpandedCardId) {
          tempLayout.h = (layout.maxH / 2);
          theCardObject.card.collapseCard();
        }

        // update the height of the card (shrink or expand)
        if (layout.i === cardId) {
          tempLayout.h = expanded ? layout.maxH : (layout.maxH / 2);
        }

        return tempLayout;
      });

      this.setState({
        layout: newLayout,
        expandedCardId: (expanded) ? cardId : ''
      });
    }

    onDragStart = (layout, oldGridItem) => {
      const dragItem = oldGridItem.i;
      const cardObject = this.getCardObjectById(dragItem);
      if (cardObject) {
        cardObject.card.setDrag(true);
      }
    }

    onDragStop = (layout, oldGridItem) => {
      const dragItem = oldGridItem.i;
      const cardObject = this.getCardObjectById(dragItem);
      if (cardObject) {
        cardObject.card.setDrag(false);
      }
      if (!_.isEqual(this.state.layout, layout)) {
        this.props.onLayoutChange && this.props.onLayoutChange({
          type: 'onLayoutChange',
          value: layout,
          data: this.props.eventData
        });
      }
    }

    onLayoutChange = (layout) => {
      this.setState({
        layout
      });
    }

    // returns the Card object given a Card ID
    getCardObjectById(cardId) {
      return _.find(this.cards, { id: cardId });
    }

    // define so it is only created once (throttle will not work otherwise)
    throttler = _.throttle(this.forceUpdate, 1000);
    updateIfNeeded = () => {
      this.props.dynamicWidth && this.throttler();
    }

    updateLayoutAsResponsive() {
      if (!this.props.dynamicWidth) {
        return;
      }
      // if not set, assume window width. It will be readjusted after it is rendered
      const componentWidth = this.container && this.container.state && this.container.state.width ? this.container.state.width : window.innerWidth;
      const columnPixelWidth = componentWidth / this.props.cols;
      let widthCounter = 0;
      let rowCounter = 0;
      let previousMaxRowHeight = 0; // keep track of previousMaxRowHeight so we know where to go for the next row in the grid
      const newLayout = _.cloneDeep(this.state.layout).map((card) => {
        const tempCard = card;
        const minCardWidth = this.props.minCardWidth * this.originalCardSizes[tempCard.i].w; // minimum pixel width for this card

        // ensure card is at least the min width
        if (tempCard.w * columnPixelWidth < minCardWidth) {
          tempCard.w = tempCard.maxW = Math.ceil(minCardWidth / columnPixelWidth); // eslint-disable-line

        // reduce width (columns occupied) towards original if possible
        } else if (Math.ceil(minCardWidth / columnPixelWidth) < tempCard.w) {
          tempCard.w = Math.ceil(minCardWidth / columnPixelWidth);
          const originalMaxW = this.originalCardSizes[tempCard.i].maxW;
          tempCard.maxW = tempCard.w > originalMaxW ? tempCard.w : originalMaxW;
        }

        // make full width for mobile if need be
        if (tempCard.w > this.props.cols / 2) {
          tempCard.w = tempCard.maxW = this.props.cols; // eslint-disable-line
        }

        previousMaxRowHeight = previousMaxRowHeight || tempCard.h;
        // go to new line if we will overshoot the edge of the grid
        if (widthCounter + tempCard.w > this.props.cols) {
          widthCounter = 0;
          rowCounter += previousMaxRowHeight;
          previousMaxRowHeight = 0;
        }
        tempCard.x = widthCounter;
        tempCard.y = rowCounter;
        // limit card width to max columns if needed
        if (tempCard.w > this.props.cols) { tempCard.w = this.props.cols; }
        widthCounter += tempCard.w;
        return tempCard;
      });

      if (!_.isEqual(newLayout, this.state.layout)) {
        this.setState({ layout: newLayout });
      }
    }

    initLayout(thisLayout, thisChildren) {
      this.originalCardSizes = {};
      const cards = thisChildren;
      const layoutClone = _.cloneDeep(thisLayout);
      const newLayout = layoutClone.map((card, index) => {
        // determine if the cards being passed are react elements or just card objects
        const cardProps = React.isValidElement(cards[index]) ? cards[index].props : cards[index];
        const tempCard = card;
        tempCard.i = (index < cards.length) ? cardProps.id : `x:${card.x}, y:${card.y}`;
        tempCard.maxW = card.w;
        tempCard.maxH = 2 * card.h;
        tempCard.isResizable = false;
        tempCard.isDraggable = true;
        this.originalCardSizes[tempCard.i] = {
          x: tempCard.x, y: tempCard.y, w: tempCard.w, maxW: tempCard.maxW
        };
        return tempCard;
      });
      return newLayout;
    }

    cards = []
    addrefs = (id,ref) => {
      // Remove previous ref for the same id!
      this.cards = this.cards.filter( obj => ( obj.id !== id ) )
      if( ref === null )
        // Do not polluate with unless reference
        return;
      this.cards.push({ id: id, card: ref });
    }

    renderChildren() {
      const cards = this.props.children;
      const numCards = cards.length;
      return this.state.layout.map((layoutItem, index) => {
        if (Object.prototype.hasOwnProperty.call(layoutItem, 'i')) {
          const key = layoutItem.i;
          // we don't want to index past the end of the cards array.
          // less or equal number of cards than grid locations is fine.
          if (index < numCards) {
            const currentCard = cards[index];
            // if the user passes Card as a react Element
            if (React.isValidElement(currentCard)) {
              return (
                <div key={key}>
                  <Card
                    {...currentCard.props}
                    onCardExpansion={this.onCardExpansion}
                    key={key}
                    ref={(ref) => this.addrefs(currentCard.props.id,ref)}
                  />
                </div>
              );
            } // if user passes card array
            const {
              id,
              className,
              expandable,
              initiallyExpanded,
              autoResize,
              children,
              expansionChildren
            } = currentCard;

            const cardStyle = {
              width: currentCard.css.width,
              height: currentCard.css.height,
              backgroundColor: currentCard.css.backgroundColor
            };
            return (
              <div key={key}>
                <Card
                  key={key}
                  id={id}
                  className={className}
                  css={cardStyle}
                  expandable={expandable}
                  initiallyExpanded={initiallyExpanded}
                  autoResize={autoResize}
                  expansionChildren={expansionChildren}
                  onCardExpansion={this.onCardExpansion}
                  ref={(card) => { this.cards.push({ id, card }); }}
                >
                  {children}
                </Card>
              </div>
            );
          }

          return (
            <div key={key}>
              <span className="text">{key}</span>
            </div>
          );
        }

        return null;
      });
    }

    render() {
      const newChildren = this.renderChildren();

      const {
        className,
        alignCenter,
        width,
        height,
        rowHeight,
        cols,
        dynamicWidth,
        margin,
        padding
      } = this.props;

      let cardCollectionStyle = {
        width: (dynamicWidth) ? '100%' : width
      };

      // if a height is defined by the end user, utilize that for the Card Collection
      if (height) {
        cardCollectionStyle = { ...cardCollectionStyle, height };
      }

      // change the Card Collection alignment in the browser based upon user configuration
      if (alignCenter && !dynamicWidth) {
        cardCollectionStyle = { ...cardCollectionStyle, margin: '0 auto' };
      } else {
        cardCollectionStyle = { ...cardCollectionStyle, float: 'left' };
      }

      if (dynamicWidth) {
        return (
          <div className="csfWidgets" data-test="ccfk-cardcollection">
            <DynamicWidthReactGridLayout
              id={this.id}
              className={className}
              style={cardCollectionStyle}
              rowHeight={rowHeight}
              cols={cols}
              layout={this.state.layout}
              margin={margin}
              containerPadding={padding}
              autoSize
              onLayoutChange={this.onLayoutChange}
              onDragStart={this.onDragStart}
              onDragStop={this.onDragStop}
              ref={(container) => { this.container = container; }}
            >
              {newChildren}
            </DynamicWidthReactGridLayout>
          </div>
        );
      }

      return (
        <div className="csfWidgets" data-test="ccfk-cardcollection">
          <GridLayout
            id={this.id}
            className={className}
            style={cardCollectionStyle}
            width={width}
            rowHeight={rowHeight}
            cols={cols}
            layout={this.state.layout}
            margin={margin}
            containerPadding={padding}
            autoSize
            onLayoutChange={this.onLayoutChange}
            onDragStart={this.onDragStart}
            onDragStop={this.onDragStop}
          >
            {newChildren}
          </GridLayout>
        </div>
      );
    }
}
