/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

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
								<Image className="img-responsive" src={userSession.avatar} />
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