package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import org.apache.ibatis.annotations.Mapper;

// 往来主体关联 Mapper（ERP进销存）
@Mapper
public interface ErpCounterpartySubjectLinkMapper extends BaseMapper<ErpCounterpartySubjectLink> {
}
