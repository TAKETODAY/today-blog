import component from './zh-CN/component';
import articles from './zh-CN/articles';
import globalHeader from './zh-CN/globalHeader';
import menu from './zh-CN/menu';
import pwa from './zh-CN/pwa';
import settingDrawer from './zh-CN/settingDrawer';
import settings from './zh-CN/settings';
import pages from './zh-CN/pages';
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
  ...statusMessages,
  ...workplaceMessages,
  ...loginMessages,
  ...usersMessages,
  ...upgradeMessages,
  ...optionsMessages,
  ...mailMessages,
  ...logsMessages,
  ...backupsMessages,
  ...apiKeysMessages,
  ...accountMessages,
  ...errorsMessages,
  ...imageChooserMessages,
  ...interfaceMessages,
  ...attachmentMessages,
  'app.layout.settings': '界面设置',
  'app.layout.settings.saveFailed': '界面已更新，但浏览器未能保存设置，刷新后将恢复默认。',
  'navBar.lang': '语言',
  'layout.user.link.help': '帮助',
  'layout.user.link.privacy': '隐私',
  'layout.user.link.terms': '条款',
  'app.preview.down.block': '下载此页面到本地项目',
  'app.welcome.link.fetch-blocks': '获取全部区块',
  'app.welcome.link.block-list': '基于 block 开发，快速构建标准页面',
  ...pages,
  ...globalHeader,
  ...menu,
  ...settingDrawer,
  ...settings,
  ...pwa,
  ...component,
  ...articles,
};
