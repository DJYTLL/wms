package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpOrderSequence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 订单号序列 Mapper（ERP进销存）
@Mapper
public interface ErpOrderSequenceMapper extends BaseMapper<ErpOrderSequence> {
    // 初始化序列行
    @Insert("INSERT INTO erp_order_sequence (tenant_id, order_type, date_key, current_value, updated_at) " +
        "VALUES (#{tenantId}, #{orderType}, #{dateKey}, 0, NOW()) " +
        "ON CONFLICT (tenant_id, order_type, date_key) DO NOTHING")
    int insertIgnore(@Param("tenantId") Long tenantId,
                     @Param("orderType") String orderType,
                     @Param("dateKey") String dateKey);

    // 递增并返回序号
    @Select("UPDATE erp_order_sequence SET current_value = current_value + 1, updated_at = NOW() " +
        "WHERE tenant_id = #{tenantId} AND order_type = #{orderType} AND date_key = #{dateKey} " +
        "RETURNING current_value")
    Long incrementAndGet(@Param("tenantId") Long tenantId,
                         @Param("orderType") String orderType,
                         @Param("dateKey") String dateKey);
}
