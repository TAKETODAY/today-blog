import { Settings as LayoutSettings } from '@ant-design/pro-layout';

const Settings: LayoutSettings & {
  pwa?: boolean;
  logo?: string;
} = {
  navTheme: 'light',
  // 拂晓蓝
  primaryColor: '#337ab7',
  // layout: 'mix',
  layout: 'side',
  contentWidth: 'Fluid',
  // fixedHeader: false,
  fixedHeader: true,
  fixSiderbar: true,
  colorWeak: false,
  title: 'TODAY BLOG',
  pwa: false,
  logo: '/logo.svg',
  iconfontUrl: '',
};

export default Settings;
