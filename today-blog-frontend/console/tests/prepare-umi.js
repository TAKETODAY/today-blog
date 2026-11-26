const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const root = path.resolve(__dirname, '..');
const generated = ['core/plugin.ts', 'core/route.tsx', 'exports.ts'];

// 正在运行的开发服务器也会维护 .umi；已有文件时避免 setup 清理并重建它。
if (!generated.every(file => fs.existsSync(path.join(root, 'src/.umi', file)))) {
  execFileSync(process.platform === 'win32' ? 'max.cmd' : 'max', ['setup'], {
    cwd: root,
    stdio: 'inherit',
  });
}
