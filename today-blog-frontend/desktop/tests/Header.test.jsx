import React from 'react';
import ReactDOM from 'react-dom';
import { cleanup, render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { createStore } from 'redux';
import { Modal } from 'antd';
import Header from '../src/components/Header';
import { setDefaultSiteSettings } from '../../common/visitor-preferences';

vi.mock('../src/components/Search', () => ({ default: () => <input aria-label="搜索文章"/> }));

beforeEach(() => {
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['zh-CN']);
});

afterEach(() => {
  cleanup();
  localStorage.clear();
  setDefaultSiteSettings();
  vi.restoreAllMocks();
});

function renderHeader(session = null) {
  const store = createStore((state = {
    user: { session }, options: {},
    article: { categories: [{ name: '后端', articleCount: 3 }], labels: [{ name: 'Java' }] },
  }) => state);
  return render(<Provider store={store}><MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><Header/></MemoryRouter></Provider>);
}

test('renders navigation without the deprecated Menu children API', async () => {
  const error = vi.spyOn(console, 'error').mockImplementation(() => {});
  try {
    renderHeader();
    expect(screen.getByRole('link', { name: /首页/ })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /登录/ })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: /订阅/ })).toHaveAttribute('href', expect.stringMatching(/\/feed\.atom$/));
    expect(screen.getByText('分类')).toBeInTheDocument();
    expect(screen.getByText('标签')).toBeInTheDocument();
    await waitFor(() => expect(error.mock.calls.flat().join(' ')).not.toMatch(/\[antd: Menu\].*children/));
  }
  finally { error.mockRestore(); }
});

test('switches language directly from the menu without changing the saved time zone', async () => {
  setDefaultSiteSettings({ 'site.locale': 'en-US' });
  localStorage.setItem('today-blog.timezone', 'America/New_York');
  renderHeader();

  const trigger = screen.getByRole('button', { name: '切换语言' });
  expect(trigger).toHaveAttribute('aria-expanded', 'false');
  fireEvent.click(trigger);
  expect(await screen.findByRole('menuitemradio', { name: '跟随浏览器', checked: true })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('menuitemradio', { name: '站点默认语言' }));

  expect(screen.getByRole('button', { name: 'Change language' })).toHaveAttribute('aria-expanded', 'false');
  expect(screen.getByRole('link', { name: /Sign in/ })).toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(localStorage.getItem('today-blog.locale')).toBe('site');
  expect(localStorage.getItem('today-blog.timezone')).toBe('America/New_York');

  fireEvent.click(trigger);
  expect(await screen.findByRole('menuitemradio', { name: 'Site language', checked: true })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('menuitemradio', { name: 'Browser language' }));
  expect(screen.getByRole('button', { name: '切换语言' })).toBeInTheDocument();
  expect(localStorage.getItem('today-blog.locale')).toBeNull();
});

test('opens the independent time zone selector from the language menu', async () => {
  const getComputedStyle = window.getComputedStyle.bind(window);
  vi.spyOn(window, 'getComputedStyle').mockImplementation(element => getComputedStyle(element));
  localStorage.setItem('today-blog.locale', 'site');
  localStorage.setItem('today-blog.timezone', 'America/New_York');
  renderHeader();

  expect(screen.queryByRole('combobox', { name: '显示时区' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: '切换语言' }));
  fireEvent.click(await screen.findByRole('menuitem', { name: '显示时区' }));

  expect(await screen.findByRole('dialog', { name: '显示时区' })).toBeInTheDocument();
  expect(screen.getByText('America/New_York')).toBeInTheDocument();
  fireEvent.mouseDown(screen.getByRole('combobox', { name: '显示时区' }));
  fireEvent.click(await screen.findByText('站点默认时区'));
  expect(localStorage.getItem('today-blog.locale')).toBe('site');
  expect(localStorage.getItem('today-blog.timezone')).toBeNull();
});

test.each([true, false])('preserves user menu permissions and logout for blogger=%s', async blogger => {
  const confirm = vi.spyOn(Modal, 'confirm').mockImplementation(() => ({}));
  const findDOMNode = vi.spyOn(ReactDOM, 'findDOMNode');
  try {
    renderHeader({ blogger, avatar: '/avatar.png' });
    fireEvent.click(screen.getByAltText('pic'));
    await waitFor(() => expect(screen.getByText('安全退出')).toBeInTheDocument());
    expect(screen.queryByText('写文章') !== null).toBe(blogger);
    expect(screen.queryByText('后台管理') !== null).toBe(blogger);
    fireEvent.click(screen.getByText('安全退出'));
    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({ title: '安全退出' }));
    expect(findDOMNode).not.toHaveBeenCalled();
  }
  finally {
    confirm.mockRestore();
    findDOMNode.mockRestore();
  }
});
