import { computed, type Reactive } from 'vue';
import type { ProductOption, SaleOrderFormData, SaleOrderItem } from './saleOrderTypes';
import {
  calcLineAmount,
  formatMoney,
  formatRate,
  parseAmount,
  parseDecimal
} from './saleOrderNumberUtils';

interface UseSaleOrderPricingOptions {
  formData: Reactive<SaleOrderFormData>;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
  };
  notifyError: (error: unknown) => void;
  normalizeArray: <T>(value: any) => T[];
  findKnownProduct: (productId?: number | null) => ProductOption | undefined;
  getCurrentCustomerCategoryId: () => number | undefined;
}

export const useSaleOrderPricing = ({
  formData,
  request,
  notifyError,
  normalizeArray,
  findKnownProduct,
  getCurrentCustomerCategoryId
}: UseSaleOrderPricingOptions) => {
  const fetchLastSalePrice = async (row: SaleOrderItem) => {
    if (!row.productId || !formData.customerId) return null;
    try {
      const res: any = await request.get('/erp/sale-orders/recent-items', {
        params: { customerId: formData.customerId, productId: row.productId, limit: 1 }
      });
      const items = normalizeArray<any>(res?.data?.data);
      if (!items.length) return null;
      const latest = items[0];
      if (latest?.price == null || latest?.price === '') return null;
      return Number(latest.price);
    } catch (error) {
      notifyError(error);
      return null;
    }
  };

  const beginRowPriceRequest = (row: SaleOrderItem) => {
    const nextSeq = (row._priceRequestSeq || 0) + 1;
    row._priceRequestSeq = nextSeq;
    return nextSeq;
  };

  const isLatestRowPriceRequest = (
    row: SaleOrderItem,
    requestSeq: number,
    productId?: number,
    customerId?: number | null
  ) => {
    return row._priceRequestSeq === requestSeq
      && row.productId === productId
      && formData.customerId === customerId;
  };

  const applyPriceForRow = async (row: SaleOrderItem, force = false) => {
    if (!row.productId) return;
    if (!force && row.price) return;
    const productId = row.productId;
    const customerId = formData.customerId;
    const requestSeq = beginRowPriceRequest(row);
    const lastPrice = await fetchLastSalePrice(row);
    if (!isLatestRowPriceRequest(row, requestSeq, productId, customerId)) return;
    if (lastPrice != null) {
      row.price = String(lastPrice);
      return;
    }
    const categoryId = getCurrentCustomerCategoryId();
    if (categoryId) {
      try {
        const res: any = await request.get('/erp/product-prices/resolve', {
          params: { productId, customerCategoryId: categoryId }
        });
        if (!isLatestRowPriceRequest(row, requestSeq, productId, customerId)) return;
        const resolved = res.data.data?.salePrice;
        if (resolved != null && resolved !== '') {
          row.price = String(resolved);
          return;
        }
      } catch (error) {
        notifyError(error);
      }
    }
    if (!isLatestRowPriceRequest(row, requestSeq, productId, customerId)) return;
    const product = findKnownProduct(productId);
    if (product && product.salePrice != null) {
      row.price = String(product.salePrice);
    }
  };

  const getProductCost = (row: SaleOrderItem) => {
    if (row.unitCost != null) {
      return Number(row.unitCost);
    }
    if (!row.productId) return null;
    const product = findKnownProduct(row.productId);
    if (!product || product.costPrice == null) return null;
    return Number(product.costPrice);
  };

  const totalAmountBeforeDiscount = computed(() => {
    let amount = 0;
    formData.items.forEach((row) => {
      const lineAmount = calcLineAmount(row);
      if (lineAmount == null) return;
      amount += lineAmount;
    });
    return amount;
  });

  const getDiscountAmount = () => {
    const discount = parseAmount(formData.discountAmount || '');
    if (discount == null) return 0;
    return Math.max(0, discount);
  };

  const calcLineDiscount = (row: SaleOrderItem) => {
    const totalAmount = totalAmountBeforeDiscount.value;
    const discountAmount = getDiscountAmount();
    if (!totalAmount || !discountAmount) return 0;
    const lineAmount = calcLineAmount(row);
    if (lineAmount == null) return 0;
    return (lineAmount / totalAmount) * discountAmount;
  };

  const calcLineNetAmount = (row: SaleOrderItem) => {
    const amount = calcLineAmount(row);
    if (amount == null) return null;
    return amount - calcLineDiscount(row);
  };

  const calcLineProfit = (row: SaleOrderItem) => {
    const amount = calcLineNetAmount(row);
    if (amount == null) return null;
    const cost = getProductCost(row);
    if (cost == null) return null;
    return amount - (cost * (parseDecimal(row.qty, 4) || 0));
  };

  const formatProfitCell = (row: SaleOrderItem) => {
    const profit = calcLineProfit(row);
    const amount = calcLineNetAmount(row);
    if (profit == null || amount == null || amount === 0) return '-';
    return `${formatMoney(profit)} (${formatRate(profit / amount)})`;
  };

  const totalSummary = computed(() => {
    let amount = 0;
    let netAmount = 0;
    let profit = 0;
    let hasMissingCost = false;
    formData.items.forEach((row) => {
      const lineAmount = calcLineAmount(row);
      if (lineAmount == null) return;
      amount += lineAmount;
      const lineNetAmount = calcLineNetAmount(row);
      if (lineNetAmount != null) {
        netAmount += lineNetAmount;
      }
      const lineProfit = calcLineProfit(row);
      if (lineProfit == null) {
        if (row.productId) {
          hasMissingCost = true;
        }
        return;
      }
      profit += lineProfit;
    });
    const rate = netAmount ? profit / netAmount : null;
    return { amount, netAmount, profit, rate, hasMissingCost };
  });

  const totalProfitText = computed(() => {
    if (totalSummary.value.hasMissingCost) return '-';
    return formatMoney(totalSummary.value.profit);
  });

  const totalProfitRateText = computed(() => {
    if (totalSummary.value.hasMissingCost) return '-';
    return formatRate(totalSummary.value.rate);
  });

  return {
    applyPriceForRow,
    calcLineDiscount,
    calcLineNetAmount,
    calcLineProfit,
    formatProfitCell,
    getDiscountAmount,
    getProductCost,
    totalAmountBeforeDiscount,
    totalProfitRateText,
    totalProfitText,
    totalSummary
  };
};
