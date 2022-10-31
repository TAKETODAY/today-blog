import React from 'react';
import { Image } from '../';
import { Button } from 'antd';
import { Link } from 'react-router-dom';

export default class UserDescription extends React.Component {

	getStyle = (background) => {
		return {
			background: `url(${background}) no-repeat center`,
		}
	}

	render() {
		const { userSession } = this.props
		return (<>
			<div className="card">
				<div className="user-background-box">
					<Image src={userSession.background} alt="用户封面" />
				</div>
				<div className="user-box" >
					<div className="user-content">
						<div className="user-avatar-box">
							<a href="/user/settings#avatar" title="点击修改头像" >
								<Image className="img-responsive" src={userSession.image} />
							</a>
						</div>
						<div className="user-introduce">
							<h1 title={userSession.name}>{userSession.name}</h1>
							<div >
								<span title={userSession.introduce}>{userSession.introduce}</span>
							</div>
						</div>
						<div className="user-buttons">
							<Button type='dashed'>
								<Link to="/user/info" title="主页">主页</Link>
							</Button>
							<Button type='dashed'>
								<Link to="/user/settings" title="修改资料">修改资料</Link>
							</Button>
						</div>
					</div>
				</div>
			</div>
		</>
		)

	}
}