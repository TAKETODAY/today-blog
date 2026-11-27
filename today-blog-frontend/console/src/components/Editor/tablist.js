/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import CodeMirror from 'codemirror';

CodeMirror.commands.tabAndIndentMarkdownList = function (cm) {
  const ranges = cm.listSelections();
  const pos = ranges[0].head;
  const eolState = cm.getStateAfter(pos.line);
  const inList = eolState.list !== false;

  if (inList) {
    cm.execCommand('indentMore');
    return;
  }

  if (cm.options.indentWithTabs) {
    cm.execCommand('insertTab');
  }
  else {
    const spaces = Array(cm.options.tabSize + 1).join(' ');
    cm.replaceSelection(spaces);
  }
};

CodeMirror.commands.shiftTabAndUnindentMarkdownList = function (cm) {
  const ranges = cm.listSelections();
  const pos = ranges[0].head;
  const eolState = cm.getStateAfter(pos.line);
  const inList = eolState.list !== false;

  if (inList) {
    cm.execCommand('indentLess');
    return;
  }

  if (cm.options.indentWithTabs) {
    cm.execCommand('insertTab');
  }
  else {
    const spaces = Array(cm.options.tabSize + 1).join(' ');
    cm.replaceSelection(spaces);
  }
};
