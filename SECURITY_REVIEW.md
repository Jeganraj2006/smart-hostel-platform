# Security Review Report & Hardening Logs

This document records the security audit vulnerabilities identified and resolved across the project controllers to enforce role checks, data ownership validations, and prevent post-setup administrative backdoors.

## Identified and Resolved Issues

1. **SetupController Backdoor Block**: Restricted `/api/setup/create-user` to only execute when `userRepository.count() == 0` to prevent post-setup rogue admin account registrations.
2. **LeaveController Emergency Override Lock**: Added `@PreAuthorize("hasRole('WARDEN')")` to prevent students or other roles from approving their own leaves via emergency override endpoints.
3. **LeaveController Send Reminder Ownership Check**: Verified that the logged-in student's ID matches the leave record `studentId` to block cross-user reminder spamming.
4. **LeaveController Role Protection**: Enforced `@PreAuthorize("hasAnyRole('WARDEN', 'HOD', 'PARENT')")` checks on pending lists, approvals, and rejections.
5. **ComplaintController Listing Access Control**: Restrained the global `GET /api/complaints` list endpoint to `WARDEN`, `SUPER_ADMIN`, and `STAFF` roles using `@PreAuthorize`.
6. **ComplaintController Status Update Ownership Check**: Added role checks (WARDEN/STAFF/ADMIN) and student ownership validation on complaint status updates to prevent unauthorized closure.
7. **ComplaintController Rating Ownership Check**: Validated that students can only submit satisfaction ratings for complaints they raised.
8. **FeeController Global Listing Access Control**: Restricted the global `GET /api/fees` listing endpoint to WARDEN and SUPER_ADMIN roles to prevent cross-student fee history leaks.
9. **FeeController Status Modifications Lock**: Enforced `@PreAuthorize("hasAnyRole('WARDEN', 'SUPER_ADMIN')")` restrictions on manual fee status update calls.
10. **AuditController Role Compatibility**: Updated path matching constraints in `SecurityConfig.java` and added `@PreAuthorize` to allow both `SUPER_ADMIN` and `ADMIN` roles to view system audit logs.
