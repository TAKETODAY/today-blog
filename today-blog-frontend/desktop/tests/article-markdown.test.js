import { renderArticleBody, renderArticleMarkdown } from 'core/article-markdown';

const baseUrl = 'https://blog.example/articles/markdown-example';

test('renders the shared Markdown rules for a complex article', () => {
  const html = renderArticleMarkdown([
    '# Title',
    '',
    'A **bold** paragraph with [a link](../docs) and an image:',
    '',
    '![cover](images/cover.png)',
    '',
    '- one',
    '  - nested',
    '',
    '| Name | Value |',
    '| --- | ---: |',
    '| count | 2 |',
    '',
    '```javascript',
    'const value = "<safe>";',
    '```',
  ].join('\n'), baseUrl);

  expect(html).toContain('<h1>Title</h1>');
  expect(html).toContain('<strong>bold</strong>');
  expect(html).toContain('href="https://blog.example/docs"');
  expect(html).toContain('src="https://blog.example/articles/images/cover.png"');
  expect(html).toContain('<table>');
  expect(html).toContain('text-align:right');
  expect(html).toContain('class="language-javascript hljs"');
  expect(html).toContain('&lt;safe&gt;');
});

test('sanitizes raw HTML and unsafe resource URLs', () => {
  const html = renderArticleMarkdown([
    '<script>alert("xss")</script>',
    '<img src="javascript:alert(1)" onerror="alert(2)">',
    '<a href="javascript:alert(3)">bad</a>',
  ].join('\n'), baseUrl);

  expect(html).not.toContain('<script>');
  expect(html).not.toContain('onerror');
  expect(html).not.toContain('javascript:');
  expect(html).toContain('>bad</a>');
});

test('restores legacy lazy-loaded images when displaying saved HTML', () => {
  const html = renderArticleBody({
    content: '<p><img src="/assets/images/loading.gif" data-original="/uploads/cover.png"></p>',
    markdown: null,
  }, baseUrl);

  expect(html).toContain('src="https://blog.example/uploads/cover.png"');
  expect(html).not.toContain('data-original');
  expect(html).not.toContain('loading.gif');
});

test('prefers Markdown over the stored HTML for new article rendering', () => {
  const html = renderArticleBody({
    content: '<p>old content</p>',
    markdown: '**current content**',
  }, baseUrl);

  expect(html).toContain('<strong>current content</strong>');
  expect(html).not.toContain('old content');
});

test('preserves CommonMark line breaks, inline formatting, lists and extensions', () => {
  const source = [
    'First line',
    'second line  ',
    'third line',
    '',
    '> *italic* and ~~deleted~~ with `code`',
    '',
    '1. first',
    '2. second',
    '',
    '---',
    '',
    ':smile: https://example.org',
  ].join('\n');
  const html = renderArticleMarkdown(source, baseUrl);
  expect(html).toContain('First line\nsecond line<br>\nthird line');
  expect(html).toContain('<blockquote>');
  expect(html).toContain('<em>italic</em>');
  expect(html).toContain('<s>deleted</s>');
  expect(html).toContain('<code>code</code>');
  expect(html).toContain('<ol>');
  expect(html).toContain('<hr>');
  expect(html).toContain('😄');
  expect(html).toContain('href="https://example.org/"');
  expect(renderArticleBody({ markdown: source, content: html }, baseUrl)).toBe(html);
});
