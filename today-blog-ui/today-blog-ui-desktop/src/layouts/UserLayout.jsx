import React from 'react';
import { Link } from 'react-router-dom';
import { InfoRight, UserDescription, UserNav } from '../components';
import { Affix, Anchor, Col, Row } from 'antd'

const AnchorLink = Anchor.Link;

export default class UserLayout extends React.Component {

  componentDidMount() {
  }

  render() {
    const { userSession } = this.props
    if (userSession === null) {
      this.props.history.push('/login')
      return null
    }
    if (!userSession) {
      return <></>
    }
    return (<>
      <div className="container" style={ { marginTop: '70px' } }>
        <Row>
          <Col xs={ 24 } md={ 18 } lg={ 24 }>
            { userSession.defaultPassword &&
            <div className="alert alert-info" role="alert">提示：当前使用默认密码:'https://taketoday.cn' 请尽快
              <Link to="/user/settings#password" className="alert-link">修改密码</Link>
            </div>
            }
            <UserDescription userSession={ userSession }/>
            {/* <UserNav /> */ }
          </Col>
          <Col xs={ 24 } md={ 18 } lg={ 18 }>
            { this.props.children }
          </Col>
          {/*侧边栏*/ }
          <Col xs={ 24 } md={ 6 } style={ { paddingLeft: 15, paddingRight: 15 } }>
            <Affix offsetTop={ 80 }>
              <InfoRight userSession={ userSession }/>
              <Anchor offsetTop={ 80 } className="list-group">
                <AnchorLink href="settings#info" title="基本信息修改"/>
                <AnchorLink href="settings#email" title="邮箱修改"/>
                <AnchorLink href="settings#password" title="密码修改"/>
                <AnchorLink href="settings#avatar" title="头像修改"/>
                <AnchorLink href="settings#background" title="背景修改"/>
              </Anchor>
            </Affix>

          </Col>
        </Row>
      </div>
    </>)
  }
}
