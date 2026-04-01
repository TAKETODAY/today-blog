import { BackTop } from 'antd';
import React from 'react';
import qq from '../assets/images/share/qq.png';
import weibo from '../assets/images/share/weibo.png';
import zone from '../assets/images/share/zone.png';
import { shareQQ, shareQQZone, shareWeiBo } from 'core';
import { connect } from "react-redux";
import { optionsMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";

const toTop = "data:image/png;base64,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"

function buildOptions(options) {
  return {
    ...options, url: options["site.host"]
  }
}

class Footer extends React.PureComponent {

  render() {
    const { options } = this.props
    return (
        <section style={ { padding: '10px 16px' } } className="text-center">
          <div className="row" style={ { marginBottom: '16px', marginTop: '55px' } }>
            <img onClick={ () => shareQQ(buildOptions(options)) } className="share" title="分享到QQ好友" src={ qq } width="22"/>
            <img onClick={ () => shareQQZone(buildOptions(options)) } className="share" title="分享到QQ空间" src={ zone } width="25"/>
            <img onClick={ () => shareWeiBo(buildOptions(options)) } className="share" title="分享到微博" src={ weibo } width="25"/>
          </div>
          <div className="copyright">
            <p className="footer-p">
              <a href="https://beian.miit.gov.cn/" target="_blank">
                { options['site.icp'] }
              </a>
            </p>
            <p className="footer-p">{ options['site.copyright'] }</p>
            <p className="footer-p">{ options['site.otherFooter'] }</p>
          </div>
          <BackTop className="backTop" visibilityHeight={ 200 }>
            <img alt="返回顶部" title='返回顶部' src={ toTop }/>
          </BackTop>
        </section>
    )
  }
}

export default connect(
    optionsMapStateToProps, { updateUserSession }
)(Footer)
