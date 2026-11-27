import fs from 'fs';
import path from 'path';
import ts from 'typescript';

describe('Umi runtime registration', () => {
  it('accepts every runtime export in app.tsx', () => {
    const source = fs.readFileSync(path.resolve(__dirname, '../../../src/app.tsx'), 'utf8');
    const file = ts.createSourceFile('app.tsx', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
    const runtime: Record<string, unknown> = {};
    file.statements.forEach(statement => {
      if (!ts.canHaveModifiers(statement) || !ts.getModifiers(statement)?.some(modifier => modifier.kind === ts.SyntaxKind.ExportKeyword)) return;
      if (ts.isVariableStatement(statement)) {
        statement.declarationList.declarations.forEach(declaration => {
          if (ts.isIdentifier(declaration.name)) runtime[declaration.name.text] = {};
        });
      } else if (ts.isFunctionDeclaration(statement) && statement.name) {
        runtime[statement.name.text] = () => {};
      }
    });
    const generated = fs.readFileSync(path.resolve(__dirname, '../../../src/.umi/core/plugin.ts'), 'utf8');
    const keys = generated.match(/function getValidKeys\(\)\s*\{\s*return (\[[^\]]*\]);/);
    expect(keys).not.toBeNull();
    const { PluginManager } = require(require.resolve('umi/client/client/plugin.js', {
      paths: [path.dirname(require.resolve('@umijs/max'))],
    }));
    expect(() => PluginManager.create({
      plugins: [{ apply: runtime, path: 'src/app.tsx' }],
      validKeys: JSON.parse(keys![1].replace(/'/g, '"').replace(/,\]/g, ']')),
    })).not.toThrow();
  });
});
