package com.example.wms.service;

import com.example.wms.dto.EffectiveListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesUpdateRequest;

public interface MyPreferenceService {
    MyListPreferencesResponse getListPreferences();

    MyListPreferencesResponse updateListPreferences(MyListPreferencesUpdateRequest request);

    EffectiveListPreferencesResponse getEffectiveListPreferences();
}
