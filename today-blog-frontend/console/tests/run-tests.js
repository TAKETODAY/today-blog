const net = require('net');
const { spawn } = require('child_process');

// 可连接已运行的独立测试服务；默认启动并且仅清理本脚本自己的服务。
async function main() {
  let server;
  let tests;
  const cleanup = () => {
    for (const child of [tests, server]) {
      if (child && child.exitCode === null) {
        if (process.platform === 'win32') child.kill();
        else {
          try { process.kill(-child.pid, 'SIGTERM'); } catch (error) {
            if (error.code !== 'ESRCH') throw error;
          }
        }
      }
    }
  };
  process.once('SIGINT', () => { cleanup(); process.exit(130); });
  process.once('SIGTERM', () => { cleanup(); process.exit(143); });
  try {
    const env = { ...process.env };
    if (!env.E2E_BASE_URL) {
      const port = await new Promise((resolve, reject) => {
        const probe = net.createServer();
        probe.on('error', reject);
        probe.listen(0, '127.0.0.1', () => {
          const address = probe.address();
          probe.close(() => resolve(address.port));
        });
      });
      env.E2E_BASE_URL = `http://localhost:${port}`;
      await new Promise((resolve, reject) => {
        server = spawn(process.execPath, [require.resolve('@umijs/max/bin/max'), 'dev'], {
          env: { ...env, CONSOLE_E2E: '1', PORT: String(port), BROWSER: 'none', MOCK: 'none', UMI_ENV: 'dev' },
          detached: process.platform !== 'win32',
          stdio: ['ignore', 'pipe', 'pipe'],
        });
        const timer = setTimeout(() => reject(new Error('E2E 开发服务启动超时')), 120000);
        let output = '';
        const onData = data => {
          process.stdout.write(data);
          output = (output + data).slice(-10000);
          if (/\[Webpack\] Compiled|Compiled successfully/.test(output)) {
            clearTimeout(timer);
            resolve();
          }
        };
        server.stdout.on('data', onData);
        server.stderr.on('data', onData);
        server.once('error', error => { clearTimeout(timer); reject(error); });
        server.once('exit', code => { clearTimeout(timer); reject(new Error(`开发服务退出：${code}`)); });
      });
    }
    process.exitCode = await new Promise((resolve, reject) => {
      tests = spawn(process.execPath, [require.resolve('jest/bin/jest'),
        '--config', 'jest.e2e.config.js', '--runInBand'], {
        env, stdio: 'inherit', detached: process.platform !== 'win32',
      });
      tests.once('error', reject);
      tests.once('exit', code => resolve(code === null ? 1 : code));
    });
  }
  finally { cleanup(); }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
