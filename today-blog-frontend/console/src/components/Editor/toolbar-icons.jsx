import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import {
  BoldOutlined, ItalicOutlined, StrikethroughOutlined, FontSizeOutlined,
  CodeOutlined, MessageOutlined, UnorderedListOutlined, OrderedListOutlined,
  ClearOutlined, LinkOutlined, PictureOutlined, TableOutlined, MinusOutlined,
  EyeOutlined, LayoutOutlined, FullscreenOutlined, QuestionCircleOutlined,
  UndoOutlined, RedoOutlined, UploadOutlined, SaveOutlined,
} from '@ant-design/icons';

const icons = {
  bold: BoldOutlined,
  italic: ItalicOutlined,
  strikethrough: StrikethroughOutlined,
  heading: FontSizeOutlined,
  'heading-smaller': FontSizeOutlined,
  'heading-bigger': FontSizeOutlined,
  'heading-1': FontSizeOutlined,
  'heading-2': FontSizeOutlined,
  'heading-3': FontSizeOutlined,
  code: CodeOutlined,
  quote: MessageOutlined,
  'unordered-list': UnorderedListOutlined,
  'ordered-list': OrderedListOutlined,
  'clean-block': ClearOutlined,
  link: LinkOutlined,
  image: PictureOutlined,
  'upload-image': PictureOutlined,
  table: TableOutlined,
  'horizontal-rule': MinusOutlined,
  preview: EyeOutlined,
  'side-by-side': LayoutOutlined,
  fullscreen: FullscreenOutlined,
  guide: QuestionCircleOutlined,
  undo: UndoOutlined,
  redo: RedoOutlined,
  custom: UploadOutlined,
  updateArticle: SaveOutlined,
};

export function toolbarIcon(name) {
  const Icon = Object.prototype.hasOwnProperty.call(icons, name) && icons[name];
  return Icon ? renderToStaticMarkup(<Icon aria-hidden="true"/>) : '';
}
