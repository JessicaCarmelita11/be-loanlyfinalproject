# 📚 Dokumentasi Lengkap - Loan Banking System

## Daftar Isi
1. [Struktur Project](#1-struktur-project)
2. [Konfigurasi](#2-konfigurasi)
3. [Entity (Model Database)](#3-entity-model-database)
4. [Repository (Data Access)](#4-repository-data-access)
5. [Service (Business Logic)](#5-service-business-logic)
6. [Controller (API Endpoints)](#6-controller-api-endpoints)
7. [Security (JWT Authentication)](#7-security-jwt-authentication)
8. [Flow Lengkap](#8-flow-lengkap)

---

## 1. Struktur Project

```
src/main/java/com/example/loanlyFinalProject/
├── config/                 # Konfigurasi aplikasi
│   ├── SecurityConfig.java     # Spring Security + JWT
│   ├── SwaggerConfig.java      # API Documentation
│   ├── StorageConfig.java      # Cloudflare R2
│   └── DataInitializer.java    # Data awal (roles, admin)
│
├── controller/             # REST API Endpoints
│   ├── AuthController.java           # Register, Login
│   ├── PlafondController.java        # CRUD Plafond (Admin)
│   ├── PlafondApplicationController.java  # Apply, Review, Approve
│   ├── DisbursementController.java   # Pencairan dana
│   └── NotificationController.java   # Notifikasi user
│
├── dto/                    # Data Transfer Objects
│   ├── request/                # Input dari client
│   └── response/               # Output ke client
│
├── entity/                 # JPA Entities (Tabel Database)
│   ├── User.java               # Tabel users
│   ├── Role.java               # Tabel roles
│   ├── Plafond.java            # Produk kredit
│   ├── UserPlafond.java        # Pengajuan limit kredit
│   ├── Disbursement.java       # Pencairan dana
│   └── Notification.java       # Notifikasi
│
├── repository/             # JPA Repositories
├── service/                # Business Logic
├── security/               # JWT Components
└── exception/              # Exception Handling
```

---

## 2. Konfigurasi

### 2.1 application.properties
```properties
# Database SQL Server
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=loanDatabase
spring.datasource.username=sa
spring.datasource.password=admin1101

# JWT Token (berlaku 24 jam)
jwt.secret=dGhpc0lzQVNlY3JldEtleUZvckpXVFRva2Vu...
jwt.expiration=86400000

# Email (Mailtrap)
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
```

### 2.2 SecurityConfig.java
```java
// Endpoint yang TIDAK perlu login
private static final String[] PUBLIC_ENDPOINTS = {
    "/api/auth/**",       // Register, Login
    "/api/public/**",     // Lihat plafond
    "/swagger-ui/**"      // Dokumentasi API
};

// Endpoint yang perlu ROLE tertentu
.requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
.requestMatchers("/api/marketing/**").hasAnyRole("SUPER_ADMIN", "MARKETING")
.requestMatchers("/api/branch-manager/**").hasAnyRole("SUPER_ADMIN", "BRANCH_MANAGER")
.requestMatchers("/api/back-office/**").hasAnyRole("SUPER_ADMIN", "BACK_OFFICE")
.requestMatchers("/api/customer/**").hasAnyRole("SUPER_ADMIN", "CUSTOMER")
```

### 2.3 DataInitializer.java
Membuat data awal saat aplikasi pertama kali jalan:
- 5 Roles: SUPER_ADMIN, MARKETING, BRANCH_MANAGER, BACK_OFFICE, CUSTOMER
- 1 User: superadmin / Admin@123

---

## 3. Entity (Model Database)

### 3.1 User.java (Tabel: users)
```java
@Entity
@Table(name = "users")
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;      // Di-hash dengan BCrypt
    private String fullName;
    private Boolean isActive;
    
    @ManyToMany
    private Set<Role> roles;      // User bisa punya banyak role
}
```

### 3.2 Plafond.java (Tabel: plafonds)
Produk kredit yang ditawarkan bank.
```java
@Entity
@Table(name = "plafonds")
public class Plafond {
    private Long id;
    private String name;          // "Gold Plafond"
    private BigDecimal maxAmount; // 100.000.000
    private BigDecimal interestRate; // 12.5%
    private Integer tenorMonth;   // 24 bulan
    private Boolean isActive;
    private LocalDateTime deletedAt; // Soft delete
}
```

### 3.3 UserPlafond.java (Tabel: user_plafonds)
Pengajuan limit kredit oleh customer.
```java
@Entity
@Table(name = "user_plafonds")
public class UserPlafond {
    private Long id;
    private User user;            // Siapa yang mengajukan
    private Plafond plafond;      // Produk yang dipilih
    
    // STATUS WORKFLOW
    private PlafondApplicationStatus status;
    // PENDING_REVIEW → WAITING_APPROVAL → APPROVED / REJECTED
    
    // LIMIT KREDIT
    private BigDecimal approvedLimit;  // Limit yang disetujui
    private BigDecimal usedAmount;     // Yang sudah dipakai
    
    // DATA PENGAJU
    private String nik;
    private String occupation;
    private BigDecimal monthlyIncome;
    
    // APPROVAL
    private User reviewedBy;      // Marketing yang review
    private User approvedBy;      // BM yang approve
}
```

### 3.4 Disbursement.java (Tabel: disbursements)
Pencairan dana dari limit yang sudah disetujui.
```java
@Entity
@Table(name = "disbursements")
public class Disbursement {
    private Long id;
    private UserPlafond userPlafond;  // Dari limit mana
    
    // JUMLAH
    private BigDecimal amount;         // Nominal pencairan
    private BigDecimal interestRate;   // Bunga
    private Integer tenorMonth;        // Tenor
    private BigDecimal interestAmount; // Jumlah bunga
    private BigDecimal totalAmount;    // Total bayar
    
    // STATUS
    private DisbursementStatus status; // PENDING → DISBURSED
    private User disbursedBy;          // Back Office yang proses
}
```

---

## 4. Repository (Data Access)

Repository adalah interface untuk akses database menggunakan JPA.

### Contoh: UserPlafondRepository.java
```java
@Repository
public interface UserPlafondRepository extends JpaRepository<UserPlafond, Long> {
    
    // Cari pengajuan yang statusnya PENDING_REVIEW
    List<UserPlafond> findAllPendingReview();
    
    // Cari pengajuan yang sudah APPROVED untuk user tertentu
    List<UserPlafond> findApprovedByUserId(Long userId);
    
    // Cek apakah user sudah punya pengajuan aktif untuk plafond ini
    boolean existsActiveByUserIdAndPlafondId(Long userId, Long plafondId);
}
```

---

## 5. Service (Business Logic)

### 5.1 AuthService.java
```java
// REGISTER
public AuthResponse register(RegisterRequest request) {
    // 1. Cek apakah username/email sudah ada
    // 2. Hash password dengan BCrypt
    // 3. Assign role CUSTOMER
    // 4. Simpan ke database
    // 5. Generate JWT token
    return AuthResponse(token, user);
}

// LOGIN
public AuthResponse login(LoginRequest request) {
    // 1. Cari user berdasarkan username/email
    // 2. Validasi password
    // 3. Generate JWT token
    return AuthResponse(token, user);
}
```

### 5.2 PlafondApplicationService.java
```java
// CUSTOMER: Apply for Plafond
public UserPlafondResponse applyForPlafond(Long userId, request) {
    // 1. Cek plafond ada dan aktif
    // 2. Cek user belum punya pengajuan aktif untuk plafond ini
    // 3. Buat UserPlafond dengan status PENDING_REVIEW
    // 4. Simpan data pengaju (NIK, pekerjaan, penghasilan)
    // 5. Kirim notifikasi ke customer
    return response;
}

// MARKETING: Review
public UserPlafondResponse reviewApplication(Long marketingId, request) {
    // 1. Cek status harus PENDING_REVIEW
    // 2. Jika approve: status → WAITING_APPROVAL
    // 3. Jika reject: status → REJECTED
    // 4. Kirim notifikasi ke customer
}

// BRANCH MANAGER: Approve
public UserPlafondResponse approveApplication(Long bmId, request) {
    // 1. Cek status harus WAITING_APPROVAL
    // 2. Jika approve: 
    //    - status → APPROVED
    //    - set approvedLimit (misal Rp 50 juta)
    // 3. Kirim notifikasi ke customer
}
```

### 5.3 DisbursementService.java
```java
// CUSTOMER: Request Pencairan
public DisbursementResponse requestDisbursement(Long userId, request) {
    // 1. Cek limit sudah APPROVED
    // 2. Cek sisa limit cukup
    // 3. Hitung bunga:
    //    interestAmount = amount × (rate/100) × (tenor/12)
    //    totalAmount = amount + interestAmount
    // 4. Buat Disbursement dengan status PENDING
    // 5. Kurangi availableLimit
    // 6. Kirim notifikasi
}

// BACK OFFICE: Process
public DisbursementResponse processDisbursement(Long boId, disbursementId) {
    // 1. Cek status PENDING
    // 2. Update status → DISBURSED
    // 3. Kirim notifikasi "Dana sudah dicairkan"
}
```

---

## 6. Controller (API Endpoints)

### 6.1 AuthController.java
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(RegisterRequest request) {
        // Panggil authService.register()
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request) {
        // Panggil authService.login()
    }
}
```

### 6.2 PlafondApplicationController.java
```java
@RestController
@RequestMapping("/api")
public class PlafondApplicationController {
    
    // CUSTOMER
    @PostMapping("/customer/plafonds/apply")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<?> apply(@RequestAttribute("userId") Long userId, request) {
        // userId didapat dari JWT token
    }
    
    // MARKETING
    @PostMapping("/marketing/plafond-applications/review")
    @PreAuthorize("hasAnyRole('MARKETING', 'SUPER_ADMIN')")
    public ResponseEntity<?> review(request) { ... }
    
    // BRANCH MANAGER
    @PostMapping("/branch-manager/plafond-applications/approve")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> approve(request) { ... }
}
```

---

## 7. Security (JWT Authentication)

### 7.1 Flow Autentikasi
```
1. User login → Server generate JWT token
2. Client simpan token
3. Setiap request, client kirim: Authorization: Bearer <token>
4. Server validasi token → extract userId, roles
5. Cek apakah user punya akses ke endpoint tersebut
```

### 7.2 JwtService.java
```java
// Generate token
public String generateToken(CustomUserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getId().toString())
        .claim("username", userDetails.getUsername())
        .issuedAt(now)
        .expiration(expiryDate)  // 24 jam
        .signWith(signingKey)
        .compact();
}

// Validasi token
public boolean validateToken(String token) {
    Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token);
    return true;
}
```

### 7.3 JwtAuthenticationFilter.java
```java
// Filter yang jalan setiap request
protected void doFilterInternal(request, response, filterChain) {
    // 1. Ambil token dari header Authorization
    String token = getTokenFromRequest(request);
    
    // 2. Validasi token
    if (jwtService.validateToken(token)) {
        // 3. Extract userId dari token
        Long userId = jwtService.getUserIdFromToken(token);
        
        // 4. Set ke request attribute (bisa diakses di controller)
        request.setAttribute("userId", userId);
        
        // 5. Set SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    
    filterChain.doFilter(request, response);
}
```

---

## 8. Flow Lengkap

### 8.1 Register
```
POST /api/auth/register
    ↓
AuthController.register()
    ↓
AuthService.register()
    ↓
1. Validasi username/email unik
2. Hash password: passwordEncoder.encode(password)
3. Assign role CUSTOMER
4. userRepository.save(user)
5. Generate JWT token
    ↓
Response: { token, user info }
```

### 8.2 Apply Plafond
```
POST /api/customer/plafonds/apply
Header: Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter
    ↓
1. Extract userId dari token
2. Cek role = CUSTOMER
    ↓
PlafondApplicationController.apply()
    ↓
PlafondApplicationService.applyForPlafond()
    ↓
1. Cari user & plafond
2. Cek belum ada pengajuan aktif
3. Buat UserPlafond (status: PENDING_REVIEW)
4. Simpan data pengaju
5. notificationService.createNotification()
    ↓
Response: { application id, status }
```

### 8.3 Marketing Review
```
POST /api/marketing/plafond-applications/review
{ "applicationId": 1, "approved": true }
    ↓
PlafondApplicationService.reviewApplication()
    ↓
1. Cek status = PENDING_REVIEW
2. Update status → WAITING_APPROVAL
3. Set reviewedBy = marketing user
4. Kirim notifikasi ke customer
```

### 8.4 Branch Manager Approve
```
POST /api/branch-manager/plafond-applications/approve
{ "applicationId": 1, "approved": true, "approvedLimit": 50000000 }
    ↓
PlafondApplicationService.approveApplication()
    ↓
1. Cek status = WAITING_APPROVAL
2. Update status → APPROVED
3. Set approvedLimit = 50.000.000
4. Set usedAmount = 0
5. Kirim notifikasi ke customer
```

### 8.5 Request Disbursement
```
POST /api/customer/disbursements
{ "userPlafondId": 1, "amount": 10000000 }
    ↓
DisbursementService.requestDisbursement()
    ↓
1. Cek status = APPROVED
2. Cek availableLimit >= amount (50jt - 0 = 50jt >= 10jt ✓)
3. Hitung bunga:
   interestAmount = 10jt × 12.5% × (12/12) = 1.25jt
   totalAmount = 10jt + 1.25jt = 11.25jt
4. Buat Disbursement (status: PENDING)
5. Update usedAmount = 10jt
6. Kirim notifikasi
```

### 8.6 Back Office Process
```
POST /api/back-office/disbursements/1/process
    ↓
DisbursementService.processDisbursement()
    ↓
1. Cek status = PENDING
2. Update status → DISBURSED
3. Set disbursedAt, disbursedBy
4. Kirim notifikasi "Dana dicairkan"
   "Pencairan Rp 10jt berhasil. Total bayar: Rp 11.25jt"
```

---

## Kredensial Default

```
Username: superadmin
Password: Admin@123
Role: SUPER_ADMIN (akses semua endpoint)
```

---

**Dokumentasi ini menjelaskan seluruh alur kode dari awal hingga akhir.**
