import hljs from '../src/components/Editor/highlight';

test.each(['java', 'javascript', 'typescript', 'json', 'xml', 'sql', 'bash', 'yaml'])('%s grammar is available', language => {
  expect(hljs.getLanguage(language)).toBeTruthy();
});

test('highlights Java and preserves HTML escaping', () => {
  const result = hljs.highlight('java', 'public class Example { String value = "<script>"; }');
  expect(result.value).toContain('hljs-keyword');
  expect(result.value).toContain('&lt;script&gt;');
  expect(result.value).not.toContain('<script>');
});

test('does not register the full language catalogue', () => {
  expect(hljs.listLanguages()).toHaveLength(15);
  expect(hljs.getLanguage('brainfuck')).toBeUndefined();
});
