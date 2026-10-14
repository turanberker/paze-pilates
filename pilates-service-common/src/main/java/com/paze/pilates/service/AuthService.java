package com.paze.pilates.service;

import com.paze.pilates.dto.AppUserDto;
import java.util.Optional;

public interface AuthService {

    boolean authenticate(String username, String password);

    Optional<AppUserDto> getCurrentUser();

    void logout();
}
