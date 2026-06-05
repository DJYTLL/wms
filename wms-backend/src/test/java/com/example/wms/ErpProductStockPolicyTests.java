package com.example.wms;

import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErpProductStockPolicyTests {

    @Test
    void productCreateRequestShouldExposeWarehouseStockPolicies() {
        List<String> componentNames = Arrays.stream(ErpProductCreateRequest.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

        assertThat(componentNames).contains("stockPolicies");
    }

    @Test
    void productUpdateRequestShouldExposeWarehouseStockPolicies() {
        List<String> componentNames = Arrays.stream(ErpProductUpdateRequest.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

        assertThat(componentNames).contains("stockPolicies");
    }

    @Test
    void productRequestShouldKeepPriceItemsAfterStockPolicies() {
        RecordComponent[] components = ErpProductCreateRequest.class.getRecordComponents();
        List<String> componentNames = Arrays.stream(components)
            .map(RecordComponent::getName)
            .toList();

        assertThat(componentNames).containsSubsequence("stockPolicies", "priceItems");
    }
}
