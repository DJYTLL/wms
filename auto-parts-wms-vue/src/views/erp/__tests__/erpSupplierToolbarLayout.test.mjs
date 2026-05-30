import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const srcRoot = join(viewsRoot, '..', '..');

const readView = (relativePath) => readFileSync(join(viewsRoot, relativePath), 'utf8');
const readSrc = (relativePath) => readFileSync(join(srcRoot, relativePath), 'utf8');

test('erp basic management toolbars keep actions fixed on the first-row right side', () => {
  const toolbarViews = readdirSync(viewsRoot)
    .filter(fileName => fileName.endsWith('.vue'))
    .filter(fileName => readView(fileName).includes('erp-basic-toolbar'));
  const tableStyleSource = readSrc('styles/table.css');

  assert.ok(toolbarViews.length > 0, 'expected at least one ERP basic toolbar view');

  for (const viewPath of toolbarViews) {
    const componentSource = readView(viewPath);
    const toolbarClasses = componentSource.match(/class="[^"]*\berp-basic-toolbar\b[^"]*"/g) ?? [];

    assert.ok(toolbarClasses.length > 0, `${viewPath} should contain ERP basic toolbar classes`);
    for (const toolbarClass of toolbarClasses) {
      const classTokens = toolbarClass.replace(/^class="/, '').replace(/"$/, '').split(/\s+/);
      assert.ok(classTokens.includes('erp-basic-toolbar--fixed-actions'), `${viewPath}: ${toolbarClass}`);
    }
  }

  assert.match(tableStyleSource, /\.erp-basic-toolbar--fixed-actions\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto;/);
  assert.match(tableStyleSource, /\.erp-basic-toolbar--fixed-actions\s+\.erp-basic-filters\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*wrap;/);
  assert.match(tableStyleSource, /\.erp-basic-toolbar--fixed-actions\s+\.erp-basic-filters\s+>\s+\*\s*\{[\s\S]*flex:\s*0 0 140px;[\s\S]*width:\s*140px;/);
  assert.match(tableStyleSource, /\.erp-basic-toolbar--fixed-actions\s+\.erp-basic-actions\s*\{[\s\S]*flex:\s*0 0 auto;[\s\S]*flex-wrap:\s*nowrap;[\s\S]*justify-content:\s*flex-end;/);
  assert.match(tableStyleSource, /@media \(max-width:\s*1280px\)\s*\{[\s\S]*\.erp-basic-toolbar--fixed-actions\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto;/);
});
