package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpCounterpartyFinanceSummaryView;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 往来主体 Mapper（ERP进销存）
@Mapper
public interface ErpCounterpartySubjectMapper extends BaseMapper<ErpCounterpartySubject> {
    @Select("SELECT * FROM erp_counterparty_subject WHERE tenant_id = #{tenantId} AND name = #{name} AND deleted_at IS NULL")
    ErpCounterpartySubject findByName(@Param("tenantId") Long tenantId, @Param("name") String name);

    @Select("""
        SELECT subject_id AS subjectId,
               subject_name AS subjectName,
               receivable_total AS receivableTotal,
               payable_total AS payableTotal,
               net_amount AS netAmount,
               customer_count AS customerCount,
               supplier_count AS supplierCount
        FROM erp_counterparty_subject_finance_summary_v
        WHERE tenant_id = #{tenantId}
        ORDER BY subject_name, subject_id
        """)
    List<ErpCounterpartyFinanceSummaryView> listFinanceSummaries(@Param("tenantId") Long tenantId);
}
