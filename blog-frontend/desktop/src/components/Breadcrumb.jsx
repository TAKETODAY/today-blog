import React from 'react';
import { HomeFilled } from '@ant-design/icons';
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { translate } from 'core';
import { useDisplayPreferences } from './hooks/useDisplayPreferences';


export default connect(navigationsMapStateToProps)(({ navigations }) => {
  useDisplayPreferences();
  const home = translate('首页', 'Home');
  return (
      <div className="row">
        <ul className="breadcrumb">
          <li>
            <Link title={home} aria-label={home} to="/"> <HomeFilled aria-hidden="true"/></Link>
          </li>
          {navigations && navigations.map((nav, idx) => {
            const name = nav.label ? translate(...nav.label) : nav.name;
            return <li key={idx}><Link title={name} to={nav.url || '/'}>{name}</Link></li>
          })}
        </ul>
      </div>
  )
})
