const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { gzipSync } = require('node:zlib');

// Check the built dependency graph, not filenames or source import patterns.
const root = path.resolve(process.argv[2] || 'build');
const manifest = JSON.parse(fs.readFileSync(path.join(root, '.vite/manifest.json'), 'utf8'));
const visited = new Set();
function visit(key) {
  if (visited.has(key)) return;
  visited.add(key);
  for (const dependency of manifest[key].imports || []) visit(dependency);
}
visit('index.html');
if (manifest['src/pages/Home.jsx']) visit('src/pages/Home.jsx');

let js = 0;
let css = 0;
const styles = new Set();
for (const key of visited) {
  const chunk = manifest[key];
  const code = fs.readFileSync(path.join(root, chunk.file));
  assert.ok(!/CodeMirror|spell-checker/.test(code.toString()), '首页不应静态加载编辑器');
  js += gzipSync(code).length;
  for (const file of chunk.css || []) styles.add(file);
}
for (const file of styles) css += gzipSync(fs.readFileSync(path.join(root, file))).length;

console.log(`首页 gzip: JS ${(js / 1000).toFixed(2)} KB; CSS ${(css / 1000).toFixed(2)} KB`);
assert.ok(js < 350_000, '首页 JS gzip 不应超过 350 KB');
assert.ok(css < 40_000, '首页 CSS gzip 不应超过 40 KB');
assert.ok(!manifest['src/pages/Home.jsx']?.isDynamicEntry, '首页不应等待额外的路由分包');
console.log('首页资源预算检查通过');
