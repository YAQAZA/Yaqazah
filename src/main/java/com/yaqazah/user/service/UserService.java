package com.yaqazah.user.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.detection.repository.DetectionLogRepository;
import com.yaqazah.session.repository.SessionRepository;
import com.yaqazah.user.dto.response.AdminListDto;
import com.yaqazah.user.dto.response.CompanyInfoDto;
import com.yaqazah.user.dto.response.UserProfileResponseDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.RefreshTokenRepository;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    // --- REPOSITORIES & DEPENDENCIES ---
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CompanyRepository companyRepository;
    private final DetectionLogRepository detectionLogRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

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
        User user = findByEmail(email);

        UserProfileResponseDto response = new UserProfileResponseDto();
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setGender(user.getGender().name());
        response.setRole(user.getRole().name());
        response.setBirthDate(user.getBirthDate());

        return response;
    }

    // ========================================================================
    // 1. SOFT DELETE (Called immediately when user clicks "Delete My Account")
    // ========================================================================
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setDeleted(true);
        user.setDeletedAt(Instant.now());

        switch (user.getRole()) {
            case INDEPENDENT_DRIVER:
            case FLEET_DRIVER:
                userRepository.save(user);
                break;

            case ADMIN:
                long totalAdmins = userRepository.countByRole(Role.ADMIN);

                // Promote the oldest company admin if there is one
                if (totalAdmins <= 1) {
                    Optional<User> oldestCompanyAdminOpt = userRepository
                            .findFirstByRoleOrderByInsertedAtAsc(Role.COMPANY_ADMIN);

                    if (oldestCompanyAdminOpt.isPresent()) {
                        User oldestCompanyAdmin = oldestCompanyAdminOpt.get();
                        oldestCompanyAdmin.setRole(Role.ADMIN);
                        userRepository.save(oldestCompanyAdmin);
                    }
                }
                // INTENTIONAL FALL-THROUGH: Drop directly into COMPANY_ADMIN logic
                // to soft-delete their company and drivers!

            case COMPANY_ADMIN:
                Company company = user.getCompany();
                if (company != null) {
                    UUID cid = company.getCompanyId();

                    // Count how many leaders (Admins or Company Admins) this company has left
                    int companyLeaderCount = userRepository.countByCompany_CompanyIdAndRoleIn(
                            cid, List.of(Role.ADMIN, Role.COMPANY_ADMIN)
                    );

                    if (companyLeaderCount <= 1) {
                        // Soft delete all fleet drivers for this company
                        List<User> fleetDrivers = userRepository.findByCompany_CompanyIdAndRole(cid, Role.FLEET_DRIVER);
                        for (User driver : fleetDrivers) {
                            driver.setDeleted(true);
                            driver.setDeletedAt(Instant.now());
                            userRepository.save(driver);
                        }
                        // Soft delete the company itself
                        company.setDeleted(true);
                        company.setDeletedAt(Instant.now());
                        companyRepository.save(company);
                    }
                }
                userRepository.save(user);
                break;

            default:
                throw new IllegalStateException("Deletion not permitted for role: " + user.getRole());
        }
    }

    // ========================================================================
    // 2. HARD DELETE (Called by your Scheduled Cleanup Service after 30 days)
    // ========================================================================
    @Transactional
    public void hardDeleteAccount(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return; // User is already permanently gone

        switch (user.getRole()) {
            case INDEPENDENT_DRIVER:
            case FLEET_DRIVER:
                deleteUserAssociatedData(user);
                userRepository.delete(user);
                break;

            case ADMIN:
            case COMPANY_ADMIN:
                Company company = user.getCompany();
                if (company != null) {
                    UUID cid = company.getCompanyId();

                    // Count how many leaders are left
                    int companyLeaderCount = userRepository.countByCompany_CompanyIdAndRoleIn(
                            cid, List.of(Role.ADMIN, Role.COMPANY_ADMIN)
                    );

                    if (companyLeaderCount <= 1) {
                        // 1. Permanently delete all drivers
                        List<User> fleetDrivers = userRepository.findByCompany_CompanyIdAndRole(cid, Role.FLEET_DRIVER);
                        for (User driver : fleetDrivers) {
                            deleteUserAssociatedData(driver);
                            userRepository.delete(driver);
                        }
                        // 2. Permanently delete this admin
                        deleteUserAssociatedData(user);
                        userRepository.delete(user);
                        // 3. Permanently delete the company
                        companyRepository.delete(company);
                    } else {
                        deleteUserAssociatedData(user);
                        userRepository.delete(user);
                    }
                } else {
                    deleteUserAssociatedData(user);
                    userRepository.delete(user);
                }
                break;
        }
    }

    /**
     * Helper method to safely delete all child records before deleting the user.
     * Order matters to prevent Foreign Key constraint violations.
     */
    private void deleteUserAssociatedData(User user) {
        refreshTokenRepository.deleteByUser(user);
        detectionLogRepository.deleteByUser(user);
        sessionRepository.deleteByUserId(user.getUserId());
    }

    // ========================================================================
    // 3. RESTORE ACCOUNT (First-Come, First-Serve Admin Assignment)
    // ========================================================================
    @Transactional
    public void restoreAccount(String email, String password) {
        // 1. Find the user by email including deleted ones
        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // 2. Make sure they are actually deleted
        if (!user.isDeleted()) {
            throw new IllegalStateException("Account is already active.");
        }

        // 3. Check the password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new SecurityException("Invalid credentials.");
        }

        // 4. Restore the user's base properties
        user.setDeleted(false);
        user.setDeletedAt(null);

        // 5. Handle Company Leaders (Admin & Company Admin)
        if (user.getRole() == Role.COMPANY_ADMIN || user.getRole() == Role.ADMIN) {
            Company company = user.getCompany();

            if (company != null) {
                // Restore the company and drivers if the company was deleted
                if (company.isDeleted()) {
                    company.setDeleted(false);
                    company.setDeletedAt(null);
                    companyRepository.save(company);

                    List<User> fleetDrivers = userRepository.findByCompany_CompanyIdAndRole(
                            company.getCompanyId(), Role.FLEET_DRIVER);

                    for (User driver : fleetDrivers) {
                        driver.setDeleted(false);
                        driver.setDeletedAt(null);
                        userRepository.save(driver);
                    }
                }

                // --- DYNAMIC ROLE ASSIGNMENT ---
                // Check if the company currently has an active ADMIN
                boolean activeAdminExists = userRepository.existsByCompany_CompanyIdAndRoleAndIsDeletedFalse(
                        company.getCompanyId(), Role.ADMIN
                );

                if (!activeAdminExists) {
                    // No active ADMIN exists -> The first leader back gets the crown
                    user.setRole(Role.ADMIN);
                } else {
                    // An active ADMIN already exists -> Anyone else comes back as a COMPANY_ADMIN
                    user.setRole(Role.COMPANY_ADMIN);
                }
            }
        }

        userRepository.save(user);
    }

    // ========================================================================
    // GET COMPANY ADMINS
    // ========================================================================
    public List<AdminListDto> getCompanyAdmins(String requesterEmail) {
        User requester = findByEmail(requesterEmail);

        if (requester.getCompany() == null) {
            throw new IllegalStateException("User is not associated with any company.");
        }

        // Fetch users in the same company who are either ADMIN or COMPANY_ADMIN
        List<User> admins = userRepository.findByCompany_CompanyIdAndRoleIn(
                requester.getCompany().getCompanyId(),
                List.of(Role.ADMIN, Role.COMPANY_ADMIN)
        );

        return admins.stream()
                .map(user -> AdminListDto.builder()
                        .name(user.getFullName())
                        .email(user.getEmail())
                        // Note: If your User entity uses a different field name for the creation date
                        // (like getCreatedAt() or getJoinedAt()), update getInsertedAt() below to match.
                        .insertedAt(user.getInsertedAt())
                        .role(user.getRole().name())
                        .build())
                .toList();
    }

    // ========================================================================
    // GET COMPANY INFO
    // ========================================================================
    public CompanyInfoDto getCompanyInfo(String requesterEmail) {
        // 1. Get the logged-in user and their company
        User requester = findByEmail(requesterEmail);
        Company company = requester.getCompany();

        if (company == null) {
            throw new IllegalStateException("User is not associated with any company.");
        }

        UUID companyId = company.getCompanyId();

        // 2. Count the total number of admins (ADMIN + COMPANY_ADMIN)
        int totalAdmins = userRepository.countByCompany_CompanyIdAndRoleIn(
                companyId,
                List.of(Role.ADMIN, Role.COMPANY_ADMIN)
        );

        // 3. Find the single primary ADMIN for this company
        User primaryAdmin = userRepository.findByCompany_CompanyIdAndRole(companyId, Role.ADMIN)
                .stream()
                .findFirst() // Grabs the first item from the List (returns an Optional)
                .orElseThrow(() -> new IllegalStateException("Company has no primary ADMIN (Owner)."));

        // 4. Map everything to the DTO
        return CompanyInfoDto.builder()
                .companyName(company.getName())
//                .companyAddress(company.getAddress())
                .totalAdmins(totalAdmins)
                // Note: If your Company entity uses getCreatedAt(), change this to match
                .companyInsertedAt(company.getInsertedAt())

                .adminName(primaryAdmin.getFullName())
                .adminEmail(primaryAdmin.getEmail())
                .build();
    }
}