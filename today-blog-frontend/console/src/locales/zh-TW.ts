import component from './zh-TW/component';
import articles from './zh-TW/articles';
import globalHeader from './zh-TW/globalHeader';
import menu from './zh-TW/menu';
import pwa from './zh-TW/pwa';
import settingDrawer from './zh-TW/settingDrawer';
import settings from './zh-TW/settings';
import pages from './zh-TW/pages';
import attachmentMessages from './zh-CN/attachmentMessages';
import interfaceMessages from './zh-CN/interfaceMessages';
import imageChooserMessages from './zh-CN/imageChooserMessages';
import errorsMessages from './zh-CN/errorsMessages';
import accountMessages from './zh-CN/accountMessages';
import apiKeysMessages from './zh-CN/apiKeysMessages';
import backupsMessages from './zh-CN/backupsMessages';
import logsMessages from './zh-CN/logsMessages';
import mailMessages from './zh-CN/mailMessages';
import optionsMessages from './zh-CN/optionsMessages';
import upgradeMessages from './zh-CN/upgradeMessages';
import usersMessages from './zh-CN/usersMessages';
import loginMessages from './zh-CN/loginMessages';
import workplaceMessages from './zh-CN/workplaceMessages';
import statusMessages from './zh-CN/statusMessages';

export default {
  // Site display languages are Simplified Chinese and English.
  ...attachmentMessages,
  ...interfaceMessages,
  ...imageChooserMessages,
  ...errorsMessages,
  ...accountMessages,
  ...apiKeysMessages,
  ...backupsMessages,
  ...logsMessages,
  ...mailMessages,
  ...optionsMessages,
  ...upgradeMessages,
  ...usersMessages,
  ...loginMessages,
  ...workplaceMessages,
  ...statusMessages,
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
