# Loanly API Documentation

This documentation provides a comprehensive guide to the Loanly API endpoints, including authentication requirements, request parameters, and response structures.

## Base URL
`http://localhost:8080`

## Authentication
Most endpoints require a Bearer Token. To authenticate, include the following header in your requests:
`Authorization: Bearer <your_jwt_token>`

---

## 🔐 Authentication Endpoints (`/api/auth`)

| Endpoint | Method | Auth | Description |
| :--- | :--- | :--- | :--- |
| `/register` | `POST` | Public | Register a new user account (Role: CUSTOMER) |
| `/login` | `POST` | Public | Authenticate user and receive JWT token |
| `/forgot-password` | `POST` | Public | Request a password reset link (sent via email) |
| `/reset-password` | `POST` | Public | Reset password using the token from email |
| `/validate-token` | `GET` | Public | Check if a password reset token is still valid |

### Request Bodies (Auth)

**Register Request**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phone": "08123456789"
}
```

**Login Request**
```json
{
  "username": "johndoe",
  "password": "password123"
}
```

---

## 👥 User Management (Admin) (`/api/admin`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/users` | `GET` | SUPER_ADMIN, BRANCH_MANAGER | Get all users |
| `/users/{id}` | `GET` | SUPER_ADMIN, BRANCH_MANAGER | Get user details by ID |
| `/users` | `POST` | SUPER_ADMIN | Create a new user (Staff or Admin) |
| `/users/{id}` | `PUT` | SUPER_ADMIN | Update user details |
| `/users/{id}` | `DELETE` | SUPER_ADMIN | Delete a user |
| `/users/{id}/toggle-status` | `PUT` | SUPER_ADMIN | Deactivate/Activate a user |
| `/roles` | `GET` | SUPER_ADMIN, BRANCH_MANAGER | List all available roles |

---

## 💰 Plafond Management (`/api/plafonds`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/public/plafonds` | `GET` | Public | List all active credit products |
| `/api/public/plafonds/{id}` | `GET` | Public | Get credit product details |
| `/api/admin/plafonds` | `GET` | SUPER_ADMIN | List all plafonds (paginated) |
| `/api/admin/plafonds` | `POST` | SUPER_ADMIN | Create a new credit product |
| `/api/admin/plafonds/{id}` | `PUT` | SUPER_ADMIN | Update credit product |
| `/api/admin/plafonds/{id}` | `DELETE` | SUPER_ADMIN | Soft delete credit product |

---

## 📝 Plafond Applications (`/api`)

### Customer Endpoints
| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/customer/plafonds/apply` | `POST` | CUSTOMER | Apply for a credit limit |
| `/customer/plafonds/applications` | `GET` | CUSTOMER | View status of all my applications |
| `/customer/plafonds/approved` | `GET` | CUSTOMER | View approved credit lines and limits |
| `/customer/plafonds/applications/{id}` | `GET` | CUSTOMER | View specific application details |
| `/customer/plafonds/applications/{id}/documents` | `POST` | CUSTOMER | Upload supporting docs (Multipart/form-data) |

### Review & Approval Workflow
| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/marketing/plafond-applications/pending` | `GET` | MARKETING | Get applications waiting for review |
| `/marketing/plafond-applications/review` | `POST` | MARKETING | Review and forward to Branch Manager |
| `/branch-manager/plafond-applications/pending` | `GET` | BRANCH_MANAGER | Get applications waiting for final approval |
| `/branch-manager/plafond-applications/approve` | `POST` | BRANCH_MANAGER | Final approval/rejection of credit limit |

---

## 💸 Disbursement Workflow (`/api`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/customer/disbursements` | `POST` | CUSTOMER | Request fund disbursement from approved limit |
| `/customer/disbursements` | `GET` | CUSTOMER | View my disbursement history |
| `/back-office/disbursements/pending` | `GET` | BACK_OFFICE | Get pending disbursement requests |
| `/back-office/disbursements/{id}/process` | `POST` | BACK_OFFICE | Process and confirm disbursement |
| `/back-office/disbursements/{id}/cancel` | `POST` | BACK_OFFICE | Cancel disbursement request |

---

## 🔔 Notifications (`/api/notifications`)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/` | `GET` | Authenticated | Get all user notifications |
| `/unread` | `GET` | Authenticated | Get unread notifications |
| `/count` | `GET` | Authenticated | Get count of unread notifications |
| `/{id}/read` | `PUT` | Authenticated | Mark a notification as read |
| `/read-all` | `PUT` | Authenticated | Mark all notifications as read |

---

## 🏛️ History & Monitoring (Staff)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/plafond-histories` | `GET` | STAFF | View all plafond status changes |
| `/api/disbursements` | `GET` | STAFF | View all system disbursements |
| `/api/admin/customers/approved` | `GET` | STAFF | View all customers with active credit lines |

---

## 📦 Common Response Format

All responses follow this standard structure:

**Success Response**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

**Error Response**
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```
