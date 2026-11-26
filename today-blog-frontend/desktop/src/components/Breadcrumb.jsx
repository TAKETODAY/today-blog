import React from 'react';
import { HomeFilled } from '@ant-design/icons';
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { translate } from '../locales';
import { useDisplayPreferences } from './hooks/useDisplayPreferences';


export default connect(navigationsMapStateToProps)(({ navigations }) => {
  useDisplayPreferences();
  const home = translate('navigation.home');
  return (
      <div className="row">
        <ul className="breadcrumb">
          <li>
            <Link title={home} aria-label={home} to="/"> <HomeFilled aria-hidden="true"/></Link>
          </li>
          {navigations && navigations.map((nav, idx) => {
            const name = nav.labelKey ? translate(nav.labelKey) : nav.name;
            return <li key={idx}><Link title={name} to={nav.url || '/'}>{name}</Link></li>
          })}
        </ul>
      </div>
  )
})
