// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: http://codemirror.net/LICENSE

const CodeMirror = require('codemirror');

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
    var spaces = Array(cm.options.tabSize + 1).join(' ');
    cm.replaceSelection(spaces);
  }
};
