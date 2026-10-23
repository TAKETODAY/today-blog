import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { createStore } from 'redux';
import Home from '../src/pages/Home';
import { articleService } from '../src/services';

vi.mock('../src/components/ArticleList', () => ({ default: () => <div>articles</div> }));
vi.mock('../src/services', () => ({ articleService: { fetchHomeArticles: vi.fn() } }));
vi.mock('../src/redux/actions', () => ({ updateNavigations: () => ({ type: 'navigation' }) }));
vi.mock('../src/utils/common', () => ({ setTitle: vi.fn() }));
vi.mock('core', () => ({ applySEO: vi.fn(), extractData: response => response.data, scrollTop: vi.fn() }));

function renderHome(path = '/') {
  const store = createStore((state = { navigations: [] }) => state);
  articleService.fetchHomeArticles.mockImplementation((page = 1, size = 10) => Promise.resolve({
    data: { total: 200, current: page, size, data: [] },
  }));
  return render(<Provider store={store}><MemoryRouter initialEntries={[path]}><Home /></MemoryRouter></Provider>);
}

beforeEach(() => {
  vi.clearAllMocks();
  window.matchMedia = vi.fn().mockImplementation(media => ({
    matches: false, media, addListener: vi.fn(), removeListener: vi.fn(),
  }));
});

test('renders crawlable page links instead of anchors without href', async () => {
  const { container } = renderHome();
  const link = await screen.findByRole('link', { name: '2' });
  expect(link).toHaveAttribute('href', '/?page=2&size=10');
  expect(container.querySelector('a:not([href])')).toBeNull();
});

test('loads the page and size from a directly visited pagination URL', async () => {
  renderHome('/?page=3&size=20');
  await waitFor(() => expect(articleService.fetchHomeArticles).toHaveBeenCalledWith(3, 20));
});

test.each(['/?page=-1&size=0', '/?page=abc&size=Infinity', '/?page=1.5&size=2.5'])(
  'falls back to defaults for invalid pagination parameters: %s', async path => {
    renderHome(path);
    await waitFor(() => expect(articleService.fetchHomeArticles).toHaveBeenCalledWith(1, 10));
  }
);

test('changes page through a link and through the next-page control', async () => {
  renderHome();
  fireEvent.click(await screen.findByRole('link', { name: '2' }));
  await waitFor(() => expect(articleService.fetchHomeArticles).toHaveBeenLastCalledWith(2, 10));
  fireEvent.click(document.querySelector('.ant-pagination-next button'));
  await waitFor(() => expect(articleService.fetchHomeArticles).toHaveBeenLastCalledWith(3, 10));
});
