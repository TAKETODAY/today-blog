export type EditorMode = 'markdown' | 'richText';

export interface DraftPost {
  title?: string;
  markdown?: string;
  content?: string;
  summary?: string;
  category?: string;
  labels?: (string | { name: string })[];
  cover?: string;
  uri?: string;
  copyright?: string;
  status?: string;
  createAt?: string | null;
  password?: string;
}

export interface ArticleDraft {
  version: 1;
  id: string;
  articleId?: string;
  mode: EditorMode;
  savedAt: number;
  post: DraftPost;
}

const prefix = (userId: number) => `today-blog:article-drafts:${userId}:`;
const key = (userId: number, id: string) => prefix(userId) + id;
const textFields = ['title', 'markdown', 'content', 'summary', 'category', 'cover', 'uri', 'copyright', 'status', 'password', 'createAt'] as const;

export function draftPost(value: DraftPost): DraftPost {
  const post: DraftPost = {};
  for (const field of textFields) {
    if (value[field] !== undefined) {
      post[field] = value[field];
    }
  }
  if (value.labels !== undefined) {
    post.labels = value.labels?.map(label => typeof label === 'string' ? label : label.name);
  }
  return post;
}

export function newDraftId() {
  return Array.from(crypto.getRandomValues(new Uint32Array(4)), value => value.toString(16).padStart(8, '0')).join('');
}

function decode(raw: string, id: string): ArticleDraft {
  const draft = JSON.parse(raw);
  if (!draft || draft.version !== 1 || draft.id !== id || !Number.isFinite(draft.savedAt)
      || !['markdown', 'richText'].includes(draft.mode) || !draft.post || typeof draft.post !== 'object'
      || Array.isArray(draft.post) || (draft.articleId !== undefined && typeof draft.articleId !== 'string')) {
    throw new Error('Invalid or unsupported article draft');
  }
  for (const field of textFields) {
    if (draft.post[field] != null && typeof draft.post[field] !== 'string') {
      throw new Error('Invalid draft field');
    }
  }
  if (draft.post.labels != null && (!Array.isArray(draft.post.labels) || !draft.post.labels.every(label =>
    typeof label === 'string' || (label && typeof label.name === 'string')))) {
    throw new Error('Invalid draft labels');
  }
  return { ...draft, post: draftPost(draft.post) };
}

export function readDraft(userId: number, id: string): ArticleDraft | undefined {
  const raw = localStorage.getItem(key(userId, id));
  return raw === null ? undefined : decode(raw, id);
}

export function saveDraft(userId: number, draft: ArticleDraft) {
  localStorage.setItem(key(userId, draft.id), JSON.stringify(draft));
}

export function deleteDraft(userId: number, id: string) {
  localStorage.removeItem(key(userId, id));
}

// Individual records also serve as the index, avoiding partially written indexes and cross-tab lost updates.
export function listDrafts(userId: number) {
  const drafts: ArticleDraft[] = [];
  let damaged = false;
  for (let i = 0; i < localStorage.length; i++) {
    const name = localStorage.key(i);
    if (name?.startsWith(prefix(userId))) {
      try {
        const draft = readDraft(userId, name.slice(prefix(userId).length));
        if (draft) {
          drafts.push(draft);
        }
      }
      catch {
        damaged = true;
      }
    }
  }
  return { drafts: drafts.sort((a, b) => b.savedAt - a.savedAt), damaged };
}

export function draftUrl(draft: Pick<ArticleDraft, 'id' | 'articleId' | 'mode'>) {
  const base = draft.articleId ? `/articles/${encodeURIComponent(draft.articleId)}/modify` : '/articles/write';
  return `${base}${draft.mode === 'richText' ? '-rich-text' : ''}?draft=${encodeURIComponent(draft.id)}`;
}

// Old caches had no account identifier. Import only after explicit confirmation.
export function importLegacyDrafts(userId: number) {
  const keys: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const name = localStorage.key(i);
    if (name && /^article_(write_(md|rich)|modify_(md|rich)_\d+)$/.test(name)) {
      keys.push(name);
    }
  }
  let failed = false;
  for (const name of keys) {
    try {
      const post = JSON.parse(localStorage.getItem(name));
      const id = `legacy-${name}`;
      const draft: ArticleDraft = {
        version: 1, id, savedAt: Date.now(), post,
        mode: name.includes('_rich') ? 'richText' : 'markdown',
        articleId: name.startsWith('article_modify_') ? name.split('_').pop() : undefined,
      };
      draft.post = decode(JSON.stringify(draft), id).post;
      const existing = readDraft(userId, id);
      if (!existing) {
        saveDraft(userId, draft);
      }
      else if (JSON.stringify(existing.post) !== JSON.stringify(draft.post)) {
        throw new Error('Legacy draft already imported and changed');
      }
      localStorage.removeItem(name);
    }
    catch {
      failed = true;
    }
  }
  return !failed;
}
