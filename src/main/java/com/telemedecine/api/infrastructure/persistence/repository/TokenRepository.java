package com.telemedecine.api.infrastructure.persistence.repository;

import com.telemedecine.api.domain.token.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {


    @Query("""
        select t from Token t
        inner join UserEntity u on t.user.id = u.id
        where u.id = :userId AND (t.expired = false AND t.revoked = false)
    """)
    List<Token> findAllValidTokensByUser(Long userId);

    Optional<Token> findByToken(String token);
}
