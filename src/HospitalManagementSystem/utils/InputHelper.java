package HospitalManagementSystem.utils;

import java.util.Scanner;

public class InputHelper {
    public static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.next());
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
                scanner.nextLine();
            }
        }
    }

    public static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        scanner.nextLine(); // consume any leftover newline
        String line = scanner.nextLine();
        return line.trim();
    }
}
