package com.example.wms.mapper;

import com.example.wms.entity.Permission;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

// 角色-权限关联 Mapper：用于初始化或赋权
@Mapper
public interface RolePermissionMapper {
    // 插入关联记录，冲突时忽略
    @Insert("""
        INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
        VALUES (#{tenantId}, #{roleId}, #{permissionId}, NOW(), NOW())
        ON CONFLICT DO NOTHING
        """)
    int insertIgnore(@Param("tenantId") Long tenantId,
                     @Param("roleId") Long roleId,
                     @Param("permissionId") Long permissionId);

    // 删除指定角色下的指定权限
    @Update("""
        UPDATE app_role_permission
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND permission_id = #{permissionId}
          AND deleted_at IS NULL
        """)
    int deleteByRoleIdAndPermissionId(@Param("tenantId") Long tenantId,
                                      @Param("roleId") Long roleId,
                                      @Param("permissionId") Long permissionId);

    // 删除角色下全部权限
    @Update("""
        UPDATE app_role_permission
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND deleted_at IS NULL
        """)
    int deleteByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    // 删除角色下的指定权限
    @Update("""
        <script>
        UPDATE app_role_permission
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND deleted_at IS NULL
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
    @Update("""
        UPDATE app_role_permission
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE permission_id = #{permissionId}
          AND deleted_at IS NULL
        """)
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    // 查询拥有某权限的角色 ID
    @Select("""
        SELECT tenant_id, role_id
        FROM app_role_permission
        WHERE permission_id = #{permissionId}
          AND deleted_at IS NULL
        """)
    List<RoleTenantPair> findRoleTenantPairsByPermissionId(@Param("permissionId") Long permissionId);

    // 统计当前仍绑定该权限的角色数量
    @Select("""
        SELECT COUNT(DISTINCT role_id)
        FROM app_role_permission
        WHERE permission_id = #{permissionId}
          AND deleted_at IS NULL
        """)
    long countActiveRolesByPermissionId(@Param("permissionId") Long permissionId);

    // 批量统计权限仍绑定的角色数量
    @MapKey("permissionId")
    @Select("""
        <script>
        SELECT permission_id AS permissionId, COUNT(DISTINCT role_id) AS roleCount
        FROM app_role_permission
        WHERE deleted_at IS NULL
          AND permission_id IN
        <foreach item="id" collection="permissionIds" open="(" separator="," close=")">
            #{id}
        </foreach>
        GROUP BY permission_id
        </script>
        """)
    Map<Long, RolePermissionCountRow> countActiveRolesByPermissionIds(@Param("permissionIds") List<Long> permissionIds);

    // 查询角色拥有的权限
    @Select("""
        SELECT p.*
        FROM app_permission p
        JOIN app_role_permission rp ON p.id = rp.permission_id
        WHERE rp.tenant_id = #{tenantId}
          AND rp.role_id = #{roleId}
          AND rp.deleted_at IS NULL
          AND p.deleted_at IS NULL
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

    class RolePermissionCountRow {
        private Long permissionId;
        private Long roleCount;

        public Long getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(Long permissionId) {
            this.permissionId = permissionId;
        }

        public Long getRoleCount() {
            return roleCount;
        }

        public void setRoleCount(Long roleCount) {
            this.roleCount = roleCount;
        }
    }
}
