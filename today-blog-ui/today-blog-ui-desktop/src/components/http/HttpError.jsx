import { Result, Skeleton } from 'antd';
import React from 'react';
import { ExclamationCircleOutlined } from "@ant-design/icons";

export default class HttpError extends React.Component {

  render() {
    return (
        <Result { ...this.props }
                status
                icon={ <ExclamationCircleOutlined/> }/>
    )
  }
}
