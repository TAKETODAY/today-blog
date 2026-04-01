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

import { MailOutlined } from '@ant-design/icons';
import { Button, Card, Col, Modal, Row } from 'antd';
import React, { useState } from 'react';
import { GridContent } from '@ant-design/pro-layout';
import styles from './Center.less';
import Settings from "@/pages/system/account/settings"
import { useUserSession } from "@/components/hooks"
import Image from "@/components/Image";

const Center = () => {
  const [userSession, setUserSession] = useUserSession()
  const [modalVisible, setUpdateModalVisible] = useState(false)

  const showModal = () => setUpdateModalVisible(true)
  const closeModal = () => setUpdateModalVisible(false)

  //  渲染用户信息
  const renderUserInfo = ({ email, mobilePhone }) => {
    return (
      <div className={styles.detail}>
        <p>
          <MailOutlined
            style={{
              marginRight: 8,
            }}
          />
          {email}
        </p>
      </div>
    )
  }

  return (
    <GridContent>
      <Row gutter={24}>
        <Col lg={7} md={24}>
          <Card bordered={false} style={{ marginBottom: 24 }} extra={
            <Button onClick={showModal}>更改信息</Button>
          }>
            {userSession && (
              <div>
                <div className={styles.avatarHolder}>
                  <Image alt="头像" src={userSession.avatar}/>
                  <div className={styles.name}>{userSession.name}</div>
                  <div>{userSession?.introduce}</div>
                </div>
                {renderUserInfo(userSession)}
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Modal width={600} title="更改账号信息" onCancel={closeModal} maskClosable={false}
             open={modalVisible} footer={<Button onClick={closeModal}>关闭</Button>}>
        <Settings/>
      </Modal>
    </GridContent>
  );
};
export default Center;
