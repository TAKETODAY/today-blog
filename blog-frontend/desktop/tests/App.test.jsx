import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { createStore } from 'redux';
import App from '../src/App';

vi.mock('../src/components', () => ({
  Footer: () => <footer>footer</footer>,
  Header: () => <header>header</header>,
}));

vi.mock('../src/components/hooks', () => ({
  useUserSession: () => [null],
}));

vi.mock('../src/layouts', () => ({
  ArticleLayout: ({ children }) => <section data-testid="article-layout">{children}</section>,
  SearchLayout: ({ children }) => <section data-testid="search-layout">{children}</section>,
}));

vi.mock('../src/pages', () => ({
  ArticleDetail: () => <div>article detail</div>,
  CategoriesDetail: () => <div>category detail</div>,
  ErrorPage: ({ status }) => <div>error {status}</div>,
  Home: () => <div>home</div>,
  LabelsDetail: () => <div>tag detail</div>,
  Login: () => <div>login</div>,
  Search: () => <div>search</div>,
}));

vi.mock('core', () => ({
  http: { post: vi.fn() },
}));

function renderApp(path) {
  const store = createStore((state = {}) => state);
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[path]}>
        <App />
      </MemoryRouter>
    </Provider>
  );
}

test.each([
  ['/', 'home'],
  ['/articles/123', 'article detail'],
  ['/categories/tech', 'category detail'],
  ['/tags/react', 'tag detail'],
  ['/search', 'search'],
  ['/login', 'login'],
])('renders the %s route', (path, text) => {
  renderApp(path);
  expect(screen.getByText(text)).toBeInTheDocument();
});

test('redirects an unknown route to not-found', () => {
  renderApp('/unknown');
  expect(screen.getByText('error 404')).toBeInTheDocument();
});
