package HospitalManagementSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Patient {
    private Connection connection;
    private Scanner scanner;

    public Patient(Connection connection, Scanner scanner){
        this.connection = connection;
        this.scanner = scanner;
    }

    public void addPatient(){
        System.out.print("Enter Patient Name: ");
        String name = scanner.next();
        System.out.print("Enter Patient Age: ");
        int age = scanner.nextInt();
        System.out.print("Enter Patient Gender: ");
        String gender = scanner.next();

        try{
            String query = "INSERT INTO patients(name, age, gender) VALUES(?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, age);
            preparedStatement.setString(3, gender);
            int affectedRows = preparedStatement.executeUpdate();
            if(affectedRows>0){
                System.out.println("Patient Added Successfully!!");
            }else{
                System.out.println("Failed to add Patient!!");
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void viewPatients(){
        String query = "select * from patients";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            System.out.println("Patients: ");
            System.out.println("+------------+--------------------+----------+------------+");
            System.out.println("| Patient Id | Name               | Age      | Gender     |");
            System.out.println("+------------+--------------------+----------+------------+");
            while(resultSet.next()){
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int age = resultSet.getInt("age");
                String gender = resultSet.getString("gender");
                System.out.printf("| %-10s | %-18s | %-8s | %-10s |\n", id, name, age, gender);
                System.out.println("+------------+--------------------+----------+------------+");
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public boolean getPatientById(int id){
        String query = "SELECT * FROM patients WHERE id = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            }else{
                return false;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public void updatePatient(){
        System.out.print("Enter Patient Id to update: ");
        int id = -1;
        try{
            id = scanner.nextInt();
        }catch(Exception e){
            System.out.println("Invalid input. Returning to menu.");
            scanner.nextLine();
            return;
        }
        if(!getPatientById(id)){
            System.out.println("Patient with id " + id + " does not exist.");
            return;
        }
        System.out.print("Enter new name: ");
        String name = scanner.next();
        System.out.print("Enter new age: ");
        int age = scanner.nextInt();
        System.out.print("Enter new gender: ");
        String gender = scanner.next();

        String query = "UPDATE patients SET name = ?, age = ?, gender = ? WHERE id = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, age);
            preparedStatement.setString(3, gender);
            preparedStatement.setInt(4, id);
            int rows = preparedStatement.executeUpdate();
            if(rows>0){
                System.out.println("Patient updated successfully.");
            }else{
                System.out.println("Failed to update patient.");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void deletePatient(){
        System.out.print("Enter Patient Id to delete: ");
        int id = -1;
        try{
            id = scanner.nextInt();
        }catch(Exception e){
            System.out.println("Invalid input. Returning to menu.");
            scanner.nextLine();
            return;
        }
        if(!getPatientById(id)){
            System.out.println("Patient with id " + id + " does not exist.");
            return;
        }
        System.out.print("Are you sure you want to delete this patient and their appointments? (y/n): ");
        String confirm = scanner.next();
        if(!confirm.equalsIgnoreCase("y")){
            System.out.println("Deletion cancelled.");
            return;
        }
        // Delete appointments for patient first
        String deleteAppointments = "DELETE FROM appointments WHERE patient_id = ?";
        String deletePatient = "DELETE FROM patients WHERE id = ?";
        try{
            PreparedStatement ps1 = connection.prepareStatement(deleteAppointments);
            ps1.setInt(1, id);
            ps1.executeUpdate();

            PreparedStatement ps2 = connection.prepareStatement(deletePatient);
            ps2.setInt(1, id);
            int rows = ps2.executeUpdate();
            if(rows>0){
                System.out.println("Patient and related appointments deleted.");
            }else{
                System.out.println("Failed to delete patient.");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void searchPatientsByName(){
        System.out.print("Enter name or partial name to search: ");
        String name = scanner.next();
        String query = "SELECT * FROM patients WHERE name LIKE ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, "%" + name + "%");
            ResultSet resultSet = preparedStatement.executeQuery();
            System.out.println("Search results: ");
            System.out.println("+------------+--------------------+----------+------------+");
            System.out.println("| Patient Id | Name               | Age      | Gender     |");
            System.out.println("+------------+--------------------+----------+------------+");
            boolean any = false;
            while(resultSet.next()){
                any = true;
                int id = resultSet.getInt("id");
                String pname = resultSet.getString("name");
                int age = resultSet.getInt("age");
                String gender = resultSet.getString("gender");
                System.out.printf("| %-10s | %-18s | %-8s | %-10s |\n", id, pname, age, gender);
                System.out.println("+------------+--------------------+----------+------------+");
            }
            if(!any){
                System.out.println("No patients found matching: " + name);
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

}
