package HospitalManagementSystem;

import java.sql.*;
import java.util.Scanner;

public class Auth {
    public static class Session {
        public int userId;
        public String username;
        public String role;
        public Integer linkedPatientId;
        public Integer linkedDoctorId;
        public boolean approved;
    }

    // Simple login (plaintext password) - for demo only
    public static Session login(Connection conn, Scanner scanner){
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        String query = "SELECT id, username, password, role, linked_patient_id, linked_doctor_id, approved FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)){
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    String pw = rs.getString("password");
                    if(!pw.equals(password)){
                        System.out.println("Invalid credentials.");
                        return null;
                    }
                    Session s = new Session();
                    s.userId = rs.getInt("id");
                    s.username = rs.getString("username");
                    s.role = rs.getString("role");
                    s.linkedPatientId = rs.getObject("linked_patient_id")!=null ? rs.getInt("linked_patient_id") : null;
                    s.linkedDoctorId = rs.getObject("linked_doctor_id")!=null ? rs.getInt("linked_doctor_id") : null;
                    s.approved = rs.getInt("approved")==1;
                    if(s.role.equals("DOCTOR") && !s.approved){
                        System.out.println("Doctor account is pending approval by admin.");
                        return null;
                    }
                    return s;
                }else{
                    System.out.println("User not found.");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // Register patient: creates patient record and user linked to it
    public static Session registerPatient(Connection conn, Scanner scanner){
        System.out.print("Choose username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Choose password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Patient name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Age: ");
        int age = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Gender: ");
        String gender = scanner.nextLine().trim();
        try{
            // insert patient
            String pquery = "INSERT INTO patients(name, age, gender) VALUES(?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(pquery, Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1, name);
                ps.setInt(2, age);
                ps.setString(3, gender);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()){
                    if(keys.next()){
                        int patientId = keys.getInt(1);
                        // create user
                        String uquery = "INSERT INTO users(username, password, role, linked_patient_id, approved) VALUES(?, ?, 'PATIENT', ?, 1)";
                        try (PreparedStatement ups = conn.prepareStatement(uquery, Statement.RETURN_GENERATED_KEYS)){
                            ups.setString(1, username);
                            ups.setString(2, password);
                            ups.setInt(3, patientId);
                            ups.executeUpdate();
                            try (ResultSet ukeys = ups.getGeneratedKeys()){
                                if(ukeys.next()){
                                    Session s = new Session();
                                    s.userId = ukeys.getInt(1);
                                    s.username = username;
                                    s.role = "PATIENT";
                                    s.linkedPatientId = patientId;
                                    s.linkedDoctorId = null;
                                    s.approved = true;
                                    return s;
                                }
                            }
                        }
                    }
                }
            }
        }catch(SQLException e){
            if(e.getMessage().contains("Duplicate")){
                System.out.println("Username already exists.");
            }else{
                e.printStackTrace();
            }
        }
        return null;
    }

    // Register doctor: creates doctor record and a user with approved=0 (pending)
    public static void registerDoctor(Connection conn, Scanner scanner){
        System.out.print("Choose username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Choose password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Doctor name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Specialization: ");
        String specialization = scanner.nextLine().trim();
        try{
            // insert doctor
            String dquery = "INSERT INTO doctors(name, specialization) VALUES(?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(dquery, Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1, name);
                ps.setString(2, specialization);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()){
                    if(keys.next()){
                        int doctorId = keys.getInt(1);
                        // create user with approved=0
                        String uquery = "INSERT INTO users(username, password, role, linked_doctor_id, approved) VALUES(?, ?, 'DOCTOR', ?, 0)";
                        try (PreparedStatement ups = conn.prepareStatement(uquery)){
                            ups.setString(1, username);
                            ups.setString(2, password);
                            ups.setInt(3, doctorId);
                            ups.executeUpdate();
                            System.out.println("Doctor account created and pending admin approval.");
                        }
                    }
                }
            }
        }catch(SQLException e){
            if(e.getMessage().contains("Duplicate")){
                System.out.println("Username already exists.");
            }else{
                e.printStackTrace();
            }
        }
    }

    // Admin: view pending doctors
    public static void viewPendingDoctors(Connection conn){
        String q = "SELECT u.id as user_id, u.username, d.id as doctor_id, d.name, d.specialization FROM users u JOIN doctors d ON u.linked_doctor_id = d.id WHERE u.role='DOCTOR' AND u.approved=0";
        try (PreparedStatement ps = conn.prepareStatement(q); ResultSet rs = ps.executeQuery()){
            System.out.println("Pending Doctors:");
            while(rs.next()){
                System.out.printf("UserId:%d Username:%s DoctorId:%d Name:%s Spec:%s\n", rs.getInt("user_id"), rs.getString("username"), rs.getInt("doctor_id"), rs.getString("name"), rs.getString("specialization"));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    // Admin: approve pending doctors interactively
    public static void approvePendingDoctors(Connection conn, Scanner scanner){
        viewPendingDoctors(conn);
        System.out.print("Enter user id or username of doctor to approve (or 0 to cancel): ");
        String input = scanner.nextLine().trim();
        if(input.equals("0")) return;
        // Try to parse as id first
        try{
            int uid = Integer.parseInt(input);
            String q = "UPDATE users SET approved=1 WHERE id = ? AND role='DOCTOR'";
            try (PreparedStatement ps = conn.prepareStatement(q)){
                ps.setInt(1, uid);
                int rows = ps.executeUpdate();
                if(rows>0) System.out.println("Doctor approved."); else System.out.println("No such pending doctor user id.");
            }
        }catch(NumberFormatException nfe){
            // treat as username
            String q = "UPDATE users SET approved=1 WHERE username = ? AND role='DOCTOR'";
            try (PreparedStatement ps = conn.prepareStatement(q)){
                ps.setString(1, input);
                int rows = ps.executeUpdate();
                if(rows>0) System.out.println("Doctor approved by username."); else System.out.println("No such pending doctor username.");
            }catch(SQLException e){
                e.printStackTrace();
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
