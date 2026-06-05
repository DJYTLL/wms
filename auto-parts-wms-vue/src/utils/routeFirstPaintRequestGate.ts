let currentGate: Promise<void> | null = null;
let currentGatePath = '';

const waitForBrowserPaint = () => new Promise<void>((resolve) => {
  if (typeof window === 'undefined' || typeof window.requestAnimationFrame !== 'function') {
    setTimeout(resolve, 0);
    return;
  }

  let settled = false;
  const finish = () => {
    if (settled) return;
    settled = true;
    resolve();
  };

  const fallback = window.setTimeout(finish, 160);
  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(() => {
      window.clearTimeout(fallback);
      finish();
    });
  });
});

export const openRouteFirstPaintRequestGate = (path: string) => {
  currentGatePath = path;
  const gatePath = path;
  currentGate = waitForBrowserPaint().finally(() => {
    if (currentGatePath === gatePath) {
      currentGate = null;
    }
  });
};

export const waitForRouteFirstPaintRequestGate = async () => {
  if (!currentGate) {
    return;
  }
  await currentGate;
};
