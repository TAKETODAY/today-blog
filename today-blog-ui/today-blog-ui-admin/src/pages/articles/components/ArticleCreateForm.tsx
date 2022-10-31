import React from 'react';
import { Modal } from 'antd';

interface CreateFormProps {
  modalVisible: boolean;
  onCancel: () => void;
}

const ArticleCreateForm: React.FC<CreateFormProps> = (props) => {
  const { modalVisible, onCancel } = props;

  return (
      <Modal
          destroyOnClose
          title="新建轮播图"
          visible={modalVisible}
          onCancel={() => onCancel()}
          footer={null}
      >
        {props.children}
      </Modal>
  );
};

export default ArticleCreateForm;
