import type { ErpDataTableColumn } from '@/components/ErpDataTable.vue';

type Translate = (key: string) => string;
type SaleListWorkspace = 'draft' | 'approved';

const indexColumn = (t: Translate): ErpDataTableColumn => ({
  key: 'index',
  label: t('table.index'),
  width: 70,
  minWidth: 56,
  resizable: false,
  configurable: false
});

const actionsColumn = (t: Translate): ErpDataTableColumn => ({
  key: 'actions',
  label: t('table.actions'),
  width: 300,
  minWidth: 180,
  stickyRight: true,
  resizable: false,
  configurable: false
});

export const resolveSaleOrderRouteFrameTableKey = (workspace: SaleListWorkspace) => (
  workspace === 'approved' ? 'erp-sale-approved' : 'erp-sale-draft'
);

export const resolveSaleReturnRouteFrameTableKey = () => 'erp-sale-return-management';

export const createSaleOrderRouteFrameColumns = (
  t: Translate,
  workspace: SaleListWorkspace
): ErpDataTableColumn[] => {
  const columns: ErpDataTableColumn[] = [
    indexColumn(t),
    {
      key: 'orderNo',
      label: t('field.orderNo'),
      prop: 'orderNo',
      width: 160,
      minWidth: 56
    },
    {
      key: 'customer',
      label: t('field.customer'),
      width: 160,
      minWidth: 56
    }
  ];

  if (workspace === 'approved') {
    columns.push({
      key: 'status',
      label: t('field.status'),
      width: 120,
      minWidth: 56
    });
  }

  columns.push(
    {
      key: 'totalAmount',
      label: t('field.totalAmount'),
      prop: 'totalAmount',
      width: 140,
      minWidth: 56
    },
    {
      key: 'netSaleAmount',
      label: t('field.netSaleAmount'),
      width: 140,
      minWidth: 56
    },
    {
      key: 'receivableStatus',
      label: t('field.receivableStatus'),
      width: 150,
      minWidth: 56
    }
  );

  if (workspace === 'approved') {
    columns.push(
      {
        key: 'returnStatus',
        label: t('field.returnStatus'),
        width: 130,
        minWidth: 56
      },
      {
        key: 'redFlushTrace',
        label: t('field.redFlushTrace'),
        prop: 'redFlushTrace',
        width: 160,
        minWidth: 56
      }
    );
  }

  columns.push(
    {
      key: 'createdAt',
      label: t('field.createdTime'),
      width: 180,
      minWidth: 56,
      nowrap: true
    },
    actionsColumn(t)
  );

  return columns;
};

export const createSaleReturnRouteFrameColumns = (t: Translate): ErpDataTableColumn[] => [
  indexColumn(t),
  {
    key: 'orderNo',
    label: t('field.orderNo'),
    prop: 'orderNo',
    width: 160,
    minWidth: 56
  },
  {
    key: 'customer',
    label: t('field.customer'),
    width: 160,
    minWidth: 56
  },
  {
    key: 'status',
    label: t('field.status'),
    prop: 'status',
    width: 120,
    minWidth: 56
  },
  {
    key: 'totalAmount',
    label: t('field.totalAmount'),
    prop: 'totalAmount',
    width: 140,
    minWidth: 56
  },
  {
    key: 'refundStatus',
    label: t('field.refundStatus'),
    width: 150,
    minWidth: 56
  },
  {
    key: 'createdAt',
    label: t('field.createdTime'),
    prop: 'createdAt',
    width: 180,
    minWidth: 56,
    nowrap: true
  },
  actionsColumn(t)
];
