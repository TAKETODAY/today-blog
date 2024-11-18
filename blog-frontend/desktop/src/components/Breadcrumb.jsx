import React from 'react';
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";


export default connect(navigationsMapStateToProps)(({ navigations }) => {
  //const [navigations] = useBreadcrumb()
  //console.log(navigations)
  return (
      <div className="row">
        <ul className="breadcrumb">
          <li>
            <Link title="首页" to="/"> <i className="fa fa-home"/></Link>
          </li>
          {navigations && navigations.map((nav, idx) => {
            return <li key={idx}><Link title={nav.name} to={nav.url || '/'}>{nav.name} </Link></li>
          })}
        </ul>
      </div>
  )
})
