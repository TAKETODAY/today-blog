import { Skeleton } from 'antd';
import React from 'react';
import { Link } from "react-router-dom";
import { getCacheable, getRandLabel, isEmpty, setTitle } from '../utils';
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";


class Labels extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.state.articles !== nextState.articles
  //   // this.props.match.params.tagsId !== nextProps.match.params.tagsId
  // }

  state = {}

  componentDidMount() {

    const navigations = [{ name: '全部标签', url: '/tags' }]
    this.props.updateNavigations(navigations)
    setTitle("全部标签")
  }

  componentWillUnmount() {
    this.props.updateNavigations()
  }

  render() {
    const { labels } = this.state
    if (isEmpty(labels)) {
      return <Skeleton active/>
    }
    return (<>
      <div className="data_list" id="test1">
        <div className="data_list_title">共有 <b className='red'>{ labels.length }</b> 个分类</div>
        <div className="row labels">
          { labels &&
          labels.map((tag, idx) => {
            return (
                <div key={ idx } className="col-lg-2 col-md-4 col-xs-6 tag">
                  <Link to={ `/tags/${ tag.name }` } title={ tag.name }>
                    <span className={ getRandLabel() }>{ tag.name }</span>
                  </Link>
                </div>
            )
          })
          }
        </div>
      </div>
    </>);
  }
}

export default connect(
    navigationsMapStateToProps, { updateNavigations }
)(Labels)
