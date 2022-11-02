import React, { useState } from 'react';
// import hcSticky from 'hc-sticky';
import { Breadcrumb, CategoriesListGroup, LabelsListGroup, PopularListGroup } from '../components';
import { Affix, Col, Row } from 'antd'
import { connect } from "react-redux";

export default class ArticleLayout extends React.Component {

  // state = {
  //   affix: null
  // }
  // affixRef = React.createRef();

  // componentDidMount() {
  // if (window.innerWidth >= 1000) {
  // new hcSticky('#right', {
  //   stickTo: '#left',
  //   followScroll: false,
  //   top: 80,
  //   bottom: 100
  // })
  // }
  // }

  setAffix = (affix) => {
    super.setState({ affix })
  }

  render() {
    return (<>
      <div className="container" style={ { marginTop: '75px' } }>
        <Breadcrumb/>
        <Row>
          <Col xs={ 24 } md={ 18 }>
            { this.props.children }
          </Col>
          {/*侧边栏*/ }
          <Col xs={ 24 } md={ 6 } style={ { paddingLeft: 15, paddingRight: 15 } }>
            <RightNav { ...this.state }/>
          </Col>
        </Row>
      </div>
    </>)
  }
}

const RightNav = connect((state) => {
      return {
        ...state.article, http: state.http
      }
    }
)((props) => {
  const { categories, labels, popular } = props
  const [container, setContainer] = useState(null)
  return (
      <div className="scrollable-container" ref={ setContainer }>
        <Affix target={ () => container }>
          <div>
            <LabelsListGroup labels={ labels }/>
            <CategoriesListGroup items={ categories }/>
            <PopularListGroup items={ popular }/>
          </div>
        </Affix>
      </div>
  );
})




