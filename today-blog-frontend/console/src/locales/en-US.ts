import component from './en-US/component';
import articles from './en-US/articles';
import globalHeader from './en-US/globalHeader';
import menu from './en-US/menu';
import pwa from './en-US/pwa';
import settingDrawer from './en-US/settingDrawer';
import settings from './en-US/settings';
import pages from './en-US/pages';
import attachmentMessages from './en-US/attachmentMessages';
import interfaceMessages from './en-US/interfaceMessages';
import imageChooserMessages from './en-US/imageChooserMessages';
import errorsMessages from './en-US/errorsMessages';
import accountMessages from './en-US/accountMessages';
import apiKeysMessages from './en-US/apiKeysMessages';
import backupsMessages from './en-US/backupsMessages';
import logsMessages from './en-US/logsMessages';
import mailMessages from './en-US/mailMessages';
import optionsMessages from './en-US/optionsMessages';
import upgradeMessages from './en-US/upgradeMessages';
import usersMessages from './en-US/usersMessages';
import loginMessages from './en-US/loginMessages';
import workplaceMessages from './en-US/workplaceMessages';
import statusMessages from './en-US/statusMessages';

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
  ...articles,
  'app.layout.settings': 'Interface settings',
  'app.layout.settings.saveFailed': 'Settings applied, but could not be saved. Defaults will be restored after reloading.',
  'navBar.lang': 'Languages',
  'layout.user.link.help': 'Help',
  'layout.user.link.privacy': 'Privacy',
  'layout.user.link.terms': 'Terms',
  'app.preview.down.block': 'Download this page to your local project',
  'app.welcome.link.fetch-blocks': 'Get all block',
  'app.welcome.link.block-list': 'Quickly build standard, pages based on `block` development',
  ...globalHeader,
  ...menu,
  ...settingDrawer,
  ...settings,
  ...pwa,
  ...component,
  ...pages,
};
