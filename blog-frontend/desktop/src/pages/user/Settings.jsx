import React from 'react';
import {Settings } from '../../components';
import { UserLayout } from '../../layouts';

export default class UserSettings extends React.Component {

  state = {}

  render() {
    return (
      <UserLayout>
        <Settings />
      </UserLayout>
    )
  }
}
