import { Result, Skeleton } from 'antd';
import 'antd/es/result/style';
import 'antd/es/skeleton/style';
import React from 'react';
import { isEmpty } from 'core';
import { translate } from '../locales';
import { connect } from "react-redux";
import { updateHttpErrorMessage } from "../redux/actions";
import HttpError from "./http/HttpError";


class ListGroup extends React.Component {

  render() {
    const { items, itemFunc, title, titleKey, errors, errorKey, errorTitle } = this.props;
    const heading = titleKey ? translate(titleKey) : title;
    // popularArticles
    const error = errors && errors[errorKey];
    if (error) {
      return <HttpError {...error} title={errorTitle || translate('articles.loadSidebarFailed', { heading })}/>
    }

    if (isEmpty(items)) {
      return <Skeleton active/>
    }
    return (
        <div className='list-group with-title'>
          <div className="list-group-title">{heading}</div>
          { itemFunc &&
          items.map((item, idx) => itemFunc(item, idx))
          }
        </div>
    )
  }
}

function popularArticlesError(state) {
  return { errors: state.http.errors }
}

export default connect(popularArticlesError, {
  updateHttpErrorMessage
})(ListGroup)
