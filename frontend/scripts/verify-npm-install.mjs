import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const marker = path.join(
  root,
  'node_modules',
  'ajv',
  'dist',
  'vocabularies',
  'applicator',
  'index.js',
);

if (!fs.existsSync(marker)) {
  console.error('');
  console.error(
    '[frontend] Instalação npm incompleta (falta parte do pacote ajv — ex.: cloud sync, antivirus ou npm interrompido).',
  );
  console.error('Corrija na pasta frontend:');
  console.error('  rm -rf node_modules');
  console.error('  npm ci');
  console.error('  (ou: npm install)');
  console.error('');
  process.exit(1);
}
