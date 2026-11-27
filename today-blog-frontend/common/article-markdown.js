import DOMPurify from 'dompurify';
import MarkdownIt from 'markdown-it';
import { full as emoji } from 'markdown-it-emoji';
import hljs from './markdown-highlight';

const markdown = new MarkdownIt({
  linkify: true,
  highlight(source, language) {
    if (language && hljs.getLanguage(language)) {
      return hljs.highlight(source, { language, ignoreIllegals: true }).value;
    }
    // Let markdown-it escape unknown or unlabelled code.
    return '';
  },
}).use(emoji);

const htmlPolicy = {
  USE_PROFILES: { html: true },
  ADD_ATTR: ['data-original', 'data-echo'],
  FORBID_TAGS: ['base', 'form', 'iframe', 'input', 'object', 'embed', 'style'],
  RETURN_DOM_FRAGMENT: true,
};

export function articleBaseUrl(siteHost, articleUri, fallbackOrigin = window.location.origin) {
  let origin = fallbackOrigin;
  if (siteHost) {
    try {
      const site = new URL(siteHost);
      if (site.protocol === 'http:' || site.protocol === 'https:') {
        origin = site.origin;
      }
    }
    catch {
      // An unset or invalid site URL uses the current public origin.
    }
  }
  return new URL(`/articles/${encodeURIComponent(articleUri || 'preview')}`, origin).href;
}

function normalizeUrl(element, attribute, baseUrl) {
  const value = element.getAttribute(attribute)?.trim();
  if (!value || (attribute === 'href' && value.startsWith('#'))) {
    return;
  }
  try {
    const url = new URL(value, baseUrl);
    if (url.protocol === 'http:' || url.protocol === 'https:'
        || (attribute === 'href' && ['mailto:', 'tel:'].includes(url.protocol))) {
      element.setAttribute(attribute, url.href);
      return;
    }
  }
  catch {
    // Invalid resource URLs are removed below.
  }
  element.removeAttribute(attribute);
}

function sanitizeHtml(html, baseUrl, fromMarkdown) {
  const fragment = DOMPurify.sanitize(html, htmlPolicy);
  for (const image of fragment.querySelectorAll('img')) {
    // Older article attachments store the image URL behind a loading placeholder.
    const original = image.getAttribute('data-original') || image.getAttribute('data-echo');
    if (original) {
      image.setAttribute('src', original);
      image.removeAttribute('data-original');
      image.removeAttribute('data-echo');
    }
    image.setAttribute('loading', 'lazy');
  }
  if (baseUrl) {
    for (const element of fragment.querySelectorAll('[href], [src], [poster]')) {
      for (const attribute of ['href', 'src', 'poster']) {
        normalizeUrl(element, attribute, baseUrl);
      }
    }
  }
  if (fromMarkdown) {
    for (const element of fragment.querySelectorAll('[style], [srcset]')) {
      const style = element.getAttribute('style');
      element.removeAttribute('srcset');
      if (!element.matches('th, td') || !/^text-align:\s*(left|center|right);?$/.test(style)) {
        element.removeAttribute('style');
      }
    }
  }
  for (const code of fragment.querySelectorAll('pre > code')) {
    code.classList.add('hljs');
  }
  const template = document.createElement('template');
  template.content.appendChild(fragment);
  return template.innerHTML;
}

export function renderMarkdown(source, { baseUrl = window.location.href, allowHtml = false } = {}) {
  // The parser options are only overridden for the synchronous render call.
  const previous = markdown.options.html;
  markdown.options.html = allowHtml;
  try {
    return sanitizeHtml(markdown.render(source ?? ''), baseUrl, true);
  }
  finally {
    markdown.options.html = previous;
  }
}

export function renderArticleMarkdown(source, baseUrl) {
  return renderMarkdown(source, { baseUrl, allowHtml: true });
}

export function renderArticleBody(article, baseUrl) {
  if (article.markdown?.trim()) {
    return renderArticleMarkdown(article.markdown, baseUrl);
  }
  return sanitizeHtml(article.content ?? '', baseUrl, false);
}
