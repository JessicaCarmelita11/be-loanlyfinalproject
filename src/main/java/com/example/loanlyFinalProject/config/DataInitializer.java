package com.example.loanlyFinalProject.config;

import com.example.loanlyFinalProject.entity.Permission;
import com.example.loanlyFinalProject.entity.Plafond;
import com.example.loanlyFinalProject.entity.Role;
import com.example.loanlyFinalProject.entity.TenorRate;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.repository.PermissionRepository;
import com.example.loanlyFinalProject.repository.PlafondRepository;
import com.example.loanlyFinalProject.repository.RoleRepository;
import com.example.loanlyFinalProject.repository.TenorRateRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;
  private final PlafondRepository plafondRepository;
  private final TenorRateRepository tenorRateRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    initializePermissions();
    initializeRoles();
    initializeSuperAdmin();
    initializeMarketing();
    initializeBranchManager();
    initializeBackOffice();
    initializePlafonds();
    initializeTenorRates();
  }

  private void initializePermissions() {
    List<String[]> permissionData =
        Arrays.asList(
            // User management permissions
            new String[] {"USER_READ", "Read user data"},
            new String[] {"USER_CREATE", "Create new users"},
            new String[] {"USER_UPDATE", "Update user data"},
            new String[] {"USER_DELETE", "Delete users"},

            // Plafond permissions
            new String[] {"PLAFOND_READ", "Read plafond data"},
            new String[] {"PLAFOND_CREATE", "Create new plafond"},
            new String[] {"PLAFOND_UPDATE", "Update plafond"},
            new String[] {"PLAFOND_DELETE", "Delete plafond"},

            // Loan permissions
            new String[] {"LOAN_CREATE", "Create loan application"},
            new String[] {"LOAN_READ", "Read loan applications"},
            new String[] {"LOAN_REVIEW", "Review loan applications (Marketing)"},
            new String[] {"LOAN_APPROVE", "Approve loan applications (Branch Manager)"},
            new String[] {"LOAN_DISBURSE", "Disburse loans (Back Office)"},

            // Role management permissions
            new String[] {"ROLE_READ", "Read roles"},
            new String[] {"ROLE_CREATE", "Create roles"},
            new String[] {"ROLE_UPDATE", "Update roles"},
            new String[] {"ROLE_DELETE", "Delete roles"});

    for (String[] data : permissionData) {
      if (!permissionRepository.existsByCode(data[0])) {
        Permission permission = Permission.builder().code(data[0]).description(data[1]).build();
        permissionRepository.save(permission);
        log.info("Created permission: {}", data[0]);
      }
    }
  }

  private void initializeRoles() {
    Map<String, List<String>> rolePermissions = new LinkedHashMap<>();

    // Super Admin - all permissions
    rolePermissions.put(
        "SUPER_ADMIN",
        Arrays.asList(
            "USER_READ",
            "USER_CREATE",
            "USER_UPDATE",
            "USER_DELETE",
            "PLAFOND_READ",
            "PLAFOND_CREATE",
            "PLAFOND_UPDATE",
            "PLAFOND_DELETE",
            "LOAN_CREATE",
            "LOAN_READ",
            "LOAN_REVIEW",
            "LOAN_APPROVE",
            "LOAN_DISBURSE",
            "ROLE_READ",
            "ROLE_CREATE",
            "ROLE_UPDATE",
            "ROLE_DELETE"));

    // Marketing - review loans
    rolePermissions.put(
        "MARKETING", Arrays.asList("USER_READ", "PLAFOND_READ", "LOAN_READ", "LOAN_REVIEW"));

    // Branch Manager - approve loans
    rolePermissions.put(
        "BRANCH_MANAGER", Arrays.asList("USER_READ", "PLAFOND_READ", "LOAN_READ", "LOAN_APPROVE"));

    // Back Office - disburse loans
    rolePermissions.put(
        "BACK_OFFICE", Arrays.asList("USER_READ", "PLAFOND_READ", "LOAN_READ", "LOAN_DISBURSE"));

    // Customer - create and read own loans
    rolePermissions.put("CUSTOMER", Arrays.asList("PLAFOND_READ", "LOAN_CREATE", "LOAN_READ"));

    for (Map.Entry<String, List<String>> entry : rolePermissions.entrySet()) {
      String roleName = entry.getKey();
      List<String> permissionCodes = entry.getValue();

      if (!roleRepository.existsByName(roleName)) {
        Role role =
            Role.builder()
                .name(roleName)
                .description("Role for " + roleName.replace("_", " "))
                .build();

        Set<Permission> permissions = new HashSet<>();
        for (String code : permissionCodes) {
          permissionRepository.findByCode(code).ifPresent(permissions::add);
        }
        role.setPermissions(permissions);

        roleRepository.save(role);
        log.info("Created role: {} with {} permissions", roleName, permissions.size());
      }
    }
  }

  private void initializeSuperAdmin() {
    if (!userRepository.existsByUsername("superadmin")) {
      Role superAdminRole =
          roleRepository
              .findByName("SUPER_ADMIN")
              .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

      User superAdmin =
          User.builder()
              .username("superadmin")
              .email("superadmin@loanbankingsystem.com")
              .password(passwordEncoder.encode("Admin@123"))
              .fullName("Super Administrator")
              .isActive(true)
              .build();

      superAdmin.getRoles().add(superAdminRole);
      userRepository.save(superAdmin);

      log.info("Created super admin user: superadmin / Admin@123");
    }
  }

  private void initializeMarketing() {
    if (!userRepository.existsByUsername("marketing")) {
      Role marketingRole =
          roleRepository
              .findByName("MARKETING")
              .orElseThrow(() -> new RuntimeException("MARKETING role not found"));

      User marketing =
          User.builder()
              .username("marketing")
              .email("marketing@loanbankingsystem.com")
              .password(passwordEncoder.encode("Marketing@123"))
              .fullName("Marketing Staff")
              .isActive(true)
              .build();

      marketing.getRoles().add(marketingRole);
      userRepository.save(marketing);

      log.info("Created marketing user: marketing / Marketing@123");
    }
  }

  private void initializeBranchManager() {
    if (!userRepository.existsByUsername("branchmanager")) {
      Role branchManagerRole =
          roleRepository
              .findByName("BRANCH_MANAGER")
              .orElseThrow(() -> new RuntimeException("BRANCH_MANAGER role not found"));

      User branchManager =
          User.builder()
              .username("branchmanager")
              .email("branchmanager@loanbankingsystem.com")
              .password(passwordEncoder.encode("BranchManager@123"))
              .fullName("Branch Manager")
              .isActive(true)
              .build();

      branchManager.getRoles().add(branchManagerRole);
      userRepository.save(branchManager);

      log.info("Created branch manager user: branchmanager / BranchManager@123");
    }
  }

  private void initializeBackOffice() {
    if (!userRepository.existsByUsername("backoffice")) {
      Role backOfficeRole =
          roleRepository
              .findByName("BACK_OFFICE")
              .orElseThrow(() -> new RuntimeException("BACK_OFFICE role not found"));

      User backOffice =
          User.builder()
              .username("backoffice")
              .email("backoffice@loanbankingsystem.com")
              .password(passwordEncoder.encode("BackOffice@123"))
              .fullName("Back Office Staff")
              .isActive(true)
              .build();

      backOffice.getRoles().add(backOfficeRole);
      userRepository.save(backOffice);

      log.info("Created back office user: backoffice / BackOffice@123");
    }
  }

  private void initializePlafonds() {
    if (plafondRepository.count() > 0) {
      log.info("Plafond data already exists");
      return;
    }

    List<Plafond> plafonds = new ArrayList<>();

    // 2. Plus
    plafonds.add(
        Plafond.builder()
            .name("Plus")
            .description("Plafond Plus dengan limit maksimal Rp 5.000.000")
            .maxAmount(new BigDecimal("5000000"))
            .isActive(true)
            .build());

    // 3. Bronze
    plafonds.add(
        Plafond.builder()
            .name("Bronze")
            .description("Plafond Bronze dengan limit maksimal Rp 15.000.000")
            .maxAmount(new BigDecimal("15000000"))
            .isActive(true)
            .build());

    // 4. Silver
    plafonds.add(
        Plafond.builder()
            .name("Silver")
            .description("Plafond Silver dengan limit maksimal Rp 25.000.000")
            .maxAmount(new BigDecimal("25000000"))
            .isActive(true)
            .build());

    // 5. Gold
    plafonds.add(
        Plafond.builder()
            .name("Gold")
            .description("Plafond Gold dengan limit maksimal Rp 50.000.000")
            .maxAmount(new BigDecimal("50000000"))
            .isActive(true)
            .build());

    // 6. Diamond
    plafonds.add(
        Plafond.builder()
            .name("Diamond")
            .description("Plafond Diamond dengan limit maksimal Rp 100.000.000")
            .maxAmount(new BigDecimal("100000000"))
            .isActive(true)
            .build());

    // 7. VVIP
    plafonds.add(
        Plafond.builder()
            .name("VVIP")
            .description("Plafond VVIP dengan limit maksimal Rp 200.000.000")
            .maxAmount(new BigDecimal("200000000"))
            .isActive(true)
            .build());

    plafondRepository.saveAll(plafonds);
    log.info("Initialized {} request types (Plafond)", plafonds.size());
  }

  private void initializeTenorRates() {
    // Check if tier-based rates already exist (rates with plafond_id)
    // If any rate has a plafond, skip initialization
    long existingCount = tenorRateRepository.count();
    if (existingCount > 0) {
      log.info("Tenor rates already exist ({} records), skipping initialization", existingCount);
      return;
    }

    // Define tier-specific rates: Map<PlafondName, Map<TenorMonth, InterestRate>>
    Map<String, Map<Integer, BigDecimal>> tierRates = new LinkedHashMap<>();

    // Plus: 1, 3, 6, 9, 12 months
    Map<Integer, BigDecimal> plusRates = new LinkedHashMap<>();
    plusRates.put(1, new BigDecimal("0.00"));
    plusRates.put(3, new BigDecimal("3.00"));
    plusRates.put(6, new BigDecimal("5.00"));
    plusRates.put(9, new BigDecimal("6.00"));
    plusRates.put(12, new BigDecimal("7.00"));
    tierRates.put("Plus", plusRates);

    // Bronze: 1-18 months
    Map<Integer, BigDecimal> bronzeRates = new LinkedHashMap<>();
    bronzeRates.put(1, new BigDecimal("0.00"));
    bronzeRates.put(3, new BigDecimal("2.50"));
    bronzeRates.put(6, new BigDecimal("4.00"));
    bronzeRates.put(9, new BigDecimal("5.00"));
    bronzeRates.put(12, new BigDecimal("6.00"));
    bronzeRates.put(15, new BigDecimal("6.50"));
    bronzeRates.put(18, new BigDecimal("7.00"));
    tierRates.put("Bronze", bronzeRates);

    // Silver: 1-24 months
    Map<Integer, BigDecimal> silverRates = new LinkedHashMap<>();
    silverRates.put(1, new BigDecimal("0.00"));
    silverRates.put(3, new BigDecimal("2.00"));
    silverRates.put(6, new BigDecimal("3.50"));
    silverRates.put(9, new BigDecimal("4.50"));
    silverRates.put(12, new BigDecimal("5.50"));
    silverRates.put(15, new BigDecimal("6.00"));
    silverRates.put(18, new BigDecimal("6.50"));
    silverRates.put(21, new BigDecimal("7.00"));
    silverRates.put(24, new BigDecimal("7.50"));
    tierRates.put("Silver", silverRates);

    // Gold: 1-24 months
    Map<Integer, BigDecimal> goldRates = new LinkedHashMap<>();
    goldRates.put(1, new BigDecimal("0.00"));
    goldRates.put(3, new BigDecimal("1.50"));
    goldRates.put(6, new BigDecimal("3.00"));
    goldRates.put(9, new BigDecimal("4.00"));
    goldRates.put(12, new BigDecimal("5.00"));
    goldRates.put(15, new BigDecimal("5.50"));
    goldRates.put(18, new BigDecimal("6.00"));
    goldRates.put(21, new BigDecimal("6.50"));
    goldRates.put(24, new BigDecimal("7.00"));
    tierRates.put("Gold", goldRates);

    // Diamond: 1-24 months
    Map<Integer, BigDecimal> diamondRates = new LinkedHashMap<>();
    diamondRates.put(1, new BigDecimal("0.00"));
    diamondRates.put(3, new BigDecimal("1.00"));
    diamondRates.put(6, new BigDecimal("2.50"));
    diamondRates.put(9, new BigDecimal("3.50"));
    diamondRates.put(12, new BigDecimal("4.50"));
    diamondRates.put(15, new BigDecimal("5.00"));
    diamondRates.put(18, new BigDecimal("5.50"));
    diamondRates.put(21, new BigDecimal("6.00"));
    diamondRates.put(24, new BigDecimal("6.50"));
    tierRates.put("Diamond", diamondRates);

    // VVIP: 1-24 months (lowest rates)
    Map<Integer, BigDecimal> vvipRates = new LinkedHashMap<>();
    vvipRates.put(1, new BigDecimal("0.00"));
    vvipRates.put(3, new BigDecimal("0.50"));
    vvipRates.put(6, new BigDecimal("2.00"));
    vvipRates.put(9, new BigDecimal("3.00"));
    vvipRates.put(12, new BigDecimal("4.00"));
    vvipRates.put(15, new BigDecimal("4.50"));
    vvipRates.put(18, new BigDecimal("5.00"));
    vvipRates.put(21, new BigDecimal("5.50"));
    vvipRates.put(24, new BigDecimal("6.00"));
    tierRates.put("VVIP", vvipRates);

    // Insert rates for each plafond
    for (Map.Entry<String, Map<Integer, BigDecimal>> tierEntry : tierRates.entrySet()) {
      String plafondName = tierEntry.getKey();
      Optional<Plafond> plafondOpt =
          plafondRepository.findAllActive().stream()
              .filter(p -> p.getName().equalsIgnoreCase(plafondName))
              .findFirst();

      if (plafondOpt.isEmpty()) {
        log.warn("Plafond '{}' not found, skipping rate initialization", plafondName);
        continue;
      }

      Plafond plafond = plafondOpt.get();

      for (Map.Entry<Integer, BigDecimal> rateEntry : tierEntry.getValue().entrySet()) {
        Integer tenorMonth = rateEntry.getKey();
        BigDecimal interestRate = rateEntry.getValue();

        TenorRate rate =
            TenorRate.builder()
                .plafond(plafond)
                .tenorMonth(tenorMonth)
                .interestRate(interestRate)
                .description(plafondName + " - " + tenorMonth + " month(s) @ " + interestRate + "%")
                .isActive(true)
                .build();

        tenorRateRepository.save(rate);
      }
      log.info("Created {} tenor rates for tier: {}", tierEntry.getValue().size(), plafondName);
    }
  }
}
