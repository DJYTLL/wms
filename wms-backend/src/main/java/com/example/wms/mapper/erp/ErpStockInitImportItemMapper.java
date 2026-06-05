package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockInitImportItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ErpStockInitImportItemMapper extends BaseMapper<ErpStockInitImportItem> {
    @Insert("""
        <script>
        INSERT INTO erp_stock_init_import_item (
            tenant_id,
            batch_id,
            row_no,
            source_code,
            source_name,
            matched_product_id,
            warehouse_name,
            location_name,
            counted_qty,
            init_unit_cost,
            init_total_amount,
            status,
            error_field,
            error_message,
            suggestion,
            warning_message,
            matched_strategy,
            raw_row,
            normalized_payload,
            created_at,
            updated_at
        ) VALUES
        <foreach collection="items" item="item" separator=",">
            (
                #{item.tenantId},
                #{item.batchId},
                #{item.rowNo},
                #{item.sourceCode},
                #{item.sourceName},
                #{item.matchedProductId},
                #{item.warehouseName},
                #{item.locationName},
                #{item.countedQty},
                #{item.initUnitCost},
                #{item.initTotalAmount},
                #{item.status},
                #{item.errorField},
                #{item.errorMessage},
                #{item.suggestion},
                #{item.warningMessage},
                #{item.matchedStrategy},
                #{item.rawRow,typeHandler=com.example.wms.mybatis.JsonbTypeHandler},
                #{item.normalizedPayload,typeHandler=com.example.wms.mybatis.JsonbTypeHandler},
                #{item.createdAt},
                #{item.updatedAt}
            )
        </foreach>
        </script>
        """)
    int insertBatch(@Param("items") List<ErpStockInitImportItem> items);

    @Update("""
        UPDATE erp_stock_init_import_item
        SET status = #{targetStatus}, updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND batch_id = #{batchId}
          AND status = #{sourceStatus}
          AND deleted_at IS NULL
        """)
    int updateStatusByBatch(@Param("tenantId") Long tenantId,
                            @Param("batchId") Long batchId,
                            @Param("sourceStatus") String sourceStatus,
                            @Param("targetStatus") String targetStatus);
}
