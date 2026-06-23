import { spawnSync } from 'node:child_process';
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

const root = dirname(fileURLToPath(import.meta.url));
const frontendRoot = dirname(root);
const outDir = join(frontendRoot, '.tmp-websocket-tests');

const files = [
  ['src/services/webSocketClient.ts', 'webSocketClient.js'],
  ['src/services/webSocketClient.test.ts', 'webSocketClient.test.js'],
];

await rm(outDir, { force: true, recursive: true });
await mkdir(outDir, { recursive: true });

for (const [sourcePath, outputName] of files) {
  const source = await readFile(join(frontendRoot, sourcePath), 'utf8');
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.ES2022,
      target: ts.ScriptTarget.ES2022,
    },
    fileName: sourcePath,
    reportDiagnostics: true,
  });

  if (transpiled.diagnostics?.length) {
    for (const diagnostic of transpiled.diagnostics) {
      console.error(ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n'));
    }
    process.exit(1);
  }

  await writeFile(join(outDir, outputName), transpiled.outputText);
}

const result = spawnSync(process.execPath, ['--test', join(outDir, 'webSocketClient.test.js')], {
  cwd: frontendRoot,
  stdio: 'inherit',
});

process.exit(result.status ?? 1);
