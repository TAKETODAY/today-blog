import React, { useState } from 'react';
// import hcSticky from 'hc-sticky';
import { Breadcrumb, CategoriesListGroup, LabelsListGroup, PopularListGroup } from '../components';
import { Affix, Col, Row } from 'antd'
import { connect } from "react-redux";

const RightNav = connect((state) => {
      return {
        ...state.article, http: state.http
      }
    }
)((props) => {
  const { categories, labels, popular } = props
  const [container, setContainer] = useState(null)
  return (
      <div className="scrollable-container" ref={setContainer}>
        <Affix target={() => container}>
          <div>
            <LabelsListGroup labels={labels}/>
            <CategoriesListGroup items={categories}/>
            <PopularListGroup items={popular}/>
          </div>
        </Affix>
      </div>
  );
})


export default props => {
  return (<>
    <div className="container" style={{ marginTop: '75px' }}>
      <Breadcrumb/>
      <Row>
        <Col xs={24} md={18}>
          {props.children}
        </Col>
        {/*侧边栏*/}
        <Col xs={24} md={6} style={{ paddingLeft: 15, paddingRight: 15 }}>
          <RightNav/>
        </Col>
      </Row>
    </div>
  </>)
}
