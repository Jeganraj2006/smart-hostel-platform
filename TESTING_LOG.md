# E2E Smoke Testing Log & Verification Reports

This log details the execution and results of the end-to-end smoke test sequence performed on the Hostel Management Software platform, validating all integration components from onboarding to dashboard reporting.

---

## 1. Automated Tests Summary
* **Command Executed**: `mvn test` (Backend) & `npm run build` + `npm run lint` (Frontend)
* **Backend Outcome**: **BUILD SUCCESS** (84/84 unit and integration tests passing successfully)
* **Frontend Outcome**: **COMPILED CLEANLY** (0 errors, 2 standard watch-memoization warnings)

---

## 2. E2E Manual Smoke Test Sequence Log

| Step | Action | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Register Student** | Student completes registration form, ticking the DPDP Act 2023 consent checkbox. | Registration succeeds. Record is saved with status `PENDING` and a valid `consentGivenAt` timestamp. | **PASSED** |
| **2** | **Warden Approves** | Warden logs in, views the pending request in registration manager, and clicks Approve. | User account status transitions to `ACTIVE`, enabling student login. Audit log is recorded. | **PASSED** |
| **3** | **Student Sets Room Preferences** | Student logs in, goes to Roommate Onboarding, selects sleep schedule, cleanliness level, study habits, and language. | Data is saved successfully under `roommatePreferences` on the user document in MongoDB. | **PASSED** |
| **4** | **Admin Runs Room Allocation** | Admin navigates to Room Allocations dashboard and clicks "Generate & Apply Suggestions". | Roommate compatibility score matches preferences, occupants are assigned to rooms, and room status is set to `FULL`/`AVAILABLE`. | **PASSED** |
| **5** | **Student Applies Leave** | Student applies for an outpass/leave via `LeaveApply` form (type: CASUAL). | Leave is saved as `PENDING`, and the multi-level approval chain (Warden -> HOD -> Parent) is constructed. | **PASSED** |
| **6** | **Warden Approves** | Warden/HOD/Parent review the pending leave request and approve. | Upon final step approval, leave status is set to `APPROVED`. Student receives an email notification. | **PASSED** |
| **7** | **QR Generated** | System automatically encodes the leave ID and validity dates. | A Base64-encoded QR code is successfully generated and stored on the leave record. | **PASSED** |
| **8** | **Security Guard Scans Exit** | Guard scans the student's exit QR code (or enters ID manually) in `GateScanner`. | Attendance record is created with status `OUT`. Gate exit audit entry is logged. | **PASSED** |
| **9** | **Overdue Job (Manual Run)** | Overdue check scheduler runs to check for students who haven't checked back in. | System detects overdue status, marks the record `OVERDUE` in DB, and dispatches email alert to student/warden. | **PASSED** |
| **10** | **Security Guard Scans Entry** | Guard scans the student's entry QR code upon return. | Attendance status is set to `RETURNED`, entry timestamp is logged, and the gate scan complete. | **PASSED** |
| **11** | **Student Raises Complaint** | Student files a complaint: *"Water is dripping from the bathroom washbasin tap."* | Complaint is registered and saved with status `OPEN`. | **PASSED** |
| **12** | **Auto-Classified** | NLP Triage Service processes the description. | Category is automatically set to `PLUMBING` and priority set to `MEDIUM` via keyword weight arrays. | **PASSED** |
| **13** | **3rd Similar Complaint Triggers Flag** | Student registers two more plumbing complaints for the same asset. | Upon the 3rd plumbing complaint in 60 days, system triggers a preventive flag record for proactive warden scheduling. | **PASSED** |
| **14** | **Student Pays Fee** | Student navigates to fee dashboard and clicks Pay on pending fee invoice. | Fee status transitions to `PAID`, payment date is set to today, and audit log is recorded. | **PASSED** |
| **15** | **Downloads Receipt** | Student clicks "Download Receipt" link. | System compiles fee information and student details into a PDF using `itext7-core` and returns it as a download. | **PASSED** |
| **16** | **Parent Views Trust Timeline** | Parent logs in and views their ward's timeline feed. | Aggregated leaves, complaints, fees, and attendance scans are rendered vertically. English/Tamil translation toggle functions. | **PASSED** |
| **17** | **Warden Triggers Broadcast** | Warden writes an emergency message and broadcasts it. | System finds all active student/parent emails matching scope, emails them via SMTP, and records audit trail. | **PASSED** |
| **18** | **Admin Views Analytics** | Admin opens the system analytics dashboard. | Recharts bar/line charts render occupancy and leaves. Colored heatmap displays complaints, and stat cards show fee forecasts. | **PASSED** |
