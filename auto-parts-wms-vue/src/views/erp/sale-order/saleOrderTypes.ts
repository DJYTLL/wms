export interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
  categoryId?: number;
  defaultSettlementMethodCode?: string;
  defaultReceiptMethodCode?: string;
  deliveryMethodCode?: string;
}

export interface ProductOption {
  id: number;
  name: string;
  productType?: string;
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  salePrice?: number;
  costPrice?: number;
  enabled?: boolean;
}

export interface ErpProductDetail {
  id: number;
  name: string;
  productType?: string;
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  salePrice?: number;
  costPrice?: number;
  enabled?: boolean;
}

export interface StockOption {
  key: string;
  warehouseId: number | null;
  warehouseName: string;
  locationId: number | null;
  locationName: string;
  qtyOnHand: number;
  qtyAvailable: number;
  qtyLocked: number;
  label: string;
  searchLabel: string;
}

export interface CodeOptionItem {
  id: number;
  code: string;
  name: string;
  isDefault?: boolean;
  fundInputMode?: 'HIDDEN' | 'OPTIONAL' | 'REQUIRED';
}

export interface SaleHistoryItem {
  orderId: number;
  orderNo: string;
  orderAt: string;
  productId: number;
  qty: number;
  price: number;
  priceInclTax: number;
  customerId: number;
  customerName: string;
}

export interface PurchaseHistoryItem {
  orderId: number;
  orderNo: string;
  orderAt: string;
  productId: number;
  qty: number;
  price: number;
  priceInclTax: number;
  supplierId: number;
  supplierName: string;
}

export interface PriceHistoryItem {
  customerCategoryName: string;
  salePrice: number;
  updatedAt: string;
}

export interface HistoryDialogTabState {
  keyword: string;
  range: string[];
  page: number;
  size: number;
  total: number;
}

export interface SaleOrderItem {
  id?: number;
  productId?: number;
  productName?: string;
  warehouseId?: number;
  locationId?: number;
  stockKey?: string;
  qty?: string | number;
  price?: string | number;
  unitCost?: number;
  taxRate?: number;
  remark?: string;
  sortNo?: number;
  _priceRequestSeq?: number;
}

export interface AssemblyTemplateOption {
  id: number;
  name: string;
  orderType?: string;
  finishedProductId: number;
  finishedQty?: number | string;
  warehouseId?: number | null;
  locationId?: number | null;
  laborCost?: number | string | null;
  remark?: string;
}

export interface AssemblyQuickItem {
  productId: number | null;
  productName?: string;
  warehouseId: number | null;
  locationId: number | null;
  stockKey: string;
  qty: string;
  remark?: string;
}

export interface SaleOrderFormData {
  id?: number | null;
  orderNo: string;
  orderAt: string;
  customerId: number | null;
  deliveryMethod: string;
  settlementMethod: string;
  receiptMethodCode: string;
  paidAmount: string;
  discountAmount: string;
  customerDebtTotal: string;
  status: string;
  createdBy: string;
  updatedBy: string;
  remark: string;
  items: SaleOrderItem[];
}
