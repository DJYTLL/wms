package com.example.wms.service;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.UserCreateRequest;
import com.example.wms.dto.UserPasswordChangeRequest;
import com.example.wms.dto.UserPasswordResetRequest;
import com.example.wms.dto.UserResponse;
import com.example.wms.dto.UserRoleUpdateRequest;
import com.example.wms.dto.UserStatusUpdateRequest;
import com.example.wms.dto.UserUpdateRequest;
import com.example.wms.dto.RoleOptionResponse;
import com.example.wms.entity.Role;

import java.util.List;

// 用户管理服务接口
public interface UserService {
    // 查询用户列表
    List<UserResponse> listAll();

    // 分页查询用户列表
    PageResponse<UserResponse> page(long page, long size, String keyword, Boolean enabled);

    // 查询用户详情
    UserResponse getById(Long id);

    // 新增用户
    UserResponse create(UserCreateRequest request);

    // 更新用户
    UserResponse update(Long id, UserUpdateRequest request);

    // 更新用户状态
    void updateStatus(Long id, UserStatusUpdateRequest request);

    // 删除用户
    void delete(Long id);

    // 修改密码
    void changePassword(Long id, UserPasswordChangeRequest request);

    // 重置密码
    void resetPassword(Long id, UserPasswordResetRequest request);

    // 查询用户角色
    List<Role> listRoles(Long id);

    // 查询角色下拉选项
    List<RoleOptionResponse> listRoleOptions();

    // 设置用户角色
    void setRoles(Long id, UserRoleUpdateRequest request);
}
