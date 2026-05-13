package com.example.wms;

import com.example.wms.controller.erp.ErpLocationController;
import com.example.wms.controller.erp.ErpProductController;
import com.example.wms.controller.erp.ErpWarehouseController;
import com.example.wms.dto.erp.ErpWarehouseCreateRequest;
import com.example.wms.dto.erp.ErpLocationCreateRequest;
import com.example.wms.dto.erp.ErpLocationUpdateRequest;
import com.example.wms.dto.erp.ErpWarehouseUpdateRequest;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpLocationService;
import com.example.wms.service.erp.ErpProductService;
import com.example.wms.service.erp.ErpWarehouseService;
import com.example.wms.service.erp.impl.ErpLocationServiceImpl;
import com.example.wms.service.erp.impl.ErpWarehouseServiceImpl;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErpMasterDataGovernanceTests {
    private static final Long TENANT_ID = 100L;

    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpLocationMapper locationMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockBalanceMapper stockBalanceMapper;
    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpAssemblyOrderMapper assemblyOrderMapper;
    @Mock
    private ErpAssemblyOrderItemMapper assemblyOrderItemMapper;
    @Mock
    private ErpStockCountMapper stockCountMapper;
    @Mock
    private ErpStockCountItemMapper stockCountItemMapper;
    @Mock
    private ErpStockTxnMapper stockTxnMapper;
    @Mock
    private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpLocationService locationService;
    @Mock
    private ErpProductService productService;

    @BeforeEach
    void setUpTenant() {
        TenantContext.setTenantId(TENANT_ID);
        mockAllReferenceCounts(0L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void warehouseDeleteBlocksWhenReferencedByLocation() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(1L, "WH-001", true));
        when(locationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仓库下仍有关联库位，不能删除");

        verify(warehouseMapper, never()).deleteById(1L);
    }

    @Test
    void warehouseDeleteBlocksWhenReferencedByStockBalance() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(1L, "WH-001", true));
        when(stockBalanceMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仓库仍有关联库存，不能删除");
    }

    @Test
    void warehouseDeleteUsesLogicalDeleteWhenUnreferenced() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(1L, "WH-001", true));

        service.delete(1L);

        verify(warehouseMapper).deleteById(1L);
    }

    @Test
    void warehouseDisableBlocksWhenReferencedByDefaultProduct() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(1L, "WH-001", true));
        when(warehouseMapper.findByCode(TENANT_ID, "WH-001")).thenReturn(null);
        when(productMapper.selectCount(any())).thenReturn(1L);

        ErpWarehouseUpdateRequest request =
            new ErpWarehouseUpdateRequest(" WH-001 ", " 总仓 ", "A", "M", "P", false, "R");

        assertThatThrownBy(() -> service.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仓库仍被商品设为默认仓库，不能停用");
    }

    @Test
    void warehouseDisableBlocksWhenReferencedByDraftStockCount() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(1L, "WH-001", true));
        when(warehouseMapper.findByCode(TENANT_ID, "WH-001")).thenReturn(null);
        when(stockCountMapper.selectCount(any())).thenReturn(1L);

        ErpWarehouseUpdateRequest request =
            new ErpWarehouseUpdateRequest("WH-001", "总仓", null, null, null, false, null);

        assertThatThrownBy(() -> service.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仓库仍被未完成盘点单引用，不能停用");
    }

    @Test
    void warehouseCreateNormalizesCodeToUppercase() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.findByCode(TENANT_ID, "WH/001_A")).thenReturn(null);

        service.create(new ErpWarehouseCreateRequest(" wh/001_a ", "总仓", "A", "M", "P", true, "R"));

        ArgumentCaptor<ErpWarehouse> captor = ArgumentCaptor.forClass(ErpWarehouse.class);
        verify(warehouseMapper).insert(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("WH/001_A");
    }

    @Test
    void warehouseCreateRejectsDuplicateAfterNormalization() {
        ErpWarehouseServiceImpl service = warehouseServiceImpl();
        when(warehouseMapper.findByCode(TENANT_ID, "WH-001")).thenReturn(warehouse(9L, "WH-001", true));

        assertThatThrownBy(() -> service.create(new ErpWarehouseCreateRequest(" wh-001 ", "总仓", null, null, null, true, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仓库编码已存在");
    }

    @Test
    void locationDeleteBlocksWhenReferencedByStockBalance() {
        ErpLocationServiceImpl service = locationServiceImpl();
        when(locationMapper.selectOne(any())).thenReturn(location(2L, 8L, "LOC-001", true));
        when(stockBalanceMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("库位仍有关联库存，不能删除");
    }

    @Test
    void locationDeleteUsesLogicalDeleteWhenUnreferenced() {
        ErpLocationServiceImpl service = locationServiceImpl();
        when(locationMapper.selectOne(any())).thenReturn(location(2L, 8L, "LOC-001", true));

        service.delete(2L);

        verify(locationMapper).deleteById(2L);
    }

    @Test
    void locationDisableBlocksWhenReferencedByDefaultProduct() {
        ErpLocationServiceImpl service = locationServiceImpl();
        ErpLocation existing = location(2L, 8L, "LOC-001", true);
        when(locationMapper.selectOne(any())).thenReturn(existing);
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(8L, "WH-001", true));
        when(locationMapper.findByCode(TENANT_ID, 8L, "LOC-001")).thenReturn(null);
        when(productMapper.selectCount(any())).thenReturn(1L);

        ErpLocationUpdateRequest request =
            new ErpLocationUpdateRequest(8L, " LOC-001 ", " 一层库位 ", "A", "R", "B", false, "R");

        assertThatThrownBy(() -> service.update(2L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("库位仍被商品设为默认库位，不能停用");
    }

    @Test
    void locationCreatePersistsTrimmedAisleRackAndBin() {
        ErpLocationServiceImpl service = locationServiceImpl();
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(8L, "WH-001", true));
        when(locationMapper.findByCode(TENANT_ID, 8L, "LOC-NEW")).thenReturn(null);

        ErpLocationCreateRequest request =
            new ErpLocationCreateRequest(8L, " LOC-NEW ", " 库位一 ", " A01 ", " R01 ", " B01 ", true, " 备注 ");

        service.create(request);

        ArgumentCaptor<ErpLocation> captor = ArgumentCaptor.forClass(ErpLocation.class);
        verify(locationMapper).insert(captor.capture());
        ErpLocation saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("LOC-NEW");
        assertThat(saved.getName()).isEqualTo("库位一");
        assertThat(saved.getAisle()).isEqualTo("A01");
        assertThat(saved.getRack()).isEqualTo("R01");
        assertThat(saved.getBin()).isEqualTo("B01");
        assertThat(saved.getRemark()).isEqualTo("备注");
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    void locationUpdatePersistsTrimmedAisleRackAndBin() {
        ErpLocationServiceImpl service = locationServiceImpl();
        ErpLocation existing = location(2L, 8L, "LOC-001", true);
        when(locationMapper.selectOne(any())).thenReturn(existing);
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(8L, "WH-001", true));
        when(locationMapper.findByCode(TENANT_ID, 8L, "LOC-001")).thenReturn(existing);

        ErpLocationUpdateRequest request =
            new ErpLocationUpdateRequest(8L, " LOC-001 ", " 新库位 ", " A02 ", " R02 ", " B02 ", true, " 新备注 ");

        service.update(2L, request);

        ArgumentCaptor<ErpLocation> captor = ArgumentCaptor.forClass(ErpLocation.class);
        verify(locationMapper).updateById(captor.capture());
        ErpLocation saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("新库位");
        assertThat(saved.getAisle()).isEqualTo("A02");
        assertThat(saved.getRack()).isEqualTo("R02");
        assertThat(saved.getBin()).isEqualTo("B02");
        assertThat(saved.getRemark()).isEqualTo("新备注");
    }

    @Test
    void locationCreateRejectsInvalidCode() {
        ErpLocationServiceImpl service = locationServiceImpl();

        assertThatThrownBy(() -> service.create(
            new ErpLocationCreateRequest(8L, "loc 001", "库位一", "A01", "R01", "B01", true, "备注")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("编码只能包含字母、数字、短横线、下划线或斜杠");
    }

    @Test
    void warehouseOptionsOnlyRequestEnabledData() {
        ErpWarehouseController controller = new ErpWarehouseController(warehouseService);
        List<ErpWarehouse> warehouses = List.of(warehouse(1L, "WH-001", true));
        when(warehouseService.listAll(null, true)).thenReturn(warehouses);

        ResponseEntity<?> response = controller.options();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(warehouseService).listAll(null, true);
    }

    @Test
    void locationOptionsPassWarehouseFilter() {
        ErpLocationController controller = new ErpLocationController(locationService);
        List<ErpLocation> locations = List.of(location(2L, 8L, "LOC-001", true));
        when(locationService.listAll(null, true, 8L)).thenReturn(locations);

        ResponseEntity<?> response = controller.options(8L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(locationService).listAll(null, true, 8L);
    }

    @Test
    void productOptionsOnlyRequestEnabledData() {
        ErpProductController controller = new ErpProductController(productService);
        List<com.example.wms.entity.erp.ErpProduct> products = List.of(product(3L, true));
        when(productService.listAll(null, true, null)).thenReturn(products);

        ResponseEntity<?> response = controller.options(null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(productService).listAll(null, true, null);
    }

    private ErpWarehouseServiceImpl warehouseServiceImpl() {
        return new ErpWarehouseServiceImpl(
            warehouseMapper,
            locationMapper,
            productMapper,
            stockBalanceMapper,
            purchaseOrderItemMapper,
            purchaseReturnItemMapper,
            saleOrderItemMapper,
            saleReturnItemMapper,
            assemblyOrderMapper,
            assemblyOrderItemMapper,
            stockCountMapper,
            stockCountItemMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper
        );
    }

    private ErpLocationServiceImpl locationServiceImpl() {
        return new ErpLocationServiceImpl(
            locationMapper,
            warehouseMapper,
            productMapper,
            stockBalanceMapper,
            purchaseOrderItemMapper,
            purchaseReturnItemMapper,
            saleOrderItemMapper,
            saleReturnItemMapper,
            assemblyOrderMapper,
            assemblyOrderItemMapper,
            stockCountMapper,
            stockCountItemMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper
        );
    }

    private void mockAllReferenceCounts(Long value) {
        when(locationMapper.selectCount(any())).thenReturn(value);
        when(productMapper.selectCount(any())).thenReturn(value);
        when(stockBalanceMapper.selectCount(any())).thenReturn(value);
        when(purchaseOrderItemMapper.selectCount(any())).thenReturn(value);
        when(purchaseReturnItemMapper.selectCount(any())).thenReturn(value);
        when(saleOrderItemMapper.selectCount(any())).thenReturn(value);
        when(saleReturnItemMapper.selectCount(any())).thenReturn(value);
        when(assemblyOrderMapper.selectCount(any())).thenReturn(value);
        when(assemblyOrderItemMapper.selectCount(any())).thenReturn(value);
        when(stockCountMapper.selectCount(any())).thenReturn(value);
        when(stockCountItemMapper.selectCount(any())).thenReturn(value);
        when(stockTxnMapper.selectCount(any())).thenReturn(value);
    }

    private static ErpWarehouse warehouse(Long id, String code, boolean enabled) {
        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setId(id);
        warehouse.setCode(code);
        warehouse.setName(code);
        warehouse.setEnabled(enabled);
        warehouse.setTenantId(TENANT_ID);
        return warehouse;
    }

    private static ErpLocation location(Long id, Long warehouseId, String code, boolean enabled) {
        ErpLocation location = new ErpLocation();
        location.setId(id);
        location.setWarehouseId(warehouseId);
        location.setCode(code);
        location.setName(code);
        location.setEnabled(enabled);
        location.setTenantId(TENANT_ID);
        return location;
    }

    private static com.example.wms.entity.erp.ErpProduct product(Long id, boolean enabled) {
        com.example.wms.entity.erp.ErpProduct product = new com.example.wms.entity.erp.ErpProduct();
        product.setId(id);
        product.setCode("P-" + id);
        product.setName("Product-" + id);
        product.setEnabled(enabled);
        product.setTenantId(TENANT_ID);
        return product;
    }
}
