package com.example.wms.mapper;

import com.example.wms.entity.Permission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 角色-权限关联 Mapper：用于初始化或赋权
@Mapper
public interface RolePermissionMapper {
    // 插入关联记录，冲突时忽略
    @Insert("""
        INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
        VALUES (#{tenantId}, #{roleId}, #{permissionId}, NOW())
        ON CONFLICT DO NOTHING
        """)
    int insertIgnore(@Param("tenantId") Long tenantId,
                     @Param("roleId") Long roleId,
                     @Param("permissionId") Long permissionId);

    // 删除指定角色下的指定权限
    @Delete("""
        DELETE FROM app_role_permission
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND permission_id = #{permissionId}
        """)
    int deleteByRoleIdAndPermissionId(@Param("tenantId") Long tenantId,
                                      @Param("roleId") Long roleId,
                                      @Param("permissionId") Long permissionId);

    // 删除角色下全部权限
    @Delete("DELETE FROM app_role_permission WHERE tenant_id = #{tenantId} AND role_id = #{roleId}")
    int deleteByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    // 删除角色下的指定权限
    @Delete("""
        <script>
        DELETE FROM app_role_permission
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND permission_id IN
        <foreach item="id" collection="permissionIds" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    int deleteByRoleIdAndPermissionIds(@Param("tenantId") Long tenantId,
                                       @Param("roleId") Long roleId,
                                       @Param("permissionIds") List<Long> permissionIds);

    // 删除权限关联
    @Delete("DELETE FROM app_role_permission WHERE permission_id = #{permissionId}")
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    // 查询拥有某权限的角色 ID
    @Select("SELECT tenant_id, role_id FROM app_role_permission WHERE permission_id = #{permissionId}")
    List<RoleTenantPair> findRoleTenantPairsByPermissionId(@Param("permissionId") Long permissionId);

    // 查询角色拥有的权限
    @Select("""
        SELECT p.*
        FROM app_permission p
        JOIN app_role_permission rp ON p.id = rp.permission_id
        WHERE rp.tenant_id = #{tenantId}
          AND rp.role_id = #{roleId}
        ORDER BY p.id
        """)
    List<Permission> findPermissionsByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    // 权限影响的角色租户对
    class RoleTenantPair {
        private Long tenantId;
        private Long roleId;

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
    }
}
