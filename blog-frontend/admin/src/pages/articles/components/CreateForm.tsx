import React from 'react';
import { Modal } from 'antd';

interface CreateFormProps {
  modalVisible: boolean;
  onCancel: () => void;
  title: string;
}

const BannerCreateForm: React.FC<CreateFormProps> = (props) => {
  const { modalVisible, onCancel, title } = props;

  return (
      <Modal
          destroyOnClose
          title={title}
          visible={modalVisible}
          onCancel={() => onCancel()}
          footer={null}
      >
        {props.children}
      </Modal>
  );
};

export default BannerCreateForm;
