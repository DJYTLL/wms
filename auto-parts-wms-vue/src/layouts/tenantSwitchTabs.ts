export interface TenantSwitchVisitedView {
  key?: string;
  title: string;
  path: string;
}

export interface ResetTabsForTenantSwitchOptions {
  visitedViews: TenantSwitchVisitedView[];
  viewKeyVersions: Record<string, number>;
  currentPath: string;
  homeView: TenantSwitchVisitedView;
  onClosePath?: (path: string) => void;
}

export const resetTabsForTenantSwitch = (_options: ResetTabsForTenantSwitchOptions) => {
  const { visitedViews, viewKeyVersions, currentPath, homeView, onClosePath } = _options;
  const oldPaths = visitedViews.map(view => view.path);
  oldPaths.forEach(path => {
    onClosePath?.(path);
  });

  const pathsToInvalidate = new Set([
    ...oldPaths,
    currentPath,
    homeView.path,
  ].filter(Boolean));
  pathsToInvalidate.forEach(path => {
    viewKeyVersions[path] = (viewKeyVersions[path] || 0) + 1;
  });

  visitedViews.splice(0, visitedViews.length, { ...homeView });
};
