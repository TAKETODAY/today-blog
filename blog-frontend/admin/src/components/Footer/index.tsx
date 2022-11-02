import React from 'react';
import { useModel } from "@@/plugin-model/useModel";
import { DefaultFooter } from '@ant-design/pro-layout';

export default () => {
  const { initialState } = useModel('@@initialState');
  // @ts-ignore
  const { options } = initialState
  return (
      <DefaultFooter
          copyright={options['site.copyright']}
          links={[
            {
              key: 'TODAY BLOG',
              title: 'Powered By TODAY',
              href: 'https://taketoday.cn',
              blankTarget: true,
            }
          ]}
      />
  )
}
