import { ref } from 'vue';
import { defineStore } from 'pinia';

// 定义权限接口
export interface Permission {
  id: number;
  code: string;       // 权限标识 (如 user:view)
  name: string;       // 权限名称 (如 查看用户)
  group: string;      // 权限分组 (如 用户管理)
}

// 定义角色接口
export interface Role {
  id: number;
  name: string;       // 角色名称 (如 管理员)
  code: string;       // 角色标识 (如 admin)
  description: string;
  permissionIds: number[]; // 关联的权限ID列表
}

// 定义系统用户接口
export interface SysUser {
  id: number;
  username: string;
  name: string;
  roleIds: number[];  // 关联的角色ID列表
  status: 'active' | 'inactive';
  lastLogin?: string;
}

export const useMockDataStore = defineStore('mockData', () => {
  // --- Units (计量单位) ---
  const units = ref([
    { id: 1, name: 'Kilogram', symbol: 'kg', status: 'active' },
    { id: 2, name: 'Meter', symbol: 'm', status: 'active' },
    { id: 3, name: 'Piece', symbol: 'pcs', status: 'active' },
    { id: 4, name: 'Box', symbol: 'box', status: 'active' },
  ]);

  // --- Warehouses (仓库) ---
  const warehouses = ref([
    { id: 1, name: 'Main Warehouse', code: 'WH-001', address: '123 Main St', status: 'active' },
    { id: 2, name: 'East Depot', code: 'WH-002', address: '456 East Ave', status: 'active' },
  ]);

  // --- Shelves (货架) ---
  const shelves = ref([
    { id: 1, name: 'Shelf A1', code: 'S-A1-001', capacity: 500, status: 'active', warehouseId: 1 },
    { id: 2, name: 'Shelf A2', code: 'S-A2-002', capacity: 500, status: 'active', warehouseId: 1 },
    { id: 3, name: 'Shelf B1', code: 'S-B1-001', capacity: 300, status: 'active', warehouseId: 2 },
  ]);

  // --- Products (商品) ---
  const products = ref([
    { id: 1, name: 'Brake Pad', sku: 'BP-001', price: 45.00, unitId: 3, shelfId: 1, categoryId: 1, status: 'active' },
    { id: 2, name: 'Oil Filter', sku: 'OF-202', price: 12.50, unitId: 3, shelfId: 2, categoryId: 1, status: 'active' },
    { id: 3, name: 'Engine Oil', sku: 'EO-5L', price: 30.00, unitId: 4, shelfId: 3, categoryId: 1, status: 'active' },
  ]);

  // --- Suppliers (供应商) ---
  const suppliers = ref([
    { id: 1, name: 'AutoParts Inc.', contactPerson: 'John', phone: '1234567890', email: 'john@api.com', address: '123 Ind. Rd', status: 'active' },
  ]);

  // --- Categories (分类) ---
  const categories = ref([
    { id: 1, name: 'Engine Parts', code: 'CAT-ENG', description: 'Engine components', status: 'active' },
  ]);

  // --- Inbound Orders (入库单) ---
  const inboundOrders = ref([
    { 
      id: 1, 
      orderNumber: 'IN-20231001-001', 
      type: 'purchase', 
      status: 'received', 
      date: '2023-10-01 10:30:00', 
      supplierId: 1,
      items: [
        { productId: 1, quantity: 50, warehouseId: 1, shelfId: 1 },
        { productId: 2, quantity: 100, warehouseId: 1, shelfId: 2 }
      ]
    },
    { 
      id: 2, 
      orderNumber: 'IN-20231005-002', 
      type: 'return', 
      status: 'pending', 
      date: '2023-10-05 14:15:20', 
      supplierId: 1,
      items: [
        { productId: 3, quantity: 20, warehouseId: 2, shelfId: 3 }
      ]
    }
  ]);

  // === 系统管理 Mock 数据 ===

  // --- Permissions (权限) ---
  // 初始化一些基础权限
  const permissions = ref<Permission[]>([
    // 仓库相关
    { id: 1, code: 'warehouse:view', name: 'View Warehouse', group: 'Warehouse' },
    { id: 2, code: 'warehouse:add', name: 'Add Warehouse', group: 'Warehouse' },
    { id: 3, code: 'warehouse:edit', name: 'Edit Warehouse', group: 'Warehouse' },
    { id: 4, code: 'warehouse:delete', name: 'Delete Warehouse', group: 'Warehouse' },
    // 入库相关
    { id: 5, code: 'inbound:view', name: 'View Inbound', group: 'Inbound' },
    { id: 6, code: 'inbound:add', name: 'Add Inbound', group: 'Inbound' },
    { id: 7, code: 'inbound:edit', name: 'Edit Inbound', group: 'Inbound' },
    { id: 8, code: 'inbound:delete', name: 'Delete Inbound', group: 'Inbound' },
    // 系统用户相关
    { id: 9, code: 'user:view', name: 'View User', group: 'System' },
    { id: 10, code: 'user:add', name: 'Add User', group: 'System' },
    { id: 11, code: 'user:edit', name: 'Edit User', group: 'System' },
    { id: 12, code: 'user:delete', name: 'Delete User', group: 'System' },
    // 角色相关
    { id: 13, code: 'role:view', name: 'View Role', group: 'System' },
    { id: 14, code: 'role:add', name: 'Add Role', group: 'System' },
    { id: 15, code: 'role:edit', name: 'Edit Role', group: 'System' },
    { id: 16, code: 'role:delete', name: 'Delete Role', group: 'System' },
  ]);

  // --- Roles (角色) ---
  const roles = ref<Role[]>([
    { 
      id: 1, 
      name: 'Administrator', 
      code: 'admin', 
      description: 'Super user with full access', 
      permissionIds: [1,2,3,4, 5,6,7,8, 9,10,11,12, 13,14,15,16] // 拥有所有权限
    },
    { 
      id: 2, 
      name: 'Warehouse Keeper', 
      code: 'keeper', 
      description: 'Manage warehouse operations', 
      permissionIds: [1, 5,6,7] // 仅查看仓库，操作入库
    }
  ]);

  // --- System Users (系统用户) ---
  const sysUsers = ref<SysUser[]>([
    { 
      id: 1, 
      username: 'admin', 
      name: 'Super Admin', 
      roleIds: [1], // 关联 Admin 角色
      status: 'active',
      lastLogin: '2023-11-10 09:00:00'
    },
    { 
      id: 2, 
      username: 'worker1', 
      name: 'John Doe', 
      roleIds: [2], // 关联 Warehouse Keeper 角色
      status: 'active',
      lastLogin: '2023-11-09 15:30:00'
    }
  ]);

  return {
    units,
    warehouses,
    shelves,
    products,
    suppliers,
    categories,
    inboundOrders,
    // System Data
    permissions,
    roles,
    sysUsers
  };
});