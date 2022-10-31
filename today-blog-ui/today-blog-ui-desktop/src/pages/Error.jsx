import React from 'react';
import head from '../assets/images/404/head.png';

const errors = {
  '400': { msg: '400 Bad Request !', desc: '错误请求 !' },
  '403': { msg: '403 Access Forbidden !', desc: '服务器拒绝处理您的请求！您可能没有访问此操作的权限 !' },
  '404': { msg: '404 NOT Found !', desc: '您请求的页面,我找遍了整个服务器都没找到 !' },
  '405': { msg: '405 Method Not Allowed !', desc: '您请求的方式不正确 !' },
  '500': { msg: '500 Internal Server Error !', desc: '服务器出错啦 !' },
}

export default class Error extends React.Component {

  render() {
    const { status } = this.props
    let error = errors[`${status}`]
    if (!error) {
      error = errors['500']
    }
    document.title = error.msg
    return (<>
      <div className="container" style={{ marginTop: '100px' }}>
        <div className="row">
          <div className="col-md-12 col-lg-12">
            <div className="bg" align="center">
              <div id="box">
                <div className="img"><img src={head} /></div>
                <h1 style={{ fontSize: '48px' }}>{error.msg}</h1>
                <div id="msg" style={{ fontSize: '15px' }}>{error.desc}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>)
  }
}


