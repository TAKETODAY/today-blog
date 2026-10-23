jest.mock('@/utils', () => ({
  getSession: jest.fn(), getStorage: jest.fn(), saveSession: jest.fn(), saveStorage: jest.fn(),
}));

jest.mock('@/utils/article-i18n', () => ({ articleMessage: (key: string) => key }));

it('registers the overlay addon before the editor can be constructed', () => {
  jest.isolateModules(() => {
    const CodeMirror = require('codemirror');
    const Markdown = require('@/components/Editor/markdown').default;
    expect(typeof CodeMirror.overlayMode).toBe('function');
    expect(typeof Markdown).toBe('function');
  });
});
