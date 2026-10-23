import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { createStore } from 'redux';
import ArticleList from '../src/components/ArticleList';

vi.mock('../src/components/Image', () => ({
  default: ({ original, ...props }) => <img {...props} />,
}));

test('keeps the first cover eager and defers subsequent covers', () => {
  const store = createStore((state = { user: { session: null } }) => state);
  const articles = [0, 1].map(index => ({
    uri: String(index), title: `文章 ${index}`, summary: `摘要 ${index}`,
    cover: `/cover-${index}.webp`, createAt: 0, pv: 0,
  }));
  render(
    <Provider store={store}>
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <ArticleList articles={articles} />
      </MemoryRouter>
    </Provider>
  );
  const images = screen.getAllByRole('img');
  expect(images[0]).toHaveAttribute('loading', 'eager');
  expect(images[1]).toHaveAttribute('loading', 'lazy');
  images.forEach(image => expect(image).toHaveAttribute('decoding', 'async'));
  images.forEach(image => {
    expect(image).not.toHaveAttribute('width');
    expect(image).not.toHaveAttribute('height');
    expect(image.closest('.article-cover')).not.toBeNull();
  });
});

test('shows pinned labels only on the home list without reordering articles', () => {
  const store = createStore((state = { user: { session: null } }) => state);
  const articles = [
    { uri: 'normal', title: '普通文章', createAt: 0, pv: 0 },
    { uri: 'pinned', title: '置顶文章', createAt: 0, pv: 0, pinnedAt: '2026-10-21T00:00:00Z' },
  ];
  const list = showPinned => (
    <Provider store={store}>
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><ArticleList articles={articles} showPinned={showPinned} /></MemoryRouter>
    </Provider>
  );
  const { rerender } = render(list(false));
  expect(screen.queryByText('置顶', { exact: true })).not.toBeInTheDocument();
  rerender(list(true));
  expect(screen.getByText('置顶', { exact: true })).toBeInTheDocument();
  expect(screen.getAllByRole('heading').map(heading => heading.textContent))
    .toEqual(['普通文章', '置顶置顶文章']);
});
