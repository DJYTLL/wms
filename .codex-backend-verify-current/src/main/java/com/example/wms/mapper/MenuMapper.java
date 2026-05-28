package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

// 菜单 Mapper
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    @Select("SELECT * FROM app_menu WHERE code = #{code} AND deleted_at IS NULL")
    Menu findByCode(String code);

    @Select("SELECT * FROM app_menu WHERE is_enabled = TRUE AND deleted_at IS NULL ORDER BY sort ASC, id ASC")
    List<Menu> listEnabled();

    @Select("SELECT * FROM app_menu WHERE deleted_at IS NULL ORDER BY sort ASC, id ASC")
    List<Menu> listAllOrdered();

    @Select("""
        SELECT COUNT(1)
        FROM app_menu
        WHERE permission_code = #{permissionCode}
          AND deleted_at IS NULL
        """)
    long countActiveMenusByPermissionCode(@Param("permissionCode") String permissionCode);

    @MapKey("permissionCode")
    @Select("""
        <script>
        SELECT permission_code AS permissionCode, COUNT(1) AS menuCount
        FROM app_menu
        WHERE deleted_at IS NULL
          AND permission_code IN
        <foreach item="code" collection="permissionCodes" open="(" separator="," close=")">
            #{code}
        </foreach>
        GROUP BY permission_code
        </script>
        """)
    Map<String, MenuPermissionCountRow> countActiveMenusByPermissionCodes(@Param("permissionCodes") List<String> permissionCodes);

    class MenuPermissionCountRow {
        private String permissionCode;
        private Long menuCount;

        public String getPermissionCode() {
            return permissionCode;
        }

        public void setPermissionCode(String permissionCode) {
            this.permissionCode = permissionCode;
        }

        public Long getMenuCount() {
            return menuCount;
        }

        public void setMenuCount(Long menuCount) {
            this.menuCount = menuCount;
        }
    }
}
