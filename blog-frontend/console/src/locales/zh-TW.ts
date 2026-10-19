import component from './zh-TW/component';
import articles from './zh-TW/articles';
import globalHeader from './zh-TW/globalHeader';
import menu from './zh-TW/menu';
import pwa from './zh-TW/pwa';
import settingDrawer from './zh-TW/settingDrawer';
import settings from './zh-TW/settings';
import pages from './zh-TW/pages';

export default {
  ...articles,
  'app.layout.settings': '介面設定',
  'app.layout.settings.saveFailed': '介面已更新，但瀏覽器無法儲存設定，重新整理後將恢復預設值。',
  'navBar.lang': '語言',
  'layout.user.link.help': '幫助',
  'layout.user.link.privacy': '隱私',
  'layout.user.link.terms': '條款',
  'app.preview.down.block': '下載此頁面到本地項目',
  'app.welcome.link.fetch-blocks': '取得全部區塊',
  'app.welcome.link.block-list': '以 block 開發，快速建構標準頁面',
  ...pages,
  ...globalHeader,
  ...menu,
  ...settingDrawer,
  ...settings,
  ...pwa,
  ...component,
};
