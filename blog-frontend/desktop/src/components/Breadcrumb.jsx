import React from 'react';
import { Link } from "react-router-dom";
import { arrayNotEquals } from 'core';
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";

class Breadcrumb extends React.Component {

  shouldComponentUpdate(nextProps, nextState) {
    return arrayNotEquals(this.props.navigations, nextProps.navigations)
  }

  render() {
    const { navigations } = this.props
    // debugger
    return (
        <div className="row">
            <ul className="breadcrumb">
              <li>
                <Link title="首页" to="/"> <i className="fa fa-home"/></Link>
              </li>
              { navigations && navigations.map((nav, idx) => {
                return <li key={ idx }><Link title={ nav.name } to={ nav.url || '/' }>{ nav.name } </Link></li>
              }) }
            </ul>
        </div>
    )
  }
}

export default connect(
    navigationsMapStateToProps
)(Breadcrumb)
