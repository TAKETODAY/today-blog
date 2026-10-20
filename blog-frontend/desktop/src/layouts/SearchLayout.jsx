import React from 'react';
import Breadcrumb from '../components/Breadcrumb';
import { Col, Row } from 'antd'
import 'antd/es/grid/style';

export default class SearchLayout extends React.Component {

  render() {
    return (<>
      <div className="container" style={ { marginTop: '75px' } }>
        <Breadcrumb/>
        <Row>
          <Col md={24}>
            { this.props.children }
          </Col>
        </Row>
      </div>
    </>)
  }
}
