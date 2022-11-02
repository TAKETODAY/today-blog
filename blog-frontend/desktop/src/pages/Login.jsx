import React from 'react';
import { Login } from '../components';
import { connect } from "react-redux";
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import { getQuery } from "../utils";


class LoginPage extends React.Component {

  render() {
    const message = getQuery("message");
    return (
        <div id="loginForm" style={ { marginTop: '100px' } }>
          <Login message={message}/>
        </div>
    )
  }
}

export default connect(
    userSessionMapStateToProps, { updateUserSession }
)(LoginPage)

