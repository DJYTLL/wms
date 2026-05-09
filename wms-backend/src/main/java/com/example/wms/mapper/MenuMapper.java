package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 菜单 Mapper
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    @Select("SELECT * FROM app_menu WHERE code = #{code}")
    Menu findByCode(String code);

    @Select("SELECT * FROM app_menu WHERE is_enabled = TRUE ORDER BY sort ASC, id ASC")
    List<Menu> listEnabled();

    @Select("SELECT * FROM app_menu ORDER BY sort ASC, id ASC")
    List<Menu> listAllOrdered();
}
