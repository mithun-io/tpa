# TPA Insurance Claim Processing System - Exact API Testing Guide

This document provides a precise, DTO-aligned list of all API endpoints for the TPA system. It is structured to allow easy, copy-paste testing via Postman, matching the exact backend Java objects.

**Base URL**: `http://localhost:8080/api/v1`

---

## 🔐 Authentication & Authorization

Protected endpoints require a valid JWT token. Include it in the header:
```text
Authorization: Bearer <JWT_TOKEN>
```

---

## 1. AuthController (`/api/v1/auth`)

### 🔹 Register Customer
* **Method**: `POST`
* **URL**: `/api/v1/auth/customer/register`
* **Role**: PUBLIC
* **Description**: Registers a customer and sends an OTP.
* **Headers**: `Content-Type: application/json`
* **Request Body** (Matches `CustomerRequest`):
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "mobile": "9876543210",
  "password": "Password@123",
  "gender": "MALE",
  "dateOfBirth": "1990-05-15",
  "address": "123 Main St, Springfield"
}
```
* **Response (201 Created)**:
```json
{
  "success": true,
  "message": "otp sent successfully",
  "data": null,
  "statusCode": 201
}
```

### 🔹 Verify Customer OTP
* **Method**: `PATCH`
* **URL**: `/api/v1/auth/customer/verify`
* **Role**: PUBLIC
* **Headers**: `Content-Type: application/json`
* **Request Body** (Matches `OtpRequest`):
```json
{
  "email": "john.doe@example.com",
  "otp": "123456"
}
```

### 🔹 Register Patient
* **Method**: `POST`
* **URL**: `/api/v1/auth/patient/register`
* **Role**: PUBLIC
* **Headers**: `Content-Type: application/json`
* **Request Body** (Matches `PatientRequest`):
```json
{
  "patientName": "Jane Smith",
  "email": "jane.smith@example.com",
  "phoneNumber": "9876543211",
  "password": "Password@123!",
  "dateOfBirth": "1995-08-20",
  "gender": "FEMALE",
  "address": "456 Oak Avenue"
}
```

### 🔹 Register Carrier
* **Method**: `POST`
* **URL**: `/api/v1/auth/carrier/register`
* **Role**: PUBLIC
* **Description**: Registers a hospital/carrier.
* **Headers**: `Content-Type: application/json`
* **Request Body** (Matches `CarrierRegistrationRequest`):
```json
{
  "companyName": "City Hospital Ltd",
  "registrationNumber": "REG998877",
  "email": "admin@cityhospital.com",
  "mobile": "9988776655",
  "password": "Password@123!",
  "address": "789 Health St",
  "companyType": "HOSPITAL",
  "licenseNumber": "LIC-2026",
  "taxId": "GSTIN123456",
  "contactPersonName": "Dr. Smith",
  "contactPersonPhone": "9988776655",
  "website": "www.cityhospital.com"
}
```

### 🔹 Login User
* **Method**: `POST`
* **URL**: `/api/v1/auth/login`
* **Role**: PUBLIC
* **Headers**: `Content-Type: application/json`
* **Request Body** (Matches `LoginRequest`):
```json
{
  "email": "john.doe@example.com",
  "password": "Password@123"
}
```
* **Response (200 OK)** (Matches `LoginResponse` & `UserResponse`):
```json
{
  "success": true,
  "message": "login successful",
  "data": {
    "token": "eyJhbGciOiJIUz...",
    "refreshToken": "dGVzdC1yZWZyZXNo...",
    "userResponse": {
      "id": 1,
      "username": "John Doe",
      "email": "john.doe@example.com",
      "mobile": "9876543210",
      "dateOfBirth": "1990-05-15",
      "address": "123 Main St, Springfield",
      "gender": "MALE",
      "userRole": "CUSTOMER",
      "userStatus": "ACTIVE",
      "createdAt": "2026-04-10 10:30 AM"
    }
  },
  "statusCode": 200
}
```

---

## 2. ClaimController (`/api/v1/claims`)

### 🔹 Create Claim
* **Method**: `POST`
* **URL**: `/api/v1/claims`
* **Role**: `CUSTOMER`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body** (Matches `ClaimDataRequest`):
```json
{
  "claimFormPresent": true,
  "combinedDocumentPresent": true,
  "policyNumber": "POL-123456",
  "policyStatus": "ACTIVE",
  "claimFormPatientName": "John Doe",
  "combinedDocPatientName": "John Doe",
  "claimFormHospitalName": "City Hospital Ltd",
  "combinedDocHospitalName": "City Hospital Ltd",
  "claimFormAdmissionDate": "2026-04-12",
  "combinedDocAdmissionDate": "2026-04-12",
  "claimFormDischargeDate": "2026-04-15",
  "combinedDocDischargeDate": "2026-04-15",
  "claimedAmount": 1500.0,
  "totalBillAmount": 1500.0,
  "isDuplicate": false,
  "policyId": "POL-ID-999",
  "carrierName": "City Hospital Ltd",
  "policyName": "Health Plus Plan",
  "claimType": "REIMBURSEMENT",
  "diagnosis": "Viral Fever",
  "billNumber": "BILL-1001",
  "billDate": "2026-04-15"
}
```
* **Response (200 OK)** (Matches `ClaimResponse`):
```json
{
  "id": 1,
  "policyNumber": "POL-123456",
  "status": "SUBMITTED",
  "amount": 1500.0,
  "createdDate": "2026-04-28T10:00:00",
  "patientName": "John Doe",
  "hospitalName": "City Hospital Ltd",
  "admissionDate": "2026-04-12",
  "dischargeDate": "2026-04-15",
  "totalBillAmount": 1500.0,
  "diagnosis": "Viral Fever",
  "claimType": "REIMBURSEMENT",
  "username": "John Doe",
  "userEmail": "john.doe@example.com"
}
```

### 🔹 Get Claim by ID
* **Method**: `GET`
* **URL**: `/api/v1/claims/1`
* **Role**: `FMG_ADMIN`, `FMG_EMPLOYEE`, `CARRIER_USER`, `CUSTOMER`
* **Headers**: `Authorization: Bearer <token>`
* **Response**: Returns a full `ClaimResponse` (similar to above but includes fraud & AI fields if processed).

### 🔹 Carrier Approve Claim
* **Method**: `PUT`
* **URL**: `/api/v1/claims/1/carrier-approve`
* **Role**: `CARRIER_USER`
* **Headers**: `Authorization: Bearer <token>`

---

## 3. DocumentController (`/api/v1/files`)

### 🔹 Upload Document
* **Method**: `POST`
* **URL**: `/api/v1/files/upload`
* **Role**: `CUSTOMER`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`
* **Form Data**:
  * `claimId`: `1` (Text)
  * `documentType`: `CLAIM_FORM` (Text)
  * `file`: Select file (File)

### 🔹 Get Documents for Claim
* **Method**: `GET`
* **URL**: `/api/v1/files/claim/1`
* **Role**: `CUSTOMER`, `FMG_EMPLOYEE`, `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`

---

## 4. AdminController (`/api/v1/admin`)

### 🔹 Review Claim
* **Method**: `PATCH` (or `POST`)
* **URL**: `/api/v1/admin/claims/review`
* **Role**: `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body** (Matches `ClaimReviewRequest`):
```json
{
  "claimId": 1,
  "status": "APPROVED",
  "reviewNotes": "All documents verified and policy covers the diagnosis."
}
```

### 🔹 Approve/Reject Claim (Direct)
* **Method**: `PATCH`
* **URL**: `/api/v1/admin/claims/1/approve?reason=Verified`
* **URL**: `/api/v1/admin/claims/1/reject?reason=Missing Documents`
* **Role**: `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`

### 🔹 Get Carriers
* **Method**: `GET`
* **URL**: `/api/v1/admin/carriers`
* **Role**: `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`
* **Response**: List of `CarrierResponse`.

### 🔹 Assign Carrier to Claim
* **Method**: `PATCH`
* **URL**: `/api/v1/admin/claims/1/assign-carrier`
* **Role**: `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body**:
```json
{
  "carrierId": 5
}
```

---

## 5. CarrierController (`/api/v1/carrier`)

### 🔹 Get Assigned Claims
* **Method**: `GET`
* **URL**: `/api/v1/carrier/claims`
* **Role**: `CARRIER_USER`
* **Headers**: `Authorization: Bearer <token>`
* **Response**: Array of `CarrierClaimDetailResponse` (with full patient, fraud, and policy nested structures).

### 🔹 Add Remark
* **Method**: `PATCH`
* **URL**: `/api/v1/carrier/claims/1/remark`
* **Role**: `CARRIER_USER`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body**:
```json
{
  "remark": "Further investigation required."
}
```

---

## 6. PaymentController (`/api/v1/payments`)

### 🔹 Create Payment Order
* **Method**: `POST`
* **URL**: `/api/v1/payments/create-order`
* **Role**: `CUSTOMER`, `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body** (Matches `CreatePaymentOrderRequest`):
```json
{
  "claimId": 1,
  "amount": 1500.0
}
```

### 🔹 Verify Payment
* **Method**: `POST`
* **URL**: `/api/v1/payments/verify`
* **Role**: `CUSTOMER`, `FMG_ADMIN`
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body** (Matches `VerifyPaymentRequest`):
```json
{
  "razorpay_order_id": "order_H12345",
  "razorpay_payment_id": "pay_H12345",
  "razorpay_signature": "valid_signature_hash"
}
```
* **Response (Matches `PaymentResponse`)**:
```json
{
  "id": 1,
  "claimId": 1,
  "amount": 1500.0,
  "currency": "INR",
  "status": "COMPLETED",
  "razorpayOrderId": "order_H12345",
  "razorpayPaymentId": "pay_H12345",
  "createdAt": "2026-04-28T10:15:00"
}
```

---

## 7. AIController (`/api/v1/ai`)

### 🔹 Validate Claim (Pre-validation)
* **Method**: `POST`
* **URL**: `/api/v1/ai/validate-claim`
* **Role**: Authenticated
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body** (Matches `AiValidationRequest`):
```json
{
  "policyNumber": "POL-123456",
  "amount": 1500.0,
  "hospitalName": "City Hospital Ltd",
  "diagnosis": "Viral Fever",
  "patientName": "John Doe",
  "admissionDate": "2026-04-12",
  "dischargeDate": "2026-04-15"
}
```
* **Response (Matches `AiAnalysisResponse`)**:
```json
{
  "verdict": "APPROVED",
  "confidence": 0.95,
  "riskScore": 12.5,
  "validations": {
    "policyActive": true,
    "documentsComplete": true,
    "withinLimit": true
  },
  "financial": {
    "claimedAmount": 1500.0,
    "eligibleAmount": 1500.0
  },
  "flags": [],
  "recommendation": "All checks passed. Claim is safe to process.",
  "generatedAt": "2026-04-28T10:20:00"
}
```

### 🔹 Validate Document
* **Method**: `POST`
* **URL**: `/api/v1/ai/validate-document`
* **Role**: Authenticated
* **Headers**: `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`
* **Form Data**:
  * `documentType`: `CLAIM_FORM` (Text)
  * `file`: Select file (File)
* **Response (Matches `DocumentValidationResponse`)**:
```json
{
  "status": "VALID",
  "issues": [],
  "confidenceScore": 98
}
```
