import React from 'react';
import { readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { render } from '@testing-library/react';
import RssIcon from '../src/components/RssIcon';
import { toolbarIcon } from '../src/components/Editor/icons';

test('renders a fixed-size decorative RSS SVG', () => {
  const { container } = render(<RssIcon/>);
  const svg = container.querySelector('svg');
  expect(svg).toHaveAttribute('width', '1em');
  expect(svg).toHaveAttribute('height', '1em');
  expect(svg.closest('[aria-hidden="true"]')).not.toBeNull();
});

test.each([
  'bold', 'italic', 'strikethrough', 'heading', 'heading-smaller', 'heading-bigger',
  'heading-1', 'heading-2', 'heading-3', 'code', 'quote', 'unordered-list',
  'ordered-list', 'clean-block', 'link', 'image', 'upload-image', 'table',
  'horizontal-rule', 'preview', 'side-by-side', 'fullscreen', 'guide', 'undo', 'redo',
])('renders an inline SVG for the %s editor action', name => {
  const container = document.createElement('div');
  container.innerHTML = toolbarIcon(name);
  expect(container.querySelector('svg')).not.toBeNull();
  expect(container.querySelector('[aria-hidden="true"]')).not.toBeNull();
});

test('leaves unknown custom toolbar icons empty', () => {
  expect(toolbarIcon('custom')).toBe('');
});

test('has no Font Awesome dependency in desktop source or HTML', () => {
  function check(path) {
    for (const entry of readdirSync(path, { withFileTypes: true })) {
      const file = resolve(path, entry.name);
      if (entry.isDirectory()) check(file);
      else if (/\.(jsx?|css|less)$/.test(file)) {
        expect(readFileSync(file, 'utf8')).not.toMatch(/font-awesome|\bfa-[\w-]+/i);
      }
    }
  }
  check(resolve('src'));
  expect(readFileSync('index.html', 'utf8')).not.toMatch(/font-awesome/i);
});
