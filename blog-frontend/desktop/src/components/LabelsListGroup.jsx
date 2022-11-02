import { Skeleton } from 'antd';
import React from 'react';
import { getRandLabel, isEmpty } from '../utils';
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { updateHttpErrorMessage } from "../redux/actions";
import HttpError from "./http/HttpError";

class Labels extends React.Component {
  //
  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.props.labels !== nextProps.labels
  // }

  render() {
    const { labels, error } = this.props;
    if (error) {
      return <HttpError { ...error } title="标签加载失败"/>
    }

    if (isEmpty(labels)) {
      return <Skeleton active/>
    }
    return (
        <div className="data_list" id="tagcloud">
          <div className="data_list_title">标签云</div>
          <div style={ { paddingTop: 10 } }>
            {
              labels.map((label, idx) => {
                return <Link key={ idx } to={ `/tags/${ label.name }` } className={ getRandLabel() } title={ label.name }>{ label.name }</Link>
              })
            }
          </div>
        </div>
    )
  }
}


function labelsError(state) {
  return { error: state.http.errors.tags }
}

export default connect(labelsError, {
  updateHttpErrorMessage
})(Labels)

