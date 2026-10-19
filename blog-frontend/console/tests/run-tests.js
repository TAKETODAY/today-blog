/* eslint-disable eslint-comments/disable-enable-pair */
/* eslint-disable @typescript-eslint/no-var-requires */
/* eslint-disable eslint-comments/no-unlimited-disable */
const { spawn } = require('child_process');
// eslint-disable-next-line import/no-extraneous-dependencies
const { kill } = require('cross-port-killer');

const env = Object.create(process.env);
env.BROWSER = 'none';
env.TEST = true;
env.UMI_UI = 'none';
env.PROGRESS = 'none';
// flag to prevent multiple test
let once = false;

const startServer = spawn(/^win/.test(process.platform) ? 'npm.cmd' : 'npm', ['start'], {
  env,
});

startServer.stderr.on('data', (data) => {
  // eslint-disable-next-line
  console.log(data.toString());
});

startServer.on('exit', () => {
  kill(process.env.PORT || 8000);
});

console.log('Starting development server for e2e tests...');
startServer.stdout.on('data', (data) => {
  console.log(data.toString());
  // hack code , wait umi
  if (
    !once && /Compiled successfully|\[Webpack\] Compiled/.test(data.toString())
  ) {
    // eslint-disable-next-line
    once = true;
    console.log('Development server is started, ready to run tests.');
    const testCmd = spawn(
      /^win/.test(process.platform) ? 'npm.cmd' : 'npm',
       ['test', '--', '--config', 'jest.e2e.config.js', '--runInBand'],
      {
        stdio: 'inherit',
      },
    );
    testCmd.on('exit', (code) => {
      startServer.kill();
      process.exit(code);
    });
  }
});
