package com.yaqazah.company.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID companyId;
    private String name;
    private String address;
    private String createdAt;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isDeleted = false;
    private Instant deletedAt = null;
}