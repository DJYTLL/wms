package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpCounterpartySubjectMember;
import com.example.wms.entity.erp.ErpCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 客户 Mapper（ERP进销存）
@Mapper
public interface ErpCustomerMapper extends BaseMapper<ErpCustomer> {
    // 按编码查询
    @Select("SELECT * FROM erp_customer WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpCustomer findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("""
        <script>
        SELECT *
        FROM erp_customer
        WHERE tenant_id = #{tenantId}
          AND deleted_at IS NULL
          AND code IN
          <foreach collection="codes" item="code" open="(" separator="," close=")">
              #{code}
          </foreach>
        </script>
        """)
    List<ErpCustomer> findByCodes(@Param("tenantId") Long tenantId, @Param("codes") List<String> codes);

    @Select("""
        SELECT id,
               code,
               name,
               contact,
               phone,
               mobile,
               'CUSTOMER' AS role_type
        FROM erp_customer
        WHERE tenant_id = #{tenantId}
          AND counterparty_subject_id = #{subjectId}
          AND deleted_at IS NULL
        ORDER BY id
        """)
    List<ErpCounterpartySubjectMember> listBySubjectId(@Param("tenantId") Long tenantId, @Param("subjectId") Long subjectId);
}
