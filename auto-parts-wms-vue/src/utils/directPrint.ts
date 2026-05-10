import { printHtml } from '@/utils/qzTray';

type DirectPrintOptions = {
  removeSelectors?: string[];
};

const buildPrintableHtml = (targetWindow: Window, options?: DirectPrintOptions) => {
  const doc = targetWindow.document;
  const clonedRoot = doc.documentElement.cloneNode(true) as HTMLElement;
  const selectors = options?.removeSelectors || [];

  selectors.forEach((selector) => {
    clonedRoot.querySelectorAll(selector).forEach((node) => node.remove());
  });

  return `<!DOCTYPE html>${clonedRoot.outerHTML}`;
};

export const directPrintWindow = async (targetWindow: Window | null | undefined, options?: DirectPrintOptions) => {
  if (!targetWindow?.document?.documentElement) return false;
  const html = buildPrintableHtml(targetWindow, options);
  return printHtml(html);
};
