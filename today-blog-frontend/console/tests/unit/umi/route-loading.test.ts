import fs from 'fs';
import path from 'path';
import RouteLoading from '@/loading';
import InitialStateLoading from '@/components/InitialStateLoading';

describe('route loading', () => {
  it('reuses the initial-state loading component', () => {
    expect(RouteLoading).toBe(InitialStateLoading);
  });

  it('registers the loading component in the generated Umi renderer', () => {
    const source = fs.readFileSync(path.resolve(__dirname, '../../../src/.umi/umi.ts'), 'utf8');
    expect(source).toMatch(/import\s+Loading\s+from\s+['"][^'"]*\/loading(?:\.tsx)?['"]/);
    expect(source).toMatch(/loadingComponent:\s*Loading/);
  });
});
