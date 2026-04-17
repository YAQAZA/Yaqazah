package com.yaqazah.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userId;

    private String email;
    private String passwordHash;
    private String username;
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // --- Role-Specific Attributes ---

    // Used by both COMPANYADMIN and DRIVER. Will be null for standard ADMIN.
    private UUID companyId;

    // Used only by DRIVER.
    // Using object 'Boolean' instead of primitive 'boolean' so it can be null for non-drivers.
    private Boolean isFleetDriver;

    // You can keep your relationships here too, just make sure your logic
    // only populates this if the Role == DRIVER
    // @OneToMany(mappedBy = "driver")
    // private List<Session> sessions;
}