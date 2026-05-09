import { ElMessage, ElMessageBox } from 'element-plus';

const DEFAULT_QZ_TRAY_URL = 'https://unpkg.com/qz-tray@2.2.4/qz-tray.js';
export const QZ_TRAY_DOWNLOAD_URL = 'https://qz.io/download/';

type QzInstance = any;
let downloadPromptOpen = false;

const showDownloadPrompt = () => {
  if (downloadPromptOpen) return;
  downloadPromptOpen = true;
  ElMessageBox.confirm(
    `未检测到 QZ Tray 服务，请先安装并启动。<br/>` +
      `下载地址：<a href="${QZ_TRAY_DOWNLOAD_URL}" target="_blank" rel="noopener">QZ Tray</a>`,
    '本地打印不可用',
    {
      confirmButtonText: '打开下载页',
      cancelButtonText: '知道了',
      dangerouslyUseHTMLString: true
    }
  )
    .then(() => {
      window.open(QZ_TRAY_DOWNLOAD_URL, '_blank', 'noopener');
    })
    .finally(() => {
      downloadPromptOpen = false;
    });
};

const loadScript = (src: string) =>
  new Promise<void>((resolve, reject) => {
    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      if ((existing as HTMLScriptElement).dataset.loaded === 'true') {
        resolve();
      } else {
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', () => reject(new Error('QZ Tray script load failed')));
      }
      return;
    }
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.dataset.loaded = 'false';
    script.onload = () => {
      script.dataset.loaded = 'true';
      resolve();
    };
    script.onerror = () => reject(new Error('QZ Tray script load failed'));
    document.head.appendChild(script);
  });

export const ensureQz = async (): Promise<QzInstance | null> => {
  if (typeof window === 'undefined') return null;
  if ((window as any).qz) return (window as any).qz;
  const url = (import.meta as any).env?.VITE_QZ_TRAY_URL || DEFAULT_QZ_TRAY_URL;
  try {
    await loadScript(url);
  } catch (error) {
    ElMessage.error('QZ Tray 脚本加载失败，请检查网络或本地文件');
    return null;
  }
  return (window as any).qz || null;
};

const prepareSecurity = (qz: QzInstance) => {
  if (!qz?.security) return;
  if (!qz.security.setSignaturePromise) return;
  qz.security.setSignaturePromise(() => (resolve: (value: string) => void) => resolve(''));
  if (qz.security.setCertificatePromise) {
    qz.security.setCertificatePromise((resolve: (value: string) => void) => resolve(''));
  }
};

export const connectQz = async (): Promise<QzInstance | null> => {
  const qz = await ensureQz();
  if (!qz) return null;
  try {
    prepareSecurity(qz);
    if (!qz.websocket?.isActive?.()) {
      await qz.websocket.connect();
    }
    return qz;
  } catch (error) {
    ElMessage.error('未连接到 QZ Tray，请确认已安装并运行');
    showDownloadPrompt();
    return null;
  }
};

export const printHtml = async (html: string, options?: { printer?: string }) => {
  const qz = await connectQz();
  if (!qz) return false;
  try {
    const printer = options?.printer || (await qz.printers.getDefault());
    if (!printer) {
      ElMessage.error('未找到默认打印机');
      return false;
    }
    const config = qz.configs.create(printer);
    const data = [
      {
        type: 'html',
        format: 'plain',
        data: html
      }
    ];
    await qz.print(config, data);
    return true;
  } catch (error) {
    ElMessage.error('QZ Tray 打印失败，请检查打印机或授权设置');
    return false;
  }
};
