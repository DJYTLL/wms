package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpProductImportItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ErpProductImportItemMapper extends BaseMapper<ErpProductImportItem> {
    @Insert("""
        <script>
        INSERT INTO erp_product_import_item (
            tenant_id,
            batch_id,
            row_no,
            source_code,
            source_name,
            matched_product_id,
            category_name,
            unit_name,
            warehouse_name,
            supplier_name,
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
                #{item.categoryName},
                #{item.unitName},
                #{item.warehouseName},
                #{item.supplierName},
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
    int insertBatch(@Param("items") List<ErpProductImportItem> items);
}
