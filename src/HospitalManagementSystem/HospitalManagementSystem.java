package HospitalManagementSystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class HospitalManagementSystem {
    private static final String url = "jdbc:mysql://localhost:3306/hospital";
    private static final String username = "root";
    private static final String password = "Admin@123";

    public static void main(String[] args) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        Scanner scanner = new Scanner(System.in);

        // Load DB credentials from environment or config.properties (fallback to defaults below)
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if(dbUrl == null || dbUser == null || dbPass == null){
            Properties props = new Properties();
            try(FileInputStream in = new FileInputStream("config.properties")){
                props.load(in);
                if(dbUrl == null) dbUrl = props.getProperty("db.url");
                if(dbUser == null) dbUser = props.getProperty("db.user");
                if(dbPass == null) dbPass = props.getProperty("db.pass");
            }catch(IOException e){
                // config file not found or unreadable — we'll fall back to defaults
            }
        }
        if(dbUrl == null) dbUrl = url;
        if(dbUser == null) dbUser = username;
        if(dbPass == null) dbPass = password;

        try{
            Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            Patient patient = new Patient(connection, scanner);
            Doctor doctor = new Doctor(connection);
            while(true){

                System.out.println("HOSPITAL MANAGEMENT SYSTEM ");
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
                System.out.println("12. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();

                switch(choice){
                    case 1:
                        // Add Patient
                        patient.addPatient();
                        System.out.println();
                        break;
                    case 2:
                        // View Patients
                        patient.viewPatients();
                        System.out.println();
                        break;
                    case 3:
                        // Update Patient
                        patient.updatePatient();
                        System.out.println();
                        break;
                    case 4:
                        // Delete Patient
                        patient.deletePatient();
                        System.out.println();
                        break;
                    case 5:
                        // Search Patients
                        patient.searchPatientsByName();
                        System.out.println();
                        break;
                    case 6:
                        // View Doctors
                        doctor.viewDoctors();
                        System.out.println();
                        break;
                    case 7:
                        // Add Doctor
                        doctor.addDoctor(scanner);
                        System.out.println();
                        break;
                    case 8:
                        // Remove Doctor
                        doctor.deleteDoctor(scanner);
                        System.out.println();
                        break;
                    case 9:
                        // Book Appointment
                        bookAppointment(patient, doctor, connection, scanner);
                        System.out.println();
                        break;
                    case 10:
                        // View Appointments
                        viewAppointments(connection);
                        System.out.println();
                        break;
                    case 11:
                        // Cancel Appointment
                        cancelAppointment(connection, scanner);
                        System.out.println();
                        break;
                    case 12:
                        System.out.println("THANK YOU! FOR USING HOSPITAL MANAGEMENT SYSTEM!!");
                        return;
                    default:
                        System.out.println("Enter valid choice!!!");
                        break;
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
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(appointmentQuery);
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
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, doctorId);
            preparedStatement.setString(2, appointmentDate);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                int count = resultSet.getInt(1);
                if(count==0){
                    return true;
                }else{
                    return false;
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
        try{
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            System.out.println("Appointments:");
            System.out.println("+----+--------------------+--------------------+------------+");
            System.out.println("| ID | Patient            | Doctor             | Date       |");
            System.out.println("+----+--------------------+--------------------+------------+");
            boolean any = false;
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
        try{
            PreparedStatement ps = connection.prepareStatement(check);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if(!rs.next()){
                System.out.println("Appointment with id " + id + " does not exist.");
                return;
            }
            System.out.print("Are you sure you want to cancel this appointment? (y/n): ");
            String confirm = scanner.next();
            if(!confirm.equalsIgnoreCase("y")){
                System.out.println("Cancellation aborted.");
                return;
            }
            String delete = "DELETE FROM appointments WHERE id = ?";
            PreparedStatement ps2 = connection.prepareStatement(delete);
            ps2.setInt(1, id);
            int rows = ps2.executeUpdate();
            if(rows>0){
                System.out.println("Appointment cancelled.");
            }else{
                System.out.println("Failed to cancel appointment.");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
