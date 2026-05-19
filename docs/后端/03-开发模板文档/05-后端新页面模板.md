# 新增页面模板（后端示例：品牌管理）

本模板可直接复制修改，目标是新增 `/api/brands` 的增删改查与分页接口，并配套权限与菜单种子。

## 1) 数据库表（租户级）
```sql
CREATE TABLE IF NOT EXISTS app_brand (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(100) NOT NULL,
  name VARCHAR(200) NOT NULL,
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (tenant_id, code)
);
```

## 2) 权限与菜单种子
- 权限：`brand:view`、`brand:add`、`brand:edit`、`brand:delete`
- 菜单（父级为 `basic`）：
  - `code`: `brand`
  - `i18nKey`: `nav.brand`
  - `path`: `/basic/brand-management`
  - `permissionCode`: `brand:view`

## 3) DTO 示例
```java
public record BrandCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled
) {}

public record BrandUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    Boolean enabled
) {}

public record BrandResponse(
    Long id,
    String code,
    String name,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
```

## 4) Controller 示例
```java
@RestController
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_brand:view')")
    public ResponseEntity<ApiResponse<PageResponse<BrandResponse>>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.page(page, size, keyword, enabled)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_brand:add')")
    public ResponseEntity<ApiResponse<BrandResponse>> create(@Valid @RequestBody BrandCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_brand:edit')")
    public ResponseEntity<ApiResponse<BrandResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody BrandUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_brand:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

## 5) Service 示例（租户隔离 + 审计）
```java
@Service
public class BrandServiceImpl implements BrandService {
    private final BrandMapper brandMapper;

    public BrandServiceImpl(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @Override
    public PageResponse<BrandResponse> page(long page, long size, String keyword, Boolean enabled) {
        Long tenantId = TenantContext.requireTenantId();
        QueryWrapper<Brand> wrapper = new QueryWrapper<Brand>()
            .eq("tenant_id", tenantId)
            .orderByAsc("id");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword).or().like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        Page<Brand> result = brandMapper.selectPage(Page.of(page, size), wrapper);
        List<BrandResponse> items = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    @Override
    @AuditLog(action = "BRAND_CREATE", entityType = "brand", entityId = "{result.id}", detail = "code={arg0.code}")
    public BrandResponse create(BrandCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Brand brand = new Brand();
        brand.setTenantId(tenantId);
        brand.setCode(request.code());
        brand.setName(request.name());
        brand.setEnabled(request.enabled() == null || request.enabled());
        brand.setCreatedAt(Instant.now());
        brand.setUpdatedAt(Instant.now());
        brandMapper.insert(brand);
        return toResponse(brand);
    }
}
```

## 6) 必做清单
- 权限种子 + 菜单种子写入后重启补齐数据。
- Controller 添加 `@PreAuthorize`（注意 `PERM_` 前缀）。
- Service 层使用 `TenantContext` 做租户隔离。
- 新增/编辑/删除增加 `@AuditLog`。
