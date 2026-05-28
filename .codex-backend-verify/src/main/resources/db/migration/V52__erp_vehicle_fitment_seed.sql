-- 替换车型适配基础数据为更真实的名称（ERP进销存）
-- 注意：按租户批量重置品牌/车系/车型/适配关系数据

-- 1) 清理旧数据（按依赖顺序）
DELETE FROM erp_product_fitment;
DELETE FROM erp_vehicle_model;
DELETE FROM erp_vehicle_series;
DELETE FROM erp_vehicle_brand;

-- 2) 插入品牌
WITH brand_seed AS (
    SELECT * FROM (VALUES
        ('TOYOTA', '丰田'),
        ('HONDA', '本田'),
        ('VW', '大众'),
        ('BMW', '宝马'),
        ('BENZ', '奔驰'),
        ('AUDI', '奥迪'),
        ('NISSAN', '日产'),
        ('HYUNDAI', '现代'),
        ('KIA', '起亚'),
        ('FORD', '福特')
    ) AS v(code, name)
)
INSERT INTO erp_vehicle_brand (tenant_id, code, name, is_enabled, remark)
SELECT t.id, b.code, b.name, TRUE, 'seed'
FROM app_tenant t
CROSS JOIN brand_seed b;

-- 3) 插入车系
WITH series_seed AS (
    SELECT * FROM (VALUES
        ('TOYOTA', 'COROLLA', '卡罗拉'),
        ('TOYOTA', 'CAMRY', '凯美瑞'),
        ('TOYOTA', 'RAV4', 'RAV4荣放'),
        ('HONDA', 'CIVIC', '思域'),
        ('HONDA', 'ACCORD', '雅阁'),
        ('HONDA', 'CRV', 'CR-V'),
        ('VW', 'LAVIDA', '朗逸'),
        ('VW', 'SAGITAR', '速腾'),
        ('BMW', 'BMW3', '3系'),
        ('BMW', 'BMW5', '5系'),
        ('BENZ', 'BENZC', 'C级'),
        ('BENZ', 'BENZE', 'E级'),
        ('AUDI', 'A4L', 'A4L'),
        ('AUDI', 'A6L', 'A6L'),
        ('NISSAN', 'SYLPHY', '轩逸'),
        ('HYUNDAI', 'ELANTRA', '伊兰特'),
        ('KIA', 'K3', 'K3'),
        ('FORD', 'FOCUS', '福克斯')
    ) AS v(brand_code, code, name)
)
INSERT INTO erp_vehicle_series (tenant_id, brand_id, code, name, is_enabled, remark)
SELECT t.id, b.id, s.code, s.name, TRUE, 'seed'
FROM app_tenant t
JOIN series_seed s ON 1 = 1
JOIN erp_vehicle_brand b ON b.tenant_id = t.id AND b.code = s.brand_code;

-- 4) 插入车型
WITH model_seed AS (
    SELECT * FROM (VALUES
        ('TOYOTA', 'COROLLA', 'COROLLA_E140_2014_2018', '卡罗拉 2014-2018 1.6L', 2014, 2018, '1.6L', '1ZR-FE'),
        ('TOYOTA', 'COROLLA', 'COROLLA_E210_2019_2022', '卡罗拉 2019-2022 1.2T', 2019, 2022, '1.2T', '8NR-FTS'),
        ('TOYOTA', 'CAMRY', 'CAMRY_XV50_2015_2018', '凯美瑞 2015-2018 2.0L', 2015, 2018, '2.0L', '6AR-FSE'),
        ('TOYOTA', 'CAMRY', 'CAMRY_XV70_2018_2023', '凯美瑞 2018-2023 2.5L', 2018, 2023, '2.5L', 'A25A-FKS'),
        ('TOYOTA', 'RAV4', 'RAV4_XA40_2013_2018', 'RAV4荣放 2013-2018 2.0L', 2013, 2018, '2.0L', '3ZR-FAE'),
        ('HONDA', 'CIVIC', 'CIVIC_FK7_2016_2020', '思域 2016-2020 1.5T', 2016, 2020, '1.5T', 'L15B'),
        ('HONDA', 'ACCORD', 'ACCORD_10TH_2018_2022', '雅阁 2018-2022 1.5T', 2018, 2022, '1.5T', 'L15B'),
        ('HONDA', 'CRV', 'CRV_5TH_2017_2021', 'CR-V 2017-2021 1.5T', 2017, 2021, '1.5T', 'L15B'),
        ('VW', 'LAVIDA', 'LAVIDA_2013_2017', '朗逸 2013-2017 1.6L', 2013, 2017, '1.6L', 'EA211'),
        ('VW', 'SAGITAR', 'SAGITAR_2015_2019', '速腾 2015-2019 1.4T', 2015, 2019, '1.4T', 'EA211'),
        ('BMW', 'BMW3', 'BMW3_F30_2013_2018', '宝马3系 2013-2018 2.0T', 2013, 2018, '2.0T', 'N20'),
        ('BMW', 'BMW3', 'BMW3_G20_2019_2023', '宝马3系 2019-2023 2.0T', 2019, 2023, '2.0T', 'B48'),
        ('BMW', 'BMW5', 'BMW5_G30_2017_2021', '宝马5系 2017-2021 2.0T', 2017, 2021, '2.0T', 'B48'),
        ('BENZ', 'BENZC', 'BENZC_W205_2015_2018', '奔驰C级 2015-2018 2.0T', 2015, 2018, '2.0T', 'M274'),
        ('BENZ', 'BENZE', 'BENZE_W213_2017_2022', '奔驰E级 2017-2022 2.0T', 2017, 2022, '2.0T', 'M274'),
        ('AUDI', 'A4L', 'A4L_B8_2013_2016', '奥迪A4L 2013-2016 2.0T', 2013, 2016, '2.0T', 'EA888'),
        ('AUDI', 'A6L', 'A6L_C7_2016_2019', '奥迪A6L 2016-2019 2.0T', 2016, 2019, '2.0T', 'EA888'),
        ('NISSAN', 'SYLPHY', 'SYLPHY_B17_2016_2019', '轩逸 2016-2019 1.6L', 2016, 2019, '1.6L', 'HR16'),
        ('HYUNDAI', 'ELANTRA', 'ELANTRA_AD_2016_2019', '伊兰特 2016-2019 1.6L', 2016, 2019, '1.6L', 'Gamma'),
        ('KIA', 'K3', 'K3_2017_2020', '起亚K3 2017-2020 1.6L', 2017, 2020, '1.6L', 'Gamma'),
        ('FORD', 'FOCUS', 'FOCUS_2012_2018', '福克斯 2012-2018 1.6L', 2012, 2018, '1.6L', 'Sigma'),
        ('FORD', 'FOCUS', 'FOCUS_2019_2022', '福克斯 2019-2022 1.5T', 2019, 2022, '1.5T', 'EcoBoost')
    ) AS v(brand_code, series_code, code, name, year_from, year_to, displacement, engine)
)
INSERT INTO erp_vehicle_model (
    tenant_id,
    series_id,
    code,
    name,
    year_from,
    year_to,
    displacement,
    engine,
    is_enabled,
    remark
)
SELECT t.id,
       s.id,
       m.code,
       m.name,
       m.year_from,
       m.year_to,
       m.displacement,
       m.engine,
       TRUE,
       'seed'
FROM app_tenant t
JOIN model_seed m ON 1 = 1
JOIN erp_vehicle_brand b ON b.tenant_id = t.id AND b.code = m.brand_code
JOIN erp_vehicle_series s ON s.tenant_id = t.id AND s.brand_id = b.id AND s.code = m.series_code;

-- 5) 插入商品适配（按产品与车型顺序匹配，最多30条/租户）
WITH product_rows AS (
    SELECT id AS product_id,
           tenant_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS rn
    FROM erp_product
),
model_rows AS (
    SELECT id AS model_id,
           tenant_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS rn
    FROM erp_vehicle_model
)
INSERT INTO erp_product_fitment (tenant_id, product_id, model_id, remark)
SELECT p.tenant_id,
       p.product_id,
       m.model_id,
       'seed'
FROM product_rows p
JOIN model_rows m
  ON m.tenant_id = p.tenant_id
 AND m.rn = p.rn
WHERE p.rn <= 30;
