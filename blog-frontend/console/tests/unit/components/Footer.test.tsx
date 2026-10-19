import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server.node';
import Footer from '@/components/Footer';

jest.mock('@/components/hooks', () => ({
  useOptions: () => [{ 'site.copyright': 'Copyright © TODAY & 2017 - 2026 All Rights Reserved.' }],
}));

it('centers the footer without relying on asynchronously loaded ProLayout styles', () => {
  const container = document.createElement('div');
  container.innerHTML = renderToStaticMarkup(<Footer />);
  const footer = container.querySelector('footer')!;
  expect(footer.style.textAlign).toBe('center');
  expect(footer.textContent).toContain('由 Harry Yang 提供技术支持');
  expect(footer.textContent).toContain('Copyright © TODAY & 2017 - 2026 All Rights Reserved.');
});
