package HospitalManagementSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Doctor {
    private Connection connection;

    public Doctor(Connection connection){
        this.connection = connection;
    }

    public void viewDoctors(){
        String query = "select * from doctors";
        try{
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()){
                System.out.println("Doctors: ");
                System.out.println("+------------+--------------------+------------------+");
                System.out.println("| Doctor Id  | Name               | Specialization   |");
                System.out.println("+------------+--------------------+------------------+");
                while(resultSet.next()){
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    String specialization = resultSet.getString("specialization");
                    System.out.printf("| %-10s | %-18s | %-16s |\n", id, name, specialization);
                    System.out.println("+------------+--------------------+------------------+");
                }
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public boolean getDoctorById(int id){
        String query = "SELECT * FROM doctors WHERE id = ?";
        try{
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
                preparedStatement.setInt(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()){
                    return resultSet.next();
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public void addDoctor(Scanner scanner){
        System.out.print("Enter Doctor Name: ");
        String name = scanner.next();
        System.out.print("Enter Specialization: ");
        String specialization = scanner.next();
        String query = "INSERT INTO doctors(name, specialization) VALUES(?, ?)";
        try{
            try (PreparedStatement ps = connection.prepareStatement(query)){
                ps.setString(1, name);
                ps.setString(2, specialization);
                int rows = ps.executeUpdate();
                if(rows>0){
                    System.out.println("Doctor added successfully.");
                }else{
                    System.out.println("Failed to add doctor.");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void deleteDoctor(Scanner scanner){
        System.out.print("Enter Doctor Id to delete: ");
        int id = -1;
        try{
            id = scanner.nextInt();
        }catch(Exception e){
            System.out.println("Invalid input. Returning to menu.");
            scanner.nextLine();
            return;
        }
        if(!getDoctorById(id)){
            System.out.println("Doctor with id " + id + " does not exist.");
            return;
        }
        System.out.print("Are you sure you want to delete this doctor and their appointments? (y/n): ");
        String confirm = scanner.next();
        if(!confirm.equalsIgnoreCase("y")){
            System.out.println("Deletion cancelled.");
            return;
        }
        String deleteAppointments = "DELETE FROM appointments WHERE doctor_id = ?";
        String deleteDoctor = "DELETE FROM doctors WHERE id = ?";
        try{
            try (PreparedStatement ps1 = connection.prepareStatement(deleteAppointments)){
                ps1.setInt(1, id);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = connection.prepareStatement(deleteDoctor)){
                ps2.setInt(1, id);
                int rows = ps2.executeUpdate();
                if(rows>0){
                    System.out.println("Doctor and related appointments deleted.");
                }else{
                    System.out.println("Failed to delete doctor.");
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
