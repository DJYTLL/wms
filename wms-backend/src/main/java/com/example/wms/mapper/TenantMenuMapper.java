package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.TenantMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 租户菜单映射 Mapper
@Mapper
public interface TenantMenuMapper extends BaseMapper<TenantMenu> {
    @Select("SELECT tenant_id, menu_id, is_enabled AS enabled, created_at, updated_at FROM app_tenant_menu WHERE tenant_id = #{tenantId}")
    List<TenantMenu> findByTenantId(@Param("tenantId") Long tenantId);

    @Delete("DELETE FROM app_tenant_menu WHERE tenant_id = #{tenantId}")
    int deleteByTenantId(@Param("tenantId") Long tenantId);

    @Delete("DELETE FROM app_tenant_menu WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);
}
