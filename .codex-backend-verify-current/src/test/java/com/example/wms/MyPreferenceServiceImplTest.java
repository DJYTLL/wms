package com.example.wms;

import com.example.wms.dto.EffectiveListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesUpdateRequest;
import com.example.wms.entity.UserAccount;
import com.example.wms.entity.UserTableSetting;
import com.example.wms.mapper.UserTableSettingMapper;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.service.TenantSettingService;
import com.example.wms.service.UserAccountService;
import com.example.wms.service.impl.MyPreferenceServiceImpl;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPreferenceServiceImplTest {
    @Mock private UserTableSettingMapper userTableSettingMapper;
    @Mock private UserAccountService userAccountService;
    @Mock private TenantSettingService tenantSettingService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void effectiveListPreferencePrefersUserSetting() {
        TenantContext.setTenantId(7L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of())
        );
        when(userAccountService.loadUserAccount("alice")).thenReturn(user("alice", 18L));
        when(userTableSettingMapper.findOne(7L, 18L, "my-list-preferences"))
            .thenReturn(setting("{\"pageSize\":50}"));

        MyPreferenceServiceImpl service = new MyPreferenceServiceImpl(
            userTableSettingMapper,
            userAccountService,
            tenantSettingService,
            new ObjectMapper()
        );
        EffectiveListPreferencesResponse result = service.getEffectiveListPreferences();

        assertThat(result.pageSize()).isEqualTo(50);
        assertThat(result.source()).isEqualTo("USER");
    }

    @Test
    void updateListPreferencesPersistsSpecialPreferencePageKey() {
        TenantContext.setTenantId(7L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of())
        );
        when(userAccountService.loadUserAccount("alice")).thenReturn(user("alice", 18L));
        when(userTableSettingMapper.findOne(7L, 18L, "my-list-preferences")).thenReturn(null);

        MyPreferenceServiceImpl service = new MyPreferenceServiceImpl(
            userTableSettingMapper,
            userAccountService,
            tenantSettingService,
            new ObjectMapper()
        );
        service.updateListPreferences(new MyListPreferencesUpdateRequest(100));

        ArgumentCaptor<UserTableSetting> captor = ArgumentCaptor.forClass(UserTableSetting.class);
        verify(userTableSettingMapper).insert(captor.capture());
        assertThat(captor.getValue().getPageKey()).isEqualTo("my-list-preferences");
        assertThat(captor.getValue().getConfigJson()).contains("\"pageSize\":100");
    }

    @Test
    void effectiveListPreferenceUsesAuthenticatedPrincipalWithoutReloadingUser() {
        TenantContext.setTenantId(7L);
        UserAccount user = user("alice", 18L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.fromDatabase(
                    user,
                    null,
                    List.of()
                ),
                "n/a",
                List.of()
            )
        );
        when(userTableSettingMapper.findOne(7L, 18L, "my-list-preferences"))
            .thenReturn(setting("{\"pageSize\":60}"));

        MyPreferenceServiceImpl service = new MyPreferenceServiceImpl(
            userTableSettingMapper,
            userAccountService,
            tenantSettingService,
            new ObjectMapper()
        );
        EffectiveListPreferencesResponse result = service.getEffectiveListPreferences();

        assertThat(result.pageSize()).isEqualTo(60);
        verifyNoInteractions(userAccountService);
    }

    private UserAccount user(String username, Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private UserTableSetting setting(String configJson) {
        UserTableSetting setting = new UserTableSetting();
        setting.setConfigJson(configJson);
        return setting;
    }
}
