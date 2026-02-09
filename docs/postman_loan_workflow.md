# Loan Workflow - Postman Guide

Panduan lengkap untuk testing alur pinjaman melalui Postman.

## 🔐 Base URL
```
http://localhost:7000
```

---

## STEP 0: Login untuk mendapatkan Token

### Login sebagai Customer
```
POST /api/auth/login
Content-Type: application/json

{
    "username": "customer",
    "password": "password123"
}
```
> Simpan token untuk digunakan di STEP 1 dan 4

### Login sebagai Marketing
```
POST /api/auth/login

{
    "username": "marketing",
    "password": "password123"
}
```
> Simpan token untuk STEP 2

### Login sebagai Branch Manager
```
POST /api/auth/login

{
    "username": "branchmanager",
    "password": "password123"
}
```
> Simpan token untuk STEP 3

### Login sebagai Back Office
```
POST /api/auth/login

{
    "username": "backoffice",
    "password": "password123"
}
```
> Simpan token untuk STEP 5

---

## STEP 1: Customer Mengajukan Plafond

### 1a. Lihat Plafond yang Tersedia
```
GET /api/plafonds
Authorization: Bearer {{customer_token}}
```

### 1b. Apply Plafond
```
POST /api/plafond-applications/apply
Authorization: Bearer {{customer_token}}
Content-Type: application/json

{
    "plafondId": 4,
    "nik": "3201234567890001",
    "birthPlace": "Jakarta",
    "birthDate": "1990-05-15",
    "maritalStatus": "Single",
    "occupation": "Software Engineer",
    "monthlyIncome": 15000000,
    "phone": "081234567890",
    "npwp": "12.345.678.9-012.000"
}
```

**Response yang diharapkan:**
```json
{
    "success": true,
    "message": "Application submitted successfully",
    "data": {
        "id": 1,
        "status": "PENDING_REVIEW",
        ...
    }
}
```
> Catat `id` aplikasi untuk digunakan di step selanjutnya

---

## STEP 2: Marketing Mereview Aplikasi

### 2a. Lihat Daftar Aplikasi Pending Review
```
GET /api/plafond-applications/pending-review
Authorization: Bearer {{marketing_token}}
```

### 2b. Marketing Menyetujui Aplikasi
```
POST /api/plafond-applications/review
Authorization: Bearer {{marketing_token}}
Content-Type: application/json

{
    "applicationId": 1,
    "approved": true,
    "note": "Documents verified, all requirements met"
}
```

**Atau untuk Menolak:**
```json
{
    "applicationId": 1,
    "approved": false,
    "note": "Incomplete documents"
}
```

**Response yang diharapkan:**
```json
{
    "success": true,
    "message": "Application reviewed successfully",
    "data": {
        "id": 1,
        "status": "WAITING_APPROVAL",
        ...
    }
}
```

---

## STEP 3: Branch Manager Approve Aplikasi

### 3a. Lihat Daftar Aplikasi Waiting Approval
```
GET /api/plafond-applications/waiting-approval
Authorization: Bearer {{branchmanager_token}}
```

### 3b. Branch Manager Menyetujui dengan Limit
```
POST /api/plafond-applications/approve
Authorization: Bearer {{branchmanager_token}}
Content-Type: application/json

{
    "applicationId": 1,
    "approved": true,
    "approvedLimit": 40000000,
    "note": "Approved based on income verification"
}
```

> **Note:** `approvedLimit` wajib diisi oleh Branch Manager. Nilai tidak boleh melebihi `maxAmount` dari plafond yang diajukan.

**Atau untuk Menolak:**
```json
{
    "applicationId": 1,
    "approved": false,
    "note": "Risk assessment failed"
}
```

**Response yang diharapkan:**
```json
{
    "success": true,
    "message": "Application approved successfully",
    "data": {
        "id": 1,
        "status": "APPROVED",
        "approvedLimit": 40000000,
        "usedAmount": 0,
        "availableLimit": 40000000,
        ...
    }
}
```
> Branch Manager menentukan limit yang disetujui (tidak harus sama dengan maxAmount plafond)

---

## STEP 4: Customer Request Pencairan

### 4a. Lihat Tenor Rates yang Tersedia
```
GET /api/tenor-rates
Authorization: Bearer {{customer_token}}
```

### 4b. Lihat Plafond yang Sudah Disetujui
```
GET /api/plafond-applications/my-approved
Authorization: Bearer {{customer_token}}
```

### 4c. Request Pencairan
```
POST /api/disbursements/request
Authorization: Bearer {{customer_token}}
Content-Type: application/json

{
    "userPlafondId": 1,
    "amount": 10000000,
    "tenorMonth": 12
}
```

**Tenor yang valid:** 1, 3, 6, 9, 12, 15, 18, 21, 24

**Response yang diharapkan:**
```json
{
    "success": true,
    "message": "Disbursement request submitted",
    "data": {
        "id": 1,
        "amount": 10000000,
        "interestRate": 10.00,
        "tenorMonth": 12,
        "interestAmount": 1000000,
        "totalAmount": 11000000,
        "status": "PENDING",
        "remainingLimit": 30000000,
        ...
    }
}
```

---

## STEP 5: Back Office Mencairkan Dana

### 5a. Lihat Daftar Disbursement Pending
```
GET /api/disbursements/pending
Authorization: Bearer {{backoffice_token}}
```

### 5b. Proses Pencairan
```
POST /api/disbursements/1/process
Authorization: Bearer {{backoffice_token}}
Content-Type: application/json

{
    "note": "Dana telah ditransfer ke rekening customer"
}
```

**Atau untuk Membatalkan:**
```
POST /api/disbursements/1/cancel
Authorization: Bearer {{backoffice_token}}
Content-Type: application/json

{
    "reason": "Data tidak valid"
}
```

**Response yang diharapkan:**
```json
{
    "success": true,
    "message": "Disbursement processed successfully",
    "data": {
        "id": 1,
        "status": "DISBURSED",
        "disbursedAt": "2026-01-09T17:00:00",
        "disbursedByUsername": "backoffice",
        ...
    }
}
```

---

## BONUS: Endpoint Tambahan

### Lihat Semua Aplikasi Saya (Customer)
```
GET /api/plafond-applications/my-applications
Authorization: Bearer {{customer_token}}
```

### Lihat Semua Pencairan Saya (Customer)
```
GET /api/disbursements/my-disbursements
Authorization: Bearer {{customer_token}}
```

### Lihat Notifikasi Saya
```
GET /api/notifications
Authorization: Bearer {{token}}
```

### Lihat Detail Aplikasi
```
GET /api/plafond-applications/1
Authorization: Bearer {{token}}
```

---

## 📋 Ringkasan Headers

Untuk semua request yang membutuhkan autentikasi:
```
Authorization: Bearer <token>
Content-Type: application/json
```

---

## 🔄 Status Flow

```
PENDING_REVIEW → WAITING_APPROVAL → APPROVED → (Disbursement) PENDING → DISBURSED
                       ↓                  ↓                              ↓
                   REJECTED            REJECTED                      CANCELLED
```
