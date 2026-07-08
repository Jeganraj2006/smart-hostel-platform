# Smart Hostel Platform

Smart Hostel Platform is a comprehensive, multi-role hostel operations and analytics management ecosystem designed to automate student onboarding, residential safety, complaint triage, fee collection, and parent communication.

---

## 1. Six Unique Differentiators

1. **Compatibility-Based Room Allocation**: Matches students to rooms using an matching algorithm based on sleep schedules, cleanliness habits, study preferences, and language compatibility.
2. **Verified Gate Exit-Entry**: Uses dynamically generated QR codes on approved student leaves, scanned by security guards to log gate pass movement (OUT/RETURNED) and flag overdue students.
3. **NLP Complaint Triage & Preventive Maintenance**: Parses complaint logs automatically to assign categories and priorities. The system triggers a *Preventive Maintenance Flag* if 3 similar category complaints are raised on the same block asset within 60 days.
4. **Fee Risk Scoring**: Computes a payment risk tier (HIGH, MEDIUM, LOW) for students based on late-payment frequency and current overdue counts to identify financial default risks.
5. **Parent Trust Timeline**: A consolidated, chronologically ordered timeline displaying ward activities (leaves, fees, complaints, and gate scans). Features a Tamil static translation toggle for localized accessibility.
6. **Visitor & Emergency Management**: Manages real-time visitor checks and lets wardens dispatch system-wide emergency broadcast alerts via SMTP to students and parents.

---

## 2. Platform Access Control Matrix

| Role | Permitted Actions |
| :--- | :--- |
| **STUDENT** | Apply for leaves, raise complaints, pay fee invoices, update roommate onboarding profiles, download receipts. |
| **PARENT** | View linked ward's trust activity timeline, toggle UI languages (English/Tamil), review leave requests. |
| **WARDEN** | Approve/reject user registrations, review leaves, trigger emergency broadcast alerts, manage visitor logs, resolve preventive maintenance flags. |
| **SECURITY_GUARD** | Scan gatepass QR codes, record student exits and entries, check live daily gate logs. |
| **STAFF** | Log daily utility resource readings (Electricity, Water, Mess Wastage). |
| **HOD** | View analytics dashboard (occupancy, complaints heatmap, outpass spikes, fee forecasts, resource log summaries). |
| **ADMIN / SUPER_ADMIN** | Run roommate allocations, anonymize student data on request, view system audit logs. |

---

## 3. Installation & Run Guide

### Docker Compose (Recommended)
Launch all components (MongoDB, Redis, Backend, Frontend) with a single command:
```bash
docker-compose up --build
```
* **Frontend**: Accessible at `http://localhost:80`
* **Backend**: Accessible at `http://localhost:8080`

---

### Manual Installation

#### Prerequisites
* **Java**: JDK 17
* **Node.js**: v18+
* **Databases**: MongoDB and Redis installed and running locally

#### 1. Backend Setup
1. Navigate to directory:
   ```bash
   cd hostel-backend
   ```
2. Copy configuration environment:
   ```bash
   cp .env.example .env
   ```
3. Update `.env` with your database connections and mail host credentials:
   ```env
   MONGODB_URI=mongodb://localhost:27017/hostel_db
   REDIS_URL=redis://localhost:6379
   JWT_SECRET=your_secure_jwt_token_string
   ```
4. Build and start application:
   ```bash
   # Powershell (Windows)
   Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
       $name, $value = $_.Split('=', 2)
       [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
   }
   mvn spring-boot:run
   ```

#### 2. Frontend Setup
1. Navigate to directory:
   ```bash
   cd hostel-frontend
   ```
2. Install npm dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```

---

## 4. Known Limitations & Future Work

* **Mock Payment Gateway**: Credit card/UPI transaction flow uses a simulated success callback ("mark as paid"). In production, this integrates Razorpay/Stripe webhooks with signature checks.
* **Rule-Augmented NLP Triage**: Category and priority auto-triaging uses a weighted token-matching rule array rather than a hosted Large Language Model.
* **Consent Management**: Standard DPDP Act consent timestamp is collected. Next-phase iterations could add granular purpose-limitation consent checklists.
