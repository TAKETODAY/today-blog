import { MailOutlined, MobileOutlined } from '@ant-design/icons';
import { Button, Card, Col, Modal, Row } from 'antd';
import React, { useState } from 'react';
import { GridContent } from '@ant-design/pro-layout';
import styles from './Center.less';
import Settings from "@/pages/system/account/settings"
import { useUserSession } from "@/components/hooks"

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
        <p>
          <MobileOutlined
            style={{
              marginRight: 8,
            }}
          />
          {mobilePhone}
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
                  <img alt="" src={userSession.avatar}/>
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
