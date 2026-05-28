const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');

const htmlPath = 'D:/project/.codex-tmp/supplier-toolbar-layout-prototype.html';
const pngPath = 'D:/project/.codex-tmp/supplier-toolbar-layout-prototype.png';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({
    viewport: { width: 1660, height: 820 },
    deviceScaleFactor: 1,
  });

  await page.goto(`file:///${htmlPath}`);
  await page.screenshot({ path: pngPath, fullPage: true });
  await browser.close();

  console.log(pngPath);
})();
