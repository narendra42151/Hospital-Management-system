# Hospital Management System

A simple Java console application to manage patients, doctors, and appointments. This repository includes role-based authentication (ADMIN, DOCTOR, PATIENT), admin approval for doctor accounts, and small migration helpers to initialize the required database schema.

**Status:** Demo-ready. Passwords are stored in plaintext for convenience in this sample — DO NOT use in production. See Security notes below.

**Main source:** `src/HospitalManagementSystem/HospitalManagementSystem.java`

**Key additions:**
- `src/HospitalManagementSystem/Auth.java` — login/register/approve flows
- `db/schema.sql` — database schema (patients, doctors, appointments, users)
- `src/HospitalManagementSystem/DebugDumpUsers.java` — helper to inspect `users` table
- Startup schema migration: `ensureSchema` in `HospitalManagementSystem` creates/updates `users` table if missing

## Features
- Role-based login: `ADMIN`, `DOCTOR`, `PATIENT`
- Patient registration creates a `patients` row and an approved `PATIENT` user
- Doctor registration creates a `doctors` row and a pending `DOCTOR` user; an `ADMIN` must approve
- Admin menu includes options to approve and view pending doctors
- Doctor menu shows patients and doctor-specific appointments

## Prerequisites
- Java 21 (JDK) — verified with JavaSE-21 LTS in this workspace
- MySQL server (or compatible) running and accessible

## Database setup
1. Update connection values using environment variables or a `config.properties` file in the project root.
   - Environment variables: `DB_URL`, `DB_USER`, `DB_PASS`
   - OR create `config.properties` with:
     ```properties
     db.url=jdbc:mysql://127.0.0.1:3306/hospital
     db.user=root
     db.pass=yourpassword
     ```
2. Apply the schema (recommended):
   - Using MySQL client (cmd.exe):
     ```bat
     mysql -u root -p < "c:\Users\naren\OneDrive\Desktop\hospital managemnt\Hospital-Management-System\db\schema.sql"
     ```
   - The app also contains a lightweight migration (`ensureSchema`) that will create or alter the `users` table on startup if necessary.

## Compile
1. From a Windows command prompt (cmd.exe) in the project root:
   ```bat
   cd /d "c:\Users\naren\OneDrive\Desktop\hospital managemnt\Hospital-Management-System"
   javac -d bin -cp "lib\mysql-connector-j-9.5.0.jar" src\HospitalManagementSystem\*.java src\HospitalManagementSystem\models\*.java src\HospitalManagementSystem\utils\*.java
   ```

## Run
1. Start the application (interactive):
   ```bat
   java -cp "bin;lib\mysql-connector-j-9.5.0.jar" HospitalManagementSystem.HospitalManagementSystem
   ```
2. The app prompts:
   - `1. Login` — log in as an existing user
   - `2. Register as Patient` — interactive patient registration
   - `3. Register as Doctor (requires admin approval)` — registers a doctor as pending
   - `4. Exit`

## Demo scripts
Several demo input files were used to verify flows; you can reproduce them by redirecting input. Example:
```bat
java -cp "bin;lib\mysql-connector-j-9.5.0.jar" HospitalManagementSystem.HospitalManagementSystem < demo_register_doctor.txt
java -cp "bin;lib\mysql-connector-j-9.5.0.jar" HospitalManagementSystem.HospitalManagementSystem < demo_admin_approve.txt
java -cp "bin;lib\mysql-connector-j-9.5.0.jar" HospitalManagementSystem.HospitalManagementSystem < demo_doc_login.txt
```
These files live in the project root (created during demonstrations):
- `demo_register_doctor.txt`
- `demo_admin_approve.txt`
- `demo_doc_login.txt`

## Debug utilities
- Dump `users` table:
  ```bat
  java -cp "bin;lib\mysql-connector-j-9.5.0.jar" HospitalManagementSystem.DebugDumpUsers
  ```

## Configuration options
- Use `config.properties` to avoid environment variables.
- Or export the environment variables `DB_URL`, `DB_USER`, `DB_PASS` before running.

## Security notes (important)
- Passwords are stored in plaintext in this demo. For any real deployment you must:
  - Hash passwords (bcrypt/argon2) before storing
  - Use TLS/SSL for DB connections if remote
  - Remove the admin normalization behavior from startup (it exists here for demo convenience)

## Files changed in this workspace
- `src/HospitalManagementSystem/Auth.java` (new)
- `src/HospitalManagementSystem/HospitalManagementSystem.java` (updated: auth flow, ensureSchema)
- `db/schema.sql` (updated: `users` table)
- `src/HospitalManagementSystem/DebugDumpUsers.java` (new)

## Next steps (suggested)
- Replace plaintext password handling with hashed passwords (I can implement this).
- Add a `--demo` CLI flag to run the sample flows automatically.
- Add `init-db.bat` to run `schema.sql` and seed data.

## License & attribution
- This project is a learning/demo project. Feel free to adapt it; add a license file if you plan to share publicly.

If you'd like, I can now implement password hashing and a `README` update to reflect it — tell me which option you prefer.
