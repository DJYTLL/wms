import test from 'node:test';
import assert from 'node:assert/strict';

import { createPageRefreshTargetBinder, isListRefreshRoute } from '../pageRefresh.ts';

test('isListRefreshRoute only allows list-style routes', () => {
  assert.equal(isListRefreshRoute('/erp/sale-orders/draft'), true);
  assert.equal(isListRefreshRoute('/erp/stock-warnings'), true);
  assert.equal(isListRefreshRoute('/my/preferences'), false);
  assert.equal(isListRefreshRoute('/login'), false);
  assert.equal(isListRefreshRoute('/erp/sale-orders/draft/create'), false);
  assert.equal(isListRefreshRoute('/erp/sale-orders/approved/123'), false);
  assert.equal(isListRefreshRoute('/erp/sale-orders/123/print'), false);
});

test('binder picks up toolbar targets that appear after initial sync', () => {
  let currentTarget = null;
  const targetChanges = [];
  const seenSelectors = [];

  const root = {
    querySelector(selector) {
      seenSelectors.push(selector);
      return currentTarget;
    }
  };

  class FakeMutationObserver {
    static instances = [];

    constructor(callback) {
      this.callback = callback;
      this.observeArgs = null;
      this.disconnected = false;
      FakeMutationObserver.instances.push(this);
    }

    observe(target, options) {
      this.observeArgs = { target, options };
    }

    disconnect() {
      this.disconnected = true;
    }

    flush() {
      this.callback();
    }
  }

  const binder = createPageRefreshTargetBinder({
    root,
    MutationObserverCtor: FakeMutationObserver,
    onTargetChange: (target) => {
      targetChanges.push(target);
    }
  });

  assert.equal(FakeMutationObserver.instances.length, 1);
  assert.deepEqual(FakeMutationObserver.instances[0].observeArgs, {
    target: root,
    options: { childList: true, subtree: true }
  });
  assert.deepEqual(seenSelectors.slice(0, 2), [
    '.page-shell .page-header .table-actions',
    '.page-shell .page-header .erp-basic-actions',
  ]);
  assert.equal(targetChanges.at(-1), null);

  currentTarget = { id: 'late-toolbar' };
  FakeMutationObserver.instances[0].flush();

  assert.deepEqual(targetChanges.at(-1), currentTarget);

  binder.dispose();
  assert.equal(FakeMutationObserver.instances[0].disconnected, true);
});

test('binder supports basic master-data action containers', () => {
  const basicActionTarget = { id: 'erp-basic-actions' };
  const root = {
    querySelector(selector) {
      if (selector.includes('.table-actions')) {
        return null;
      }
      if (selector.includes('.erp-basic-actions')) {
        return basicActionTarget;
      }
      return null;
    }
  };

  const changes = [];
  const binder = createPageRefreshTargetBinder({
    root,
    MutationObserverCtor: null,
    onTargetChange: (target) => {
      changes.push(target);
    }
  });

  assert.equal(changes.at(-1), basicActionTarget);
  binder.dispose();
});
