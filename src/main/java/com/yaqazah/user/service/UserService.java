package com.yaqazah.user.service;

import com.yaqazah.user.dto.UserProfileResponseDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional
    public void updateUserName(String email, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }

        User user = findByEmail(email);
        user.setFullName(newName);
        userRepository.save(user);
    }

    public UserProfileResponseDto getUserProfileDto(String email) {
        // 1. Fetch the raw entity using your existing method
        User user = findByEmail(email);

        // 2. Map the entity to the DTO
        UserProfileResponseDto response = new UserProfileResponseDto();
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());

        if (user.getGender() != null) {
            response.setGender(user.getGender().name());
        }
        response.setRole(user.getRole().name());

        // 3. Return the safe DTO
        return response;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        switch (user.getRole()) {
            case INDEPENDENT_DRIVER:
            case ADMIN:
                userRepository.delete(user);
                break;

            case COMPANY_ADMIN:
                UUID cid = user.getCompany().getCompanyId();
                int adminCount = userRepository.countByCompany_CompanyIdAndRole(cid, Role.COMPANY_ADMIN);

                if (adminCount <= 1) {
                    userRepository.deleteByCompany_CompanyIdAndRole(cid, Role.FLEET_DRIVER);
                }
                userRepository.delete(user);
                break;

            default:
                throw new IllegalStateException("Deletion not permitted for role: " + user.getRole());
        }
    }
}