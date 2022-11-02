import React from 'react';
import { UserComment } from 'src/components';
import { UserLayout } from 'src/layouts';
import { connect } from "react-redux";
import { userSessionMapStateToProps } from "../../redux/action-types";
import { Link, withRouter } from 'react-router-dom';

class UserInfo extends React.Component {

  render() {
    return (
        <UserLayout>
          <UserComment/>
        </UserLayout>
    )
  }
}

export default connect(
    userSessionMapStateToProps
)(withRouter(UserInfo))
