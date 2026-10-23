import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { createStore } from 'redux';
import { Modal } from 'antd';
import Header from '../src/components/Header';

vi.mock('../src/components/Search', () => ({ default: () => <input aria-label="搜索文章"/> }));

function renderHeader(session = null) {
  const store = createStore((state = {
    user: { session }, options: {},
    article: { categories: [{ name: '后端', articleCount: 3 }], labels: [{ name: 'Java' }] },
  }) => state);
  return render(<Provider store={store}><MemoryRouter><Header/></MemoryRouter></Provider>);
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

test.each([true, false])('preserves user menu permissions and logout for blogger=%s', async blogger => {
  const confirm = vi.spyOn(Modal, 'confirm').mockImplementation(() => ({}));
  try {
    renderHeader({ blogger, avatar: '/avatar.png' });
    fireEvent.click(screen.getByAltText('pic'));
    await waitFor(() => expect(screen.getByText('安全退出')).toBeInTheDocument());
    expect(screen.queryByText('写文章') !== null).toBe(blogger);
    expect(screen.queryByText('后台管理') !== null).toBe(blogger);
    fireEvent.click(screen.getByText('安全退出'));
    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({ title: '安全退出' }));
  }
  finally { confirm.mockRestore(); }
});
