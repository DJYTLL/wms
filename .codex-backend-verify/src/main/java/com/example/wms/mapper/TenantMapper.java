package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

// 租户 Mapper
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
    // 按租户编码查询
    @Select("SELECT * FROM app_tenant WHERE code = #{code} AND deleted_at IS NULL")
    Tenant findByCode(String code);
}
