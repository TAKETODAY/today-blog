jest.mock('@/utils', () => ({
  getSession: jest.fn(), getStorage: jest.fn(), saveSession: jest.fn(), saveStorage: jest.fn(),
}));

it('registers the overlay addon before the editor can be constructed', () => {
  jest.isolateModules(() => {
    const CodeMirror = require('codemirror');
    const Markdown = require('@/components/Editor/markdown').default;
    expect(typeof CodeMirror.overlayMode).toBe('function');
    expect(typeof Markdown).toBe('function');
  });
});
