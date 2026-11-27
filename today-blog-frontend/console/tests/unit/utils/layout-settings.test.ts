import { loadLayoutSettings, normalizeLayoutSettings, saveLayoutSettings } from '@/utils/layout-settings';

describe('layout preferences', () => {
  it('keeps the brand color fixed and ignores unrelated settings', () => {
    expect(normalizeLayoutSettings({ primaryColor: '#ff0000', title: 'other' })).toMatchObject({
      primaryColor: '#337ab7', layout: 'side',
    });
    expect(normalizeLayoutSettings({ title: 'other' })).not.toHaveProperty('title');
  });

  it('persists preferences and restores them on reload', () => {
    saveLayoutSettings({ layout: 'mix', fixedHeader: false });
    expect(loadLayoutSettings()).toMatchObject({ layout: 'mix', fixedHeader: false, primaryColor: '#337ab7' });
  });

  it('falls back safely when storage is corrupt or unavailable', () => {
    localStorage.setItem('today-blog-console-layout', '{bad');
    expect(loadLayoutSettings().layout).toBe('side');
    const spy = jest.spyOn(Storage.prototype, 'getItem').mockImplementationOnce(() => { throw new Error('blocked'); });
    expect(loadLayoutSettings().primaryColor).toBe('#337ab7');
    spy.mockRestore();
  });
});
