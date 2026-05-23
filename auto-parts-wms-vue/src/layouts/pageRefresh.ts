const PAGE_REFRESH_TARGET_SELECTORS = [
  '.page-shell .page-header .table-actions',
  '.page-shell .page-header .erp-basic-actions',
] as const;

export const isListRefreshRoute = (path: string): boolean => {
  if (!path || path === '/login') {
    return false;
  }
  if (/^\/my(?:\/|$)/.test(path)) {
    return false;
  }
  if (/\/print(?:\/|$)/.test(path)) {
    return false;
  }
  if (/(?:\/create|\/edit|\/view)(?:\/|$)/.test(path)) {
    return false;
  }
  if (/\/\d+(?:\/|$)/.test(path)) {
    return false;
  }
  return true;
};

export const findPageRefreshTarget = (root: ParentNode | null): HTMLElement | null => {
  if (!root || typeof root.querySelector !== 'function') {
    return null;
  }
  for (const selector of PAGE_REFRESH_TARGET_SELECTORS) {
    const target = root.querySelector<HTMLElement>(selector);
    if (target) {
      return target;
    }
  }
  return null;
};

type MutationObserverLike = {
  observe(target: ParentNode, options: MutationObserverInit): void;
  disconnect(): void;
};

type MutationObserverCtorLike = new (callback: MutationCallback) => MutationObserverLike;

export const createPageRefreshTargetBinder = ({
  root,
  onTargetChange,
  MutationObserverCtor = typeof MutationObserver === 'function' ? MutationObserver : null,
}: {
  root: ParentNode | null;
  onTargetChange: (target: HTMLElement | null) => void;
  MutationObserverCtor?: MutationObserverCtorLike | null;
}) => {
  const sync = () => {
    onTargetChange(findPageRefreshTarget(root));
  };

  sync();

  const observer = MutationObserverCtor
    ? new MutationObserverCtor(() => {
        sync();
      })
    : null;

  if (observer && root) {
    observer.observe(root, { childList: true, subtree: true });
  }

  return {
    sync,
    dispose() {
      observer?.disconnect();
    }
  };
};
