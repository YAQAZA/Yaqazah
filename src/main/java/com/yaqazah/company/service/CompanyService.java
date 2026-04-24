package com.yaqazah.company.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Transactional
    public Company createCompany(Company company) {
        // Business Rule: Ensure company name is unique
        if (companyRepository.existsByNameIgnoreCase(company.getName())) {
            throw new IllegalArgumentException("A company with this name already exists.");
        }

        // Set the creation timestamp if it's missing
        if (company.getCreatedAt() == null || company.getCreatedAt().isEmpty()) {
            company.setCreatedAt(LocalDateTime.now().toString());
        }

        // Save and return the generated entity (including the UUID)
        return companyRepository.save(company);
    }

    public Optional<Company> getCompanyById(UUID companyId) {
        return companyRepository.findById(companyId);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Transactional
    public void deleteCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found.");
        }
        companyRepository.deleteById(companyId);
    }
}