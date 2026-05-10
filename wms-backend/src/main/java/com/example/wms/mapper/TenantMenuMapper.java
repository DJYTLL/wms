package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.TenantMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

// 租户菜单映射 Mapper
@Mapper
public interface TenantMenuMapper extends BaseMapper<TenantMenu> {
    @Select("""
        SELECT id, tenant_id, menu_id, is_enabled AS enabled, created_at, updated_at, deleted_at
        FROM app_tenant_menu
        WHERE tenant_id = #{tenantId}
          AND deleted_at IS NULL
        """)
    List<TenantMenu> findByTenantId(@Param("tenantId") Long tenantId);

    @Update("""
        UPDATE app_tenant_menu
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND deleted_at IS NULL
        """)
    int deleteByTenantId(@Param("tenantId") Long tenantId);

    @Update("""
        UPDATE app_tenant_menu
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE menu_id = #{menuId}
          AND deleted_at IS NULL
        """)
    int deleteByMenuId(@Param("menuId") Long menuId);
}
