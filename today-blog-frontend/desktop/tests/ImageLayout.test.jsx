import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import Image from '../src/components/Image';
import Footer from '../src/components/Footer';

const store = createStore((state = { options: { 'site.cdn': '' } }) => state);

test('does not impose fixed dimensions on covers with unknown sizes', () => {
  render(<Provider store={store}>
    <Image src="/cover.png" alt="封面" original={false}/>
  </Provider>);
  const image = screen.getByAltText('封面');
  const wrapper = image.closest('.ant-image');
  expect(wrapper.style.width).toBe('');
  expect(wrapper.style.height).toBe('');
  expect(image).not.toHaveAttribute('width');
  expect(image).not.toHaveAttribute('height');
  fireEvent.error(image);
  expect(image.closest('.ant-image')).toBe(wrapper);
  expect(image).not.toHaveAttribute('width');
  expect(image).not.toHaveAttribute('height');
});

test('reserves both dimensions for every footer share icon', () => {
  const { container } = render(<Provider store={store}><Footer/></Provider>);
  const images = container.querySelectorAll('img.share');
  expect(images).toHaveLength(3);
  images.forEach(image => {
    expect(Number(image.getAttribute('width'))).toBeGreaterThan(0);
    expect(Number(image.getAttribute('height'))).toBeGreaterThan(0);
  });
});
