# Digital Personal Data Protection (DPDP) Act 2023 Compliance & Data Handling Policy

This document outlines the data-handling policies and practices implemented within the Hostel Management Software platform to ensure compliance with the **Digital Personal Data Protection (DPDP) Act, 2023** (India).

---

## 1. Core Principles of Data Protection

In alignment with India's DPDP Act 2023, our platform adheres to the following principles:

1. **Lawfulness and Consent**: Personal data is collected and processed only with explicit, specific, unconditional, unambiguous, and informed consent (Section 6).
2. **Purpose Limitation**: Personal data is processed solely for the specified purpose of hostel operations, student safety, room allocation, and parent notification (Section 6).
3. **Data Minimization**: We only collect the minimal personal data required for functional residency registration (names, phone numbers, email addresses, and roommates preferences).
4. **Accuracy and Integrity**: Users have the right to request updates or corrections to their personal data.
5. **Storage Limitation**: Personal data is stored only as long as necessary to fulfill the operational purpose and legal/audit mandates.

---

## 2. Consent Capture during Registration

* During registration, users (students, parents, wardens, HODs, staff) are presented with a consent notice checkbox.
* Registering without checking the consent box is blocked.
* Upon successful registration, the timestamp of consent is captured and stored permanently under the `consentGivenAt` field in the database.

---

## 3. Right to Be Forgotten (Data Anonymization)

Section 12 of the DPDP Act 2023 gives Data Principals the right to erasure of their personal data. To support this while preserving institutional audit integrity (e.g. tracking historical fee receipts, leaves logged, or incident logs):

* **Admin Erasure Endpoint**: Administrators can request anonymization via `DELETE /api/students/{id}/data`.
* **Anonymization Process**:
  1. The student's account status is set to `ANONYMIZED`.
  2. The student's `name` is modified to a static `"Anonymized Student"`.
  3. The `phone` number is reset to `"0000000000"`.
  4. The `email` is updated to a unique, non-functional identifier (`"anonymized_{id}@hostel.internal"`) to prevent unique database index collisions and disable email alerts.
  5. The account is deactivated (`isActive = false`), and the hashed `password` is reset to a cryptographically secure random UUID to prevent future logins.
  6. The roommate preferences and any parent linkages are completely purged.
  7. The student is removed from any active hostel room roommate allocations.
* **Audit Retention**: Database record IDs and structural logs (e.g. fees paid, complaints resolved) are preserved for institutional audit records, but are entirely unlinked from any personally identifiable information (PII).
