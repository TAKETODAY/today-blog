import path from 'path';
import type { IApi } from '@umijs/max';

export default function e2eTemp(api: IApi) {
  api.modifyPaths(paths => process.env.CONSOLE_E2E === '1'
    ? { ...paths, absTmpPath: path.join(paths.absSrcPath, '.umi-e2e') }
    : paths);
}
