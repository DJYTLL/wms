package com.example.wms;

import com.example.wms.dto.PageResponse;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpUnitMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.impl.ErpProductServiceImpl;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpProductSearchRankingTests {
    @Mock private ErpProductMapper productMapper;
    @Mock private ErpProductPriceMapper productPriceMapper;
    @Mock private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private ErpCategoryMapper categoryMapper;
    @Mock private ErpUnitMapper unitMapper;
    @Mock private ErpWarehouseMapper warehouseMapper;
    @Mock private ErpLocationMapper locationMapper;
    @Mock private ErpCustomerCategoryMapper customerCategoryMapper;
    @Mock private ErpSupplierMapper supplierMapper;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void productPageSearchMatchesChineseFullPinyinInitialsAndRanksBestHitsFirst() {
        when(productMapper.selectList(any())).thenReturn(List.of(
            product(1L, "P-001", "重汽变速箱总成", "变速箱"),
            product(2L, "P-002", "变速箱油封", "油封"),
            product(3L, "P-003", "离合器分泵", "分泵"),
            product(4L, "LS-004", "普通螺丝", "螺丝")
        ));

        ErpProductServiceImpl service = productService();

        PageResponse<ErpProduct> chineseResult = service.page(1, 20, "变速箱", true, null);
        assertThat(chineseResult.items()).extracting(ErpProduct::getName)
            .containsExactly("重汽变速箱总成", "变速箱油封");

        PageResponse<ErpProduct> pinyinResult = service.page(1, 20, "biansuxiang", true, null);
        assertThat(pinyinResult.items()).extracting(ErpProduct::getName)
            .containsExactly("重汽变速箱总成", "变速箱油封");

        PageResponse<ErpProduct> initialsResult = service.page(1, 20, "bsx", true, null);
        assertThat(initialsResult.items()).extracting(ErpProduct::getName)
            .containsExactly("重汽变速箱总成", "变速箱油封");
    }

    private ErpProductServiceImpl productService() {
        return new ErpProductServiceImpl(
            productMapper,
            productPriceMapper,
            orderSequenceMapper,
            systemConfigMapper,
            categoryMapper,
            unitMapper,
            warehouseMapper,
            locationMapper,
            customerCategoryMapper,
            supplierMapper,
            new ObjectMapper(),
            null,
            null,
            new ExcelImportParser()
        );
    }

    private ErpProduct product(Long id, String code, String name, String shortName) {
        ErpProduct product = new ErpProduct();
        product.setId(id);
        product.setTenantId(1L);
        product.setCode(code);
        product.setName(name);
        product.setShortName(shortName);
        product.setEnabled(true);
        return product;
    }
}
