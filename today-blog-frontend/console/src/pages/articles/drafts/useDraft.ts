import { useEffect, useRef, useState } from 'react';
import { ArticleDraft, deleteDraft, DraftPost, EditorMode, readDraft, saveDraft } from './storage';

export function useDraft(userId: number, id: string, mode: EditorMode, articleId?: string) {
  const [initial] = useState(() => {
    try {
      const draft = readDraft(userId, id);
      if (draft && (draft.articleId !== articleId || draft.mode !== mode)) {
        throw new Error('Draft belongs to another editor');
      }
      return { draft, error: false };
    }
    catch {
      return { draft: undefined, error: true };
    }
  });
  const [post, setPost] = useState<DraftPost>(initial.draft?.post || {});
  const latest = useRef(post);
  const pending = useRef(false);
  const timer = useRef<ReturnType<typeof setTimeout>>();
  const [status, setStatus] = useState(initial.error ? 'error' : initial.draft ? 'saved' : 'idle');
  const [savedAt, setSavedAt] = useState(initial.draft?.savedAt);
  const storedAt = useRef(initial.draft?.savedAt);

  function flush(notify = true) {
    clearTimeout(timer.current);
    if (!pending.current) {
      return true;
    }
    try {
      // Never overwrite an unreadable or incompatible record.
      if (initial.error) {
        throw new Error('Cannot restore draft');
      }
      if (readDraft(userId, id)?.savedAt !== storedAt.current) {
        throw new Error('Draft changed in another editor');
      }
      const draft: ArticleDraft = {
        version: 1, id, articleId, mode, post: latest.current,
        savedAt: Math.max(Date.now(), (storedAt.current ?? 0) + 1),
      };
      saveDraft(userId, draft);
      storedAt.current = draft.savedAt;
      pending.current = false;
      if (notify) {
        setSavedAt(draft.savedAt);
        setStatus('saved');
      }
      return true;
    }
    catch {
      if (notify) {
        setStatus('error');
      }
      return false;
    }
  }

  function initialize(value: DraftPost) {
    latest.current = value;
    setPost(value);
  }

  function change(fields: DraftPost) {
    const next = { ...latest.current, ...fields };
    initialize(next);
    pending.current = true;
    setStatus('pending');
    clearTimeout(timer.current);
    timer.current = setTimeout(() => flush(), 600);
    return next;
  }

  function discard() {
    clearTimeout(timer.current);
    pending.current = false;
  }

  function complete(submitted: DraftPost) {
    if (latest.current !== submitted) {
      return false;
    }
    discard();
    try {
      if (initial.error) {
        setStatus('error');
        return false;
      }
      if (readDraft(userId, id)?.savedAt !== storedAt.current) {
        setStatus('error');
        return false;
      }
      deleteDraft(userId, id);
      return true;
    }
    catch {
      setStatus('error');
      return false;
    }
  }

  useEffect(() => {
    const onPageHide = () => flush(false);
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!flush()) {
        event.preventDefault();
        event.returnValue = '';
      }
    };
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') {
        flush();
      }
    };
    window.addEventListener('pagehide', onPageHide);
    window.addEventListener('beforeunload', onBeforeUnload);
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      window.removeEventListener('pagehide', onPageHide);
      window.removeEventListener('beforeunload', onBeforeUnload);
      document.removeEventListener('visibilitychange', onVisibility);
      flush(false);
    };
  }, []);

  return { post, restored: Boolean(initial.draft), initialize, change, flush, complete, discard, status, savedAt };
}
