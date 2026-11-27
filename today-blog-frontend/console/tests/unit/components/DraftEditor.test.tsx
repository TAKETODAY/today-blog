import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { history, useLocation } from '@umijs/max';
import DraftEditor from '@/pages/articles/components/DraftEditor';
import { readDraft, saveDraft } from '@/pages/articles/drafts/storage';
import articleService from '@/services/ArticleService';
import routes from '../../../config/routes';

jest.mock('@umijs/max', () => ({
  history: { replace: jest.fn(), push: jest.fn() },
  useLocation: jest.fn(),
  useParams: () => ({}),
  useModel: () => ({ initialState: { currentUser: { id: 1 } } }),
}));
jest.mock('@/utils/article-i18n', () => ({ articleMessage: key => key }));
jest.mock('@/utils', () => ({ showHttpErrorMessage: jest.fn() }));
jest.mock('@/components/hooks', () => ({ useOptions: () => [{ 'site.host': 'https://blog.example' }] }));
jest.mock('@/services/ArticleService', () => ({ getById: jest.fn(), create: jest.fn(), updateById: jest.fn() }));
jest.mock('@/components/Editor', () => ({ value, onChange, options }) => <>
  <textarea aria-label="markdown" value={value || ''} onChange={e => onChange(e.target.value, options.renderMarkdown(e.target.value))}/>
  <div data-testid="preview" dangerouslySetInnerHTML={{ __html: options.renderMarkdown(value) }}/>
</>);
jest.mock('@/pages/articles/components/ArticleSettingsDrawer', () => ({ visible, onSubmit }) => visible
  ? <button onClick={() => onSubmit({ uri: 'published' }, jest.fn())}>publish</button> : null);
jest.mock('@/components/ImageChooserModal', () => () => null);

describe('draft dialog in article editor', () => {
  const actEnvironment = globalThis.IS_REACT_ACT_ENVIRONMENT;
  const matchMedia = window.matchMedia;
  let container: HTMLDivElement;
  let root = createRoot(document.createElement('div'));
  let location: { pathname: string; search: string };

  beforeAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = true;
    window.matchMedia = jest.fn(() => ({ matches: false, addListener: jest.fn(), removeListener: jest.fn() })) as any;
  });

  afterAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = actEnvironment;
    window.matchMedia = matchMedia;
  });

  beforeEach(() => {
    jest.clearAllMocks();
    const data = new Map<string, string>();
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        get length() {
          return data.size;
        },
        key: index => Array.from(data.keys())[index] ?? null,
        getItem: key => data.get(key) ?? null,
        setItem: (key, value) => data.set(key, value),
        removeItem: key => data.delete(key),
      },
    });
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
    location = { pathname: '/articles/write', search: '?draft=first' };
    (useLocation as jest.Mock).mockImplementation(() => location);
    (history.replace as jest.Mock).mockImplementation(next => {
      location = next;
      root.render(<DraftEditor mode="markdown"/>);
    });
    saveDraft(1, { version: 1, id: 'first', mode: 'markdown', savedAt: 1, post: { title: 'First', markdown: 'First text' } });
    saveDraft(1, { version: 1, id: 'second', mode: 'markdown', articleId: '42', savedAt: 2, post: { title: 'Second', markdown: 'Second text' } });
    act(() => root.render(<DraftEditor mode="markdown"/>));
  });

  afterEach(() => {
    act(() => root.unmount());
    container.remove();
  });

  async function click(label: string, scope: ParentNode = document) {
    const button = Array.from(scope.querySelectorAll('button')).find(element => element.textContent.trim() === label);
    expect(button).toBeDefined();
    await act(async () => button.click());
  }

  it('opens and closes the dialog without navigation and switches drafts on the same editor path', async () => {
    const textarea = container.querySelector('textarea');
    act(() => {
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set.call(textarea, 'Unsaved text');
      textarea.dispatchEvent(new Event('input', { bubbles: true }));
    });
    await click('localDrafts', container);
    expect(readDraft(1, 'first').post.markdown).toBe('Unsaved text');
    expect(history.replace).not.toHaveBeenCalled();
    expect(document.body.textContent).toContain('currentDraft');
    await act(async () => document.querySelector<HTMLButtonElement>('.ant-modal-close').click());
    expect(container.querySelector('textarea').value).toBe('Unsaved text');
    await click('localDrafts', container);
    const row = document.querySelector('[data-draft-id="second"]');
    await click('resumeDraft', row);
    expect(location.pathname).toBe('/articles/write');
    expect(new URLSearchParams(location.search).get('articleId')).toBe('42');
    expect(container.querySelector<HTMLInputElement>('.article-editor-title').value).toBe('Second');
    expect(container.querySelector('textarea').value).toBe('Second text');
    expect(articleService.getById).not.toHaveBeenCalled();
    expect(history.push).not.toHaveBeenCalled();
    await click('localDrafts', container);
    await click('resumeDraft', document.querySelector('[data-draft-id="first"]'));
    expect(new URLSearchParams(location.search).get('articleId')).toBe('');
    expect(container.querySelector('textarea').value).toBe('Unsaved text');
    expect(container.querySelector<HTMLInputElement>('.article-editor-title').value).toBe('First');
  });

  it('does not expose a separate drafts page', () => {
    expect(routes.find(route => route.path === '/blog').routes.some(route => route.path === '/blog/drafts')).toBe(false);
  });

  it('saves HTML with the same Markdown rules as the preview', async () => {
    const source = '# Title\n\n**bold** and [docs](../docs)';
    const textarea = container.querySelector('textarea');
    act(() => {
      Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set.call(textarea, source);
      textarea.dispatchEvent(new Event('input', { bubbles: true }));
    });
    const preview = container.querySelector('[data-testid="preview"]').innerHTML;
    expect(preview).toContain('<h1>Title</h1>');
    expect(preview).toContain('href="https://blog.example/docs"');
    (articleService.create as jest.Mock).mockResolvedValueOnce({});
    (history.replace as jest.Mock).mockImplementationOnce(() => {});
    await click('saveArticle', container);
    await click('publish', container);
    expect(articleService.create).toHaveBeenCalledWith(expect.objectContaining({
      markdown: source,
      content: preview,
      uri: 'published',
    }));
  });
});
