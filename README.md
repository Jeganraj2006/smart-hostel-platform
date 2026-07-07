# Smart Hostel Platform

A comprehensive hostel management system built with Spring Boot (backend) and React (frontend).

## Project Structure
- [hostel-backend](file:///e:/Hostel%20Management%20Software/hostel-backend/): Spring Boot backend application.
- [hostel-frontend](file:///e:/Hostel%20Management%20Software/hostel-frontend/): React frontend application.

---

## Backend Setup Guide

### 1. Environment Configuration
The backend application requires several environment variables for database connection, security, mail sender, and file uploads.

1. Navigate to the backend directory:
   ```bash
   cd hostel-backend
   ```
2. Copy the example environment file to create your own local environment file:
   ```bash
   cp .env.example .env
   ```
3. Open the newly created `.env` file and replace the placeholder values with your actual credentials.

### 2. Loading Environment Variables

To run the Spring Boot application locally with the environment variables loaded from the `.env` file, use one of the following methods depending on your environment:

#### Method A: Using PowerShell (Windows)
Run the following script to load `.env` variables and start the application:
```powershell
# Load .env variables into the current session
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $name, $value = $_.Split('=', 2)
    [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
}

# Run the application
mvn spring-boot:run
```

#### Method B: Using Bash/Git Bash (Windows/macOS/Linux)
Export the environment variables and run:
```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

#### Method C: VS Code (Spring Boot Dashboard / Java Runner)
If you run/debug the application using the VS Code Spring Boot Extension, configure `.vscode/launch.json`:
1. Open or create `.vscode/launch.json` at the root of the workspace.
2. Add the `"envFile"` parameter pointing to the `.env` file:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "HostelBackendApplication",
         "request": "launch",
         "mainClass": "com.hostel.hostel_backend.HostelBackendApplication",
         "projectName": "hostel-backend",
         "envFile": "${workspaceFolder}/hostel-backend/.env"
       }
     ]
   }
   ```

#### Method D: IntelliJ IDEA
1. Install the **EnvFile** plugin from the marketplace.
2. Edit your Run/Debug Configuration for `HostelBackendApplication`.
3. Select the **EnvFile** tab.
4. Enable EnvFile and add the path to your `hostel-backend/.env` file.
