package com.yanis.auth_service.adapter.out.persistence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.yanis.auth_service.domain.model.User;
import com.yanis.auth_service.domain.port.out.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .roles(String.join(",", user.getRoles()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .roles(new HashSet<>(Arrays.asList(entity.getRoles().split(","))))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
