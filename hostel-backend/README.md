# Hostel Backend - Spring Boot Application

This is the backend service for the Smart Hostel Platform, built with Spring Boot, Spring Security, MongoDB, and Redis.

## Configuration & Local Setup

### 1. Copy Environment Variables Example
The application uses environment variables for all sensitive configuration keys.
Create a local `.env` file from the example:
```bash
cp .env.example .env
```
Edit the newly created `.env` file and fill in your actual credentials (MongoDB, Redis, JWT secret, Mail sender, and Cloudinary keys).

### 2. Loading `.env` and Running the App

#### Option A: PowerShell (Windows)
Run the following commands in your shell to load `.env` variables and start the server:
```powershell
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $name, $value = $_.Split('=', 2)
    [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
}
mvn spring-boot:run
```

#### Option B: Bash / Terminal (macOS/Linux/Git Bash)
```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

#### Option C: IDE Configs (VS Code / IntelliJ)
- **VS Code**: Add `"envFile": "${workspaceFolder}/hostel-backend/.env"` to your `.vscode/launch.json` configuration block.
- **IntelliJ**: Use the **EnvFile** plugin to automatically load `.env` before running `HostelBackendApplication`.
