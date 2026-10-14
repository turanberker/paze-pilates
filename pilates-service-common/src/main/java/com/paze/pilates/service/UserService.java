package com.paze.pilates.service;

import com.paze.pilates.dto.UserDto;
import com.paze.pilates.dto.UserRole;
import java.util.List;

public interface UserService {

    /** Aktif ve pasif tüm kullanıcılar (sabit admin hariç), kullanıcı adına göre. */
    List<UserDto> findAll();

    /**
     * Geçici şifreli kullanıcı açar; ilk girişte şifre değiştirmesi zorunludur.
     * Hoca rolünde {@code instructorId} zorunludur ve ad soyad hoca kaydından alınır;
     * bir hocanın tek kullanıcısı olur. Yönetici rolünde {@code fullName} zorunludur.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama hatasında
     */
    UserDto createUser(String username, String fullName, UserRole role, Long instructorId,
                       String temporaryPassword);

    /** Şifreyi geçici şifreyle değiştirir; kullanıcı ilk girişte yenisini belirler. */
    UserDto resetPassword(Long userId, String temporaryPassword);

    UserDto activate(Long userId);

    /** Son aktif yönetici ve kendi hesabı pasife alınamaz. */
    UserDto deactivate(Long userId);

    /** Son aktif yöneticinin rolü düşürülemez; hoca rolü için hoca kaydı gerekir. */
    UserDto changeRole(Long userId, UserRole role, Long instructorId);
}
