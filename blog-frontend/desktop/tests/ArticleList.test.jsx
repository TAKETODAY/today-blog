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
      <MemoryRouter>
        <ArticleList articles={articles} />
      </MemoryRouter>
    </Provider>
  );
  const images = screen.getAllByRole('img');
  expect(images[0]).toHaveAttribute('loading', 'eager');
  expect(images[1]).toHaveAttribute('loading', 'lazy');
  images.forEach(image => expect(image).toHaveAttribute('decoding', 'async'));
});
