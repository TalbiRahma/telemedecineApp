package com.telemedecine.api.dao;

import com.telemedecine.api.model.token.PasswordResetToken;
import com.telemedecine.api.model.user.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findTopByUserOrderByCreatedAtDesc(UserEntity user);

    @Modifying
    @Query("update PasswordResetToken token set token.usedAt = :usedAt "
            + "where token.user = :user and token.usedAt is null")
    int markAllUnusedByUserAsUsed(UserEntity user, java.time.Instant usedAt);
}
