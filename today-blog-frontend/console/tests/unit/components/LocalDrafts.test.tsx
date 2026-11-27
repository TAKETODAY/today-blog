import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { ArticleDraft, deleteDraft, draftUrl, importLegacyDrafts, listDrafts, readDraft, saveDraft } from '@/pages/articles/drafts/storage';
import { useDraft } from '@/pages/articles/drafts/useDraft';

describe('local article drafts', () => {
  let root: ReturnType<typeof createRoot>;
  let session: ReturnType<typeof useDraft>;
  let data: Map<string, string>;
  const actEnvironment = globalThis.IS_REACT_ACT_ENVIRONMENT;

  beforeAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  });
  afterAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = actEnvironment;
  });
  beforeEach(() => {
    jest.useFakeTimers();
    jest.restoreAllMocks();
    data = new Map();
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        get length() {
          return data.size;
        },
        key: (index: number) => Array.from(data.keys())[index] ?? null,
        getItem: jest.fn((key: string) => data.get(key) ?? null),
        setItem: jest.fn((key: string, value: string) => data.set(key, value)),
        removeItem: jest.fn((key: string) => data.delete(key)),
      },
    });
    root = createRoot(document.createElement('div'));
  });
  afterEach(() => {
    act(() => root.unmount());
    jest.useRealTimers();
  });

  function mount(id: string, articleId?: string, userId = 1) {
    function Editor() {
      session = useDraft(userId, id, 'markdown', articleId);
      return null;
    }
    act(() => root.render(<Editor/>));
  }

  const post = {
    title: 'Article', markdown: '# Heading', content: '<h1>Heading</h1>', summary: '', category: 'tech',
    labels: ['one', 'two'], cover: '/image.jpg', uri: 'article', copyright: 'original', status: 'DRAFT',
    createAt: '2026-11-27T10:00:00.000Z',
  };

  function record(id: string, savedAt: number, articleId?: string): ArticleDraft {
    return { version: 1, id, mode: 'markdown', savedAt, articleId, post };
  }

  it('debounces and merges changes, then restores all fields', () => {
    mount('one');
    act(() => session.change({ title: post.title }));
    act(() => jest.advanceTimersByTime(400));
    act(() => session.change({ ...post, title: 'Changed' }));
    expect(session.status).toBe('pending');
    expect(readDraft(1, 'one')).toBeUndefined();
    act(() => jest.advanceTimersByTime(600));
    expect(session.status).toBe('saved');
    expect(session.savedAt).toBeDefined();
    mount('one');
    expect(session.restored).toBe(true);
    expect(session.post).toEqual({ ...post, title: 'Changed' });
  });

  it('flushes immediately on leaving and keeps other drafts separate', () => {
    mount('one');
    act(() => session.change(post));
    mount('two', '42');
    expect(readDraft(1, 'one')?.post).toEqual(post);
    act(() => session.change({ title: 'Existing article' }));
    act(() => window.dispatchEvent(new Event('pagehide')));
    expect(readDraft(1, 'two')?.articleId).toBe('42');
    expect(draftUrl(readDraft(1, 'two'))).toBe('/articles/42/modify?draft=two');
    expect(readDraft(2, 'one')).toBeUndefined();
    expect(listDrafts(1).drafts).toHaveLength(2);
  });

  it('sorts drafts and removes only the submitted or deleted record', () => {
    saveDraft(1, record('old', 10));
    saveDraft(1, record('new', 20, '42'));
    saveDraft(2, record('new', 30));
    expect(listDrafts(1).drafts.map(draft => draft.id)).toEqual(['new', 'old']);
    mount('new', '42');
    let submitted;
    act(() => {
      submitted = session.change({ title: 'Submitted' });
    });
    act(() => session.flush());
    act(() => expect(session.complete(submitted)).toBe(true));
    act(() => jest.runOnlyPendingTimers());
    expect(readDraft(1, 'new')).toBeUndefined();
    expect(readDraft(2, 'new')).toBeDefined();
    deleteDraft(1, 'old');
    expect(listDrafts(1).drafts).toEqual([]);
  });

  it('does not overwrite corrupt, future-version, or mismatched records', () => {
    data.set('today-blog:article-drafts:1:broken', '{');
    data.set('today-blog:article-drafts:1:future', JSON.stringify({ ...record('future', 10), version: 2 }));
    saveDraft(1, record('existing', 10, '42'));
    expect(listDrafts(1)).toMatchObject({ damaged: true, drafts: [record('existing', 10, '42')] });
    mount('existing', '43');
    expect(session.status).toBe('error');
    act(() => session.change({ title: 'Wrong article' }));
    act(() => jest.advanceTimersByTime(600));
    expect(session.status).toBe('error');
    expect(readDraft(1, 'existing')?.post).toEqual(post);
    mount('broken');
    expect(session.status).toBe('error');
  });

  it('keeps editing when storage is unavailable or full', () => {
    (localStorage.getItem as jest.Mock).mockImplementationOnce(() => {
      throw new Error('unavailable');
    });
    mount('unavailable');
    expect(session.status).toBe('error');
    act(() => session.change(post));
    expect(session.post).toEqual(post);
    mount('full');
    (localStorage.setItem as jest.Mock).mockImplementationOnce(() => {
      throw new Error('quota');
    });
    act(() => session.change(post));
    act(() => jest.advanceTimersByTime(600));
    expect(session.status).toBe('error');
    act(() => session.change({ title: 'retry' }));
    act(() => jest.advanceTimersByTime(600));
    expect(session.status).toBe('saved');
  });

  it('does not recreate a deleted draft from a stale editor', () => {
    saveDraft(1, record('one', 10));
    mount('one');
    deleteDraft(1, 'one');
    act(() => session.change({ title: 'Stale' }));
    act(() => jest.advanceTimersByTime(600));
    expect(session.status).toBe('error');
    expect(readDraft(1, 'one')).toBeUndefined();
  });

  it('retains changes made while a server submission is pending', () => {
    mount('one');
    let submitted;
    act(() => {
      submitted = session.change(post);
    });
    act(() => session.flush());
    act(() => session.change({ title: 'Newer edit' }));
    act(() => expect(session.complete(submitted)).toBe(false));
    act(() => jest.advanceTimersByTime(600));
    expect(readDraft(1, 'one')?.post.title).toBe('Newer edit');
  });

  it('imports legacy drafts without discarding invalid data', () => {
    data.set('article_write_md', JSON.stringify(post));
    data.set('article_modify_rich_42', JSON.stringify(post));
    data.set('article_write_rich', '{');
    expect(importLegacyDrafts(1)).toBe(false);
    expect(listDrafts(1).drafts).toHaveLength(2);
    expect(readDraft(1, 'legacy-article_modify_rich_42')).toMatchObject({ mode: 'richText', articleId: '42', post });
    expect(data.has('article_write_md')).toBe(false);
    expect(data.has('article_write_rich')).toBe(true);
  });
});
