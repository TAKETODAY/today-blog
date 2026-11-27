import React, { createElement } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import { Button } from 'antd';

import styles from './index.less';

export interface EditableLink {
  title: string;
  target?: string;
  href: string;
  id?: string;
}

interface EditableLinkGroupProps {
  onAdd?: () => void;
  links: EditableLink[];
  linkElement: any;
  editable?: boolean;
}

const EditableLinkGroup: React.FC<EditableLinkGroupProps> = (props) => {
  const {
    links, linkElement, editable = false,
    onAdd = () => {
    }
  } = props;
  return (
      <div className={styles.linkGroup}>
        {links.map((link) =>
            createElement(
                linkElement,
                {
                  key: `linkGroup-item-${link.id || link.title}`,
                  to: link.href,
                  href: link.href,
                  target: link.target,
                },
                link.title,
            ),
        )}
        {editable &&
        <Button size="small" type="primary" ghost onClick={onAdd}>
          <PlusOutlined/> 添加
        </Button>
        }
      </div>
  );
};

EditableLinkGroup.defaultProps = {
  links: [],
  onAdd: () => {
  },
  linkElement: 'a',
};

export default EditableLinkGroup;
