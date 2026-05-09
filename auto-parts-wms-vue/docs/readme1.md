建议的 JWT 载荷 (Payload) 格式

后端在生成 Token 时，应将以下信息放入 JWT 的 Claims（载荷）中：

    1 {
    2   "user": {
    3     "username": "admin",
    4     "role": "admin",
    5     "avatar": "https://example.com/avatar.png"
    6   },
    7   "permissions": [
    8     "warehouse:view",
    9     "warehouse:add",

10 "product:view",
11 "product:edit",
12 "supplier:delete"
13 ],
14 "exp": 1735689600
15 }

各字段在前端代码中的作用：

1.  `user` (用户对象):
    - username: 会被显示在 MainLayout.vue 的顶部欢迎语中。
    - role: 目前前端逻辑主要用于标识，你也可以根据它来显示/隐藏特定的 UI 组件。

2.  `permissions` (权限数组):
    - 核心功能：这是系统安全性的关键。
    - `v-permission` 指令：如果在 HTML 中写了 <button v-permission="'product:add'">，前端会检查这个数组里是否包含
      'product:add'。如果不包含，按钮会被自动从页面上删除。
    - 路由守卫：可以根据此数组判断用户是否有权访问某个菜单路径。

3.  `exp` (过期时间):
    - 这是一个 Unix 时间戳（秒）。
    - 当前前端逻辑在 auth.ts 中解析它。虽然目前没有写自动过期跳转逻辑，但你可以利用它来判断 Token 是否失效，如果失效则清空 localStorage
      并跳转回登录页。

后端如何返回？

后端登录接口（例如 /api/login）成功后，应该返回如下结构：

1 {
2 "code": 200,
3 "message": "登录成功",
4 "data": {
5 "token":
"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjp7InVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiYWRtaW4ifSwicGVybWlzc2lvbnMiOlsid2FyZWhvdXNlOnZpZXciL
wcm9kdWN0OmFkZCJdLCJleHAiOjE3MzU2ODk2MDB9.xxxxxx"
6 }
7 }

     1. 角色 (Roles)

目前系统中只硬编码定义了一个模拟角色：

- `admin` (超级管理员)

---

2. 权限权利 (Permissions)
   权限采用 资源:操作 的格式定义。以下是代码中涉及的所有权限，它们在登录时被全量授予给 admin 用户：

仓库管理 (Warehouse)

- warehouse:view: 查看仓库列表
- warehouse:add: 新增仓库
- warehouse:edit: 编辑仓库信息
- warehouse:delete: 删除仓库

货架管理 (Shelf)

- shelf:view: 查看货架
- shelf:add: 新增货架
- shelf:edit: 编辑货架
- shelf:delete: 删除货架

商品管理 (Product)

- product:view: 查看商品信息
- product:add: 新增商品
- product:edit: 编辑商品
- product:delete: 删除商品

供应商管理 (Supplier)

- supplier:view: 查看供应商
- supplier:add: 新增供应商
- supplier:edit: 编辑供应商
- supplier:delete: 删除供应商

商品分类管理 (Category)

- category:view: 查看分类
- category:add: 新增分类
- category:edit: 编辑分类
- category:delete: 删除分类

计量单位管理 (Unit)

- unit:view: 查看单位
- unit:add: 新增单位
- unit:edit: 编辑单位
- unit:delete: 删除单位
