package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpCounterpartyFinanceDetailRow;
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

    @Select("""
        SELECT 'RECEIVABLE' AS detail_type,
               ar.id AS biz_id,
               ar.order_no AS biz_no,
               c.id AS target_id,
               c.code AS target_code,
               c.name AS target_name,
               ar.total_amount AS total_amount,
               ar.unpaid_amount AS unpaid_amount,
               ar.status AS status,
               ar.created_at AS created_at
        FROM erp_accounts_receivable ar
        JOIN erp_customer c
          ON c.id = ar.customer_id
         AND c.tenant_id = ar.tenant_id
         AND c.deleted_at IS NULL
        WHERE ar.tenant_id = #{tenantId}
          AND c.counterparty_subject_id = #{subjectId}
          AND ar.deleted_at IS NULL
        UNION ALL
        SELECT 'PAYABLE' AS detail_type,
               ap.id AS biz_id,
               ap.order_no AS biz_no,
               s.id AS target_id,
               s.code AS target_code,
               s.name AS target_name,
               ap.total_amount AS total_amount,
               ap.unpaid_amount AS unpaid_amount,
               ap.status AS status,
               ap.created_at AS created_at
        FROM erp_accounts_payable ap
        JOIN erp_supplier s
          ON s.id = ap.supplier_id
         AND s.tenant_id = ap.tenant_id
         AND s.deleted_at IS NULL
        WHERE ap.tenant_id = #{tenantId}
          AND s.counterparty_subject_id = #{subjectId}
          AND ap.deleted_at IS NULL
        ORDER BY created_at DESC, biz_id DESC
        """)
    List<ErpCounterpartyFinanceDetailRow> listFinanceDetailRows(@Param("tenantId") Long tenantId, @Param("subjectId") Long subjectId);
}
