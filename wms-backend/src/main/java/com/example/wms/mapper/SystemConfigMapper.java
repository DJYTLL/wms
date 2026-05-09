package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 系统配置 Mapper
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
    @Select("SELECT * FROM app_system_config ORDER BY config_key ASC")
    List<SystemConfig> findAll();

    @Select("SELECT * FROM app_system_config WHERE config_key = #{key}")
    SystemConfig findByKey(@Param("key") String key);

    @Select("SELECT * FROM app_system_config WHERE is_public = TRUE ORDER BY config_key ASC")
    List<SystemConfig> findPublic();
}
