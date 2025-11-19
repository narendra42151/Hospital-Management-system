package HospitalManagementSystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import HospitalManagementSystem.DBConfig;
import java.util.Scanner;

public class HospitalManagementSystem {
    // Use DBConfig to allow environment/properties/default override

    public static void main(String[] args) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        Scanner scanner = new Scanner(System.in);
        try{
            String url = DBConfig.getUrl();
            String username = DBConfig.getUser();
            String password = DBConfig.getPass();
            Connection connection = DriverManager.getConnection(url, username, password);
            // Ensure required schema (users table + columns) exists or migrate it
            ensureSchema(connection);
            // Authenticate user (login / register)
            Patient patient = new Patient(connection, scanner);
            Doctor doctor = new Doctor(connection);

            Auth.Session session = null;
            while(session == null){
                System.out.println("Welcome - choose action:");
                System.out.println("1. Login");
                System.out.println("2. Register as Patient");
                System.out.println("3. Register as Doctor (requires admin approval)");
                System.out.println("4. Exit");
                System.out.print("Enter choice: ");
                int c = scanner.nextInt();
                scanner.nextLine();
                if(c==1){
                    session = Auth.login(connection, scanner);
                    if(session!=null){
                        System.out.println("Logged in as: " + session.username + " (" + session.role + ")");
                    }
                }else if(c==2){
                    session = Auth.registerPatient(connection, scanner);
                    if(session!=null){
                        System.out.println("Patient registered and logged in as: " + session.username);
                    }
                }else if(c==3){
                    Auth.registerDoctor(connection, scanner);
                    System.out.println("Doctor registration submitted. Waiting for admin approval.");
                }else if(c==4){
                    System.out.println("Goodbye.");
                    return;
                }else{
                    System.out.println("Invalid choice");
                }
            }

            // Role-based menu
            if(session.role.equals("ADMIN")){
                // Admin: full access + approve doctors
                while(true){
                    System.out.println("ADMIN MENU");
                    System.out.println("1. Add Patient");
                    System.out.println("2. View Patients");
                    System.out.println("3. Update Patient");
                    System.out.println("4. Delete Patient");
                    System.out.println("5. Search Patients by Name");
                    System.out.println("6. View Doctors");
                    System.out.println("7. Add Doctor");
                    System.out.println("8. Remove Doctor");
                    System.out.println("9. Book Appointment");
                    System.out.println("10. View Appointments");
                    System.out.println("11. Cancel Appointment");
                    System.out.println("12. Approve Pending Doctors");
                    System.out.println("13. View Pending Doctors");
                    System.out.println("14. Exit");
                    System.out.print("Enter your choice: ");
                    int choice = scanner.nextInt();
                    scanner.nextLine();
                    switch(choice){
                        case 1: patient.addPatient(); break;
                        case 2: patient.viewPatients(); break;
                        case 3: patient.updatePatient(); break;
                        case 4: patient.deletePatient(); break;
                        case 5: patient.searchPatientsByName(); break;
                        case 6: doctor.viewDoctors(); break;
                        case 7: doctor.addDoctor(scanner); break;
                        case 8: doctor.deleteDoctor(scanner); break;
                        case 9: bookAppointment(patient, doctor, connection, scanner); break;
                        case 10: viewAppointments(connection); break;
                        case 11: cancelAppointment(connection, scanner); break;
                        case 12: Auth.approvePendingDoctors(connection, scanner); break;
                        case 13: Auth.viewPendingDoctors(connection); break;
                        case 14: System.out.println("Exiting admin. Goodbye."); return;
                        default: System.out.println("Enter valid choice!!!"); break;
                    }
                    System.out.println();
                }
            } else if(session.role.equals("DOCTOR")){
                if(!session.approved){
                    System.out.println("Your doctor account is pending approval. Contact admin.");
                    return;
                }
                // Doctor: limited access
                while(true){
                    System.out.println("DOCTOR MENU");
                    System.out.println("1. View Patients");
                    System.out.println("2. View My Appointments");
                    System.out.println("3. Exit");
                    System.out.print("Enter choice: ");
                    int choice = scanner.nextInt();
                    scanner.nextLine();
                    switch(choice){
                        case 1: patient.viewPatients(); break;
                        case 2: viewAppointmentsForDoctor(connection, session.linkedDoctorId); break;
                        case 3: System.out.println("Goodbye."); return;
                        default: System.out.println("Invalid choice"); break;
                    }
                }
            } else if(session.role.equals("PATIENT")){
                // Patient: only own operations
                while(true){
                    System.out.println("PATIENT MENU");
                    System.out.println("1. View My Appointments");
                    System.out.println("2. Book Appointment");
                    System.out.println("3. Exit");
                    System.out.print("Enter choice: ");
                    int choice = scanner.nextInt();
                    scanner.nextLine();
                    switch(choice){
                        case 1: viewAppointmentsForPatient(connection, session.linkedPatientId); break;
                        case 2: bookAppointmentAsPatient(connection, scanner, session.linkedPatientId); break;
                        case 3: System.out.println("Goodbye."); return;
                        default: System.out.println("Invalid choice"); break;
                    }
                }
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
    }


    public static void bookAppointment(Patient patient, Doctor doctor, Connection connection, Scanner scanner){
        System.out.print("Enter Patient Id: ");
        int patientId = scanner.nextInt();
        System.out.print("Enter Doctor Id: ");
        int doctorId = scanner.nextInt();
        System.out.print("Enter appointment date (YYYY-MM-DD): ");
        String appointmentDate = scanner.next();
        if(patient.getPatientById(patientId) && doctor.getDoctorById(doctorId)){
                if(checkDoctorAvailability(doctorId, appointmentDate, connection)){
                    String appointmentQuery = "INSERT INTO appointments(patient_id, doctor_id, appointment_date) VALUES(?, ?, ?)";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(appointmentQuery)){
                        preparedStatement.setInt(1, patientId);
                        preparedStatement.setInt(2, doctorId);
                        preparedStatement.setString(3, appointmentDate);
                        int rowsAffected = preparedStatement.executeUpdate();
                        if(rowsAffected>0){
                            System.out.println("Appointment Booked!");
                        }else{
                            System.out.println("Failed to Book Appointment!");
                        }
                    }catch (SQLException e){
                        e.printStackTrace();
                    }
                }else{
                System.out.println("Doctor not available on this date!!");
            }
        }else{
            System.out.println("Either doctor or patient doesn't exist!!!");
        }
    }

    public static boolean checkDoctorAvailability(int doctorId, String appointmentDate, Connection connection){
        String query = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1, doctorId);
            preparedStatement.setString(2, appointmentDate);
            try (ResultSet resultSet = preparedStatement.executeQuery()){
                if(resultSet.next()){
                    int count = resultSet.getInt(1);
                    return count == 0;
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static void viewAppointments(Connection connection){
        String query = "SELECT a.id, p.name AS patient_name, d.name AS doctor_name, a.appointment_date " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "JOIN doctors d ON a.doctor_id = d.id " +
                "ORDER BY a.appointment_date";
        System.out.println("Appointments:");
        System.out.println("+----+--------------------+--------------------+------------+");
        System.out.println("| ID | Patient            | Doctor             | Date       |");
        System.out.println("+----+--------------------+--------------------+------------+");
        boolean any = false;
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()){
            while(rs.next()){
                any = true;
                int id = rs.getInt("id");
                String patientName = rs.getString("patient_name");
                String doctorName = rs.getString("doctor_name");
                String date = rs.getString("appointment_date");
                System.out.printf("| %-2s | %-18s | %-18s | %-10s |\n", id, patientName, doctorName, date);
                System.out.println("+----+--------------------+--------------------+------------+");
            }
            if(!any){
                System.out.println("No appointments found.");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static void cancelAppointment(Connection connection, Scanner scanner){
        System.out.print("Enter Appointment Id to cancel: ");
        int id = -1;
        try{
            id = scanner.nextInt();
        }catch(Exception e){
            System.out.println("Invalid input. Returning to menu.");
            scanner.nextLine();
            return;
        }
        String check = "SELECT * FROM appointments WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(check)){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if(!rs.next()){
                    System.out.println("Appointment with id " + id + " does not exist.");
                    return;
                }
            }
            System.out.print("Are you sure you want to cancel this appointment? (y/n): ");
            String confirm = scanner.next();
            if(!confirm.equalsIgnoreCase("y")){
                System.out.println("Cancellation aborted.");
                return;
            }
            String delete = "DELETE FROM appointments WHERE id = ?";
            try (PreparedStatement ps2 = connection.prepareStatement(delete)){
                ps2.setInt(1, id);
                int rows = ps2.executeUpdate();
                if(rows>0){
                    System.out.println("Appointment cancelled.");
                }else{
                    System.out.println("Failed to cancel appointment.");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    // View appointments filtered by doctor id
    public static void viewAppointmentsForDoctor(Connection connection, Integer doctorId){
        if(doctorId==null){
            System.out.println("No doctor id linked to user.");
            return;
        }
        String query = "SELECT a.id, p.name AS patient_name, d.name AS doctor_name, a.appointment_date " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "JOIN doctors d ON a.doctor_id = d.id " +
                "WHERE a.doctor_id = ? " +
                "ORDER BY a.appointment_date";
        System.out.println("My Appointments:");
        System.out.println("+----+--------------------+--------------------+------------+");
        System.out.println("| ID | Patient            | Doctor             | Date       |");
        System.out.println("+----+--------------------+--------------------+------------+");
        boolean any = false;
        try (PreparedStatement ps = connection.prepareStatement(query)){
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    any = true;
                    int id = rs.getInt("id");
                    String patientName = rs.getString("patient_name");
                    String doctorName = rs.getString("doctor_name");
                    String date = rs.getString("appointment_date");
                    System.out.printf("| %-2s | %-18s | %-18s | %-10s |\n", id, patientName, doctorName, date);
                    System.out.println("+----+--------------------+--------------------+------------+");
                }
                if(!any){
                    System.out.println("No appointments found.");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    // View appointments filtered by patient id
    public static void viewAppointmentsForPatient(Connection connection, Integer patientId){
        if(patientId==null){
            System.out.println("No patient id linked to user.");
            return;
        }
        String query = "SELECT a.id, p.name AS patient_name, d.name AS doctor_name, a.appointment_date " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "JOIN doctors d ON a.doctor_id = d.id " +
                "WHERE a.patient_id = ? " +
                "ORDER BY a.appointment_date";
        System.out.println("My Appointments:");
        System.out.println("+----+--------------------+--------------------+------------+");
        System.out.println("| ID | Patient            | Doctor             | Date       |");
        System.out.println("+----+--------------------+--------------------+------------+");
        boolean any = false;
        try (PreparedStatement ps = connection.prepareStatement(query)){
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    any = true;
                    int id = rs.getInt("id");
                    String patientName = rs.getString("patient_name");
                    String doctorName = rs.getString("doctor_name");
                    String date = rs.getString("appointment_date");
                    System.out.printf("| %-2s | %-18s | %-18s | %-10s |\n", id, patientName, doctorName, date);
                    System.out.println("+----+--------------------+--------------------+------------+");
                }
                if(!any){
                    System.out.println("No appointments found.");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    // Book appointment when user is a patient (no need to enter patient id)
    public static void bookAppointmentAsPatient(Connection connection, Scanner scanner, Integer patientId){
        if(patientId==null){
            System.out.println("No patient linked to your account.");
            return;
        }
        System.out.print("Enter Doctor Id: ");
        int doctorId = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Enter appointment date (YYYY-MM-DD): ");
        String appointmentDate = scanner.next();
        if(checkDoctorAvailability(doctorId, appointmentDate, connection)){
            String appointmentQuery = "INSERT INTO appointments(patient_id, doctor_id, appointment_date) VALUES(?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(appointmentQuery)){
                preparedStatement.setInt(1, patientId);
                preparedStatement.setInt(2, doctorId);
                preparedStatement.setString(3, appointmentDate);
                int rowsAffected = preparedStatement.executeUpdate();
                if(rowsAffected>0){
                    System.out.println("Appointment Booked!");
                }else{
                    System.out.println("Failed to Book Appointment!");
                }
            }catch (SQLException e){
                e.printStackTrace();
            }
        }else{
            System.out.println("Doctor not available on this date!!");
        }
    }

    // Ensure users table exists and has required columns; run lightweight migrations
    private static void ensureSchema(Connection conn){
        try{
            // Check if `users` table exists in current database
            String checkTable = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'";
            try (PreparedStatement ps = conn.prepareStatement(checkTable); ResultSet rs = ps.executeQuery()){
                if(rs.next() && rs.getInt(1) == 0){
                    System.out.println("Creating users table (migration)");
                    String create = "CREATE TABLE IF NOT EXISTS users ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "username VARCHAR(100) NOT NULL UNIQUE,"
                            + "password VARCHAR(255) NOT NULL,"
                            + "role ENUM('ADMIN','DOCTOR','PATIENT') NOT NULL,"
                            + "linked_patient_id INT DEFAULT NULL,"
                            + "linked_doctor_id INT DEFAULT NULL,"
                            + "approved TINYINT(1) DEFAULT 0,"
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")";
                    try (PreparedStatement createPs = conn.prepareStatement(create)){
                        createPs.executeUpdate();
                    }
                    // insert default admin
                    String insAdmin = "INSERT INTO users (username, password, role, approved) SELECT 'admin','admin','ADMIN',1 WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin')";
                    try (PreparedStatement a = conn.prepareStatement(insAdmin)){
                        a.executeUpdate();
                    }
                    // Ensure admin row has the expected role/password/approved flag (normalize)
                    try (PreparedStatement na = conn.prepareStatement("UPDATE users SET role='ADMIN', password='admin', approved=1 WHERE username='admin'")){
                        na.executeUpdate();
                    }
                }
            }

            // Ensure columns exist: linked_patient_id, linked_doctor_id, approved
            String[] cols = {"linked_patient_id","linked_doctor_id","approved"};
            for(String col: cols){
                String checkCol = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = '"+col+"'";
                try (PreparedStatement ps2 = conn.prepareStatement(checkCol); ResultSet rs2 = ps2.executeQuery()){
                    if(rs2.next() && rs2.getInt(1) == 0){
                        System.out.println("Adding missing column to users: " + col);
                        String alter = null;
                        if(col.equals("linked_patient_id")) alter = "ALTER TABLE users ADD COLUMN linked_patient_id INT DEFAULT NULL";
                        if(col.equals("linked_doctor_id")) alter = "ALTER TABLE users ADD COLUMN linked_doctor_id INT DEFAULT NULL";
                        if(col.equals("approved")) alter = "ALTER TABLE users ADD COLUMN approved TINYINT(1) DEFAULT 0";
                        if(alter!=null){
                            try (PreparedStatement aps = conn.prepareStatement(alter)){
                                aps.executeUpdate();
                            }
                        }
                    }
                }
            }
            // Normalize admin user in any case (ensure correct role/password/approved)
            try (PreparedStatement na2 = conn.prepareStatement("UPDATE users SET role='ADMIN', password='admin', approved=1 WHERE username='admin'")){
                na2.executeUpdate();
            }catch(SQLException ignored){ }
        }catch(SQLException e){
            System.out.println("Schema migration check failed: " + e.getMessage());
        }
    }
}
