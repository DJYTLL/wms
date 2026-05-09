package com.example.wms.dto;

import java.util.List;

// JWT 中的用户对象载荷
public record UserClaim(String username, String role, String avatar, List<String> roles) {
}
