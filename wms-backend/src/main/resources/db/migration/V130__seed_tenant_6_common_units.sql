-- 为租户 6 补齐单位管理常用计量单位。
-- 幂等策略：同租户下已存在相同编码或相同名称的未删除单位时跳过，不覆盖历史数据。

WITH unit_seed(code, name, symbol, precision) AS (
    VALUES
        ('UNIT-001', '件', '件', 0),
        ('UNIT-002', '箱', '箱', 0),
        ('UNIT-003', '千克', 'kg', 3),
        ('UNIT-004', '米', 'm', 3),
        ('UNIT-005', '个', '个', 0),
        ('UNIT-006', '盒', '盒', 0),
        ('UNIT-007', '包', '包', 0),
        ('UNIT-008', '袋', '袋', 0),
        ('UNIT-009', '瓶', '瓶', 0),
        ('UNIT-010', '罐', '罐', 0),
        ('UNIT-011', '桶', '桶', 0),
        ('UNIT-012', '套', '套', 0),
        ('UNIT-013', '双', '双', 0),
        ('UNIT-014', '条', '条', 0),
        ('UNIT-015', '支', '支', 0),
        ('UNIT-016', '张', '张', 0),
        ('UNIT-017', '卷', '卷', 0),
        ('UNIT-018', '片', '片', 0),
        ('UNIT-019', '根', '根', 0),
        ('UNIT-020', '台', '台', 0),
        ('UNIT-021', '辆', '辆', 0),
        ('UNIT-022', '打', '打', 0),
        ('UNIT-023', '克', 'g', 3),
        ('UNIT-024', '吨', 't', 3),
        ('UNIT-025', '升', 'L', 3),
        ('UNIT-026', '毫升', 'mL', 3),
        ('UNIT-027', '厘米', 'cm', 3),
        ('UNIT-028', '毫米', 'mm', 3),
        ('UNIT-029', '平方米', 'm2', 3),
        ('UNIT-030', '立方米', 'm3', 3),
        ('UNIT-031', '斤', '斤', 2),
        ('UNIT-032', '把', '把', 0),
        ('UNIT-033', '公斤', '公斤', 3),
        ('UNIT-034', '次', '次', 0),
        ('UNIT-035', '颗', '颗', 0),
        ('UNIT-036', '元', '元', 2),
        ('UNIT-037', '只', '只', 0)
)
INSERT INTO erp_unit (tenant_id, code, name, symbol, precision, is_enabled, remark, created_at, updated_at)
SELECT 6,
       s.code,
       s.name,
       s.symbol,
       s.precision,
       TRUE,
       '租户6常用计量单位',
       NOW(),
       NOW()
FROM unit_seed s
WHERE EXISTS (
      SELECT 1
      FROM app_tenant t
      WHERE t.id = 6
        AND t.deleted_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1
      FROM erp_unit u
      WHERE u.tenant_id = 6
        AND u.deleted_at IS NULL
        AND (u.code = s.code OR u.name = s.name)
  );
