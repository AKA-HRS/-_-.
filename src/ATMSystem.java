/**
 * Author: Harsh Raj Sinha
 * Date: 30-may-2023
 * Description: This program implements a console-based ATM system using Java and MySQL as the backend database.
 *  *              The program allows users to authenticate with their user ID and PIN. Once authenticated, users
 *  *              can perform various ATM operations such as viewing transaction history, withdrawing money,
 *  *              depositing money, transferring funds to other accounts, and quitting the ATM system.
 *  *              The program connects to a MySQL database to store user account information and transaction details.
 *  *              It utilizes JDBC to establish a connection, execute SQL queries, and retrieve results from the database.
 *  *              The necessary tables, "accounts" and "transactions", are created if they do not already exist in the database.
 *  *              The program utilizes prepared statements to securely handle user input and protect against SQL injection attacks.
 *  *              The transaction history, including the transaction ID, amount, type, and date, is displayed for the authenticated user.
 *  *              Withdrawal, deposit, and transfer operations are performed on the user's account, and the account balance is updated accordingly.
 *  *              The program provides a user-friendly menu to navigate through the ATM functionalities.
 */

import java.sql.*;
import java.util.Scanner;

public class ATMSystem {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bankdatabase";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database.");

            // Create necessary tables if they don't exist
            createTables(connection);   //this is not necessary , to bypass the error i am adding this

            // Taking user ID and PIN
            String userId = promptUser("User ID: ");
            String pin = promptUser("PIN: ");

            // Verifying user credentials
            boolean isAuthenticated = verifyCredentials(connection, userId, pin);
            if (!isAuthenticated) {
                System.out.println("Invalid user ID or PIN. Exiting...");
                return;
            }

            // Main menu
            int choice;
            do {
                System.out.println("\nATM Menu:");
                System.out.println("1. Transaction History");
                System.out.println("2. Withdraw");
                System.out.println("3. Deposit");
                System.out.println("4. Transfer");
                System.out.println("5. Quit");
                choice = Integer.parseInt(promptUser("Enter your choice (1-5): "));

                switch (choice) {
                    case 1 -> displayTransactionHistory(connection, userId);
                    case 2 -> withdrawAmount(connection, userId);
                    case 3 -> depositAmount(connection, userId);
                    case 4 -> transferAmount(connection, userId);
                    case 5 -> System.out.println("Thank you for using the ATM. Goodbye!");  //program will get terminated
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } while (choice != 5);

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method to prompt the user for input
    private static String promptUser(String message) {
        System.out.print(message);
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    // method to create necessary tables if they don't exist
    private static void createTables(Connection connection) throws SQLException {
        String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts (" +
                "user_id VARCHAR(50) PRIMARY KEY," +
                "user_pin VARCHAR(50)," +
                "balance DECIMAL(10, 2)" +
                ")";
        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                "transaction_id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id VARCHAR(50)," +
                "amount DECIMAL(10, 2)," +
                "type VARCHAR(20)," +
                "date DATE" +
                ")";

        Statement statement = connection.createStatement();
        statement.executeUpdate(createAccountsTable);
        statement.executeUpdate(createTransactionsTable);
        statement.close();
    }

    // method to verify user credentials
    private static boolean verifyCredentials(Connection connection, String userId, String pin) throws SQLException {
        String query = "SELECT * FROM accounts WHERE user_id = ? AND user_pin = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, userId);
        statement.setString(2, pin);
        ResultSet resultSet = statement.executeQuery();
        boolean isAuthenticated = resultSet.next();
        resultSet.close();
        statement.close();
        return isAuthenticated;
    }

    // Method to display transaction history
    private static void displayTransactionHistory(Connection connection, String userId) throws SQLException {
        String query = "SELECT * FROM transactions WHERE user_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, userId);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("\nTransaction History:");
        while (resultSet.next()) {
            int transactionId = resultSet.getInt("transaction_id");
            double amount = resultSet.getDouble("amount");
            String type = resultSet.getString("type");
            Timestamp date = resultSet.getTimestamp("datetime");;

            System.out.println("Transaction ID: " + transactionId);
            System.out.println("Amount: " + amount);
            System.out.println("Type: " + type);
            System.out.println("Date: " + date);
            System.out.println("--------------------");
        }

        resultSet.close();
        statement.close();
    }

    // Method to withdraw amount
    private static void withdrawAmount(Connection connection, String userId) throws SQLException {
        double amountToWithdraw = Double.parseDouble(promptUser("Enter the amount to withdraw: "));

        // Check if the user has sufficient balance
        String balanceQuery = "SELECT balance FROM accounts WHERE user_id = ?";
        PreparedStatement balanceStatement = connection.prepareStatement(balanceQuery);
        balanceStatement.setString(1, userId);
        ResultSet balanceResult = balanceStatement.executeQuery();
        double currentBalance;
        if (balanceResult.next()) {
             currentBalance = balanceResult.getDouble("balance");
            if (amountToWithdraw > currentBalance) {
                System.out.println("Insufficient balance. Withdrawal failed.");
                return;
            }
        } else {
            System.out.println("Invalid user ID. Withdrawal failed.");
            return;
        }

        // Perform the withdrawal
        String withdrawalQuery = "UPDATE accounts SET balance = balance - ? WHERE user_id = ?";
        PreparedStatement withdrawalStatement = connection.prepareStatement(withdrawalQuery);
        withdrawalStatement.setDouble(1, amountToWithdraw);
        withdrawalStatement.setString(2, userId);
        int rowsAffected = withdrawalStatement.executeUpdate();

        if (rowsAffected > 0) {
            // Insert a transaction record
            String transactionQuery = "INSERT INTO transactions (user_id, amount, type, datetime) VALUES (?, ?, ?, NOW())";
            PreparedStatement transactionStatement = connection.prepareStatement(transactionQuery);
            transactionStatement.setString(1, userId);
            transactionStatement.setDouble(2, amountToWithdraw);
            transactionStatement.setString(3, "Withdrawal");
            transactionStatement.executeUpdate();

            System.out.println("Withdrawal successful. Updated balance: " + (currentBalance - amountToWithdraw));
        } else {
            System.out.println("Withdrawal failed. Please try again.");
        }

        balanceResult.close();
        balanceStatement.close();
        withdrawalStatement.close();
    }

    // Method to deposit amount
    private static void depositAmount(Connection connection, String userId) throws SQLException {
        //updating the account table
        double amountToDeposit = Double.parseDouble(promptUser("Enter the amount: "));
        String depositeQuery = "UPDATE accounts SET balance = balance + ? WHERE user_id = ?";
        PreparedStatement depositStatement = connection.prepareStatement(depositeQuery);
        depositStatement.setDouble(1, amountToDeposit);
        depositStatement.setString(2, userId);
        int rowsAffected = depositStatement.executeUpdate();

        if (rowsAffected > 0) {
            // Insert a transaction table
            String transactionQuery = "INSERT INTO transactions (user_id, amount, type, datetime) VALUES (?, ?, ?, NOW())";
            PreparedStatement transactionStatement = connection.prepareStatement(transactionQuery);
            transactionStatement.setString(1, userId);
            transactionStatement.setDouble(2, amountToDeposit);
            transactionStatement.setString(3, "Deposit");
            transactionStatement.executeUpdate();

            System.out.println("Transaction successful");
        }
    }

    // Method to transfer amount
    private static void transferAmount(Connection connection, String userId) throws SQLException {
        String recipientUserId = promptUser("Enter the recipient's user ID: ");
        double amountToTransfer = Double.parseDouble(promptUser("Enter the amount to transfer: "));

        // Check if the user has sufficient balance
        String balanceQuery = "SELECT balance FROM accounts WHERE user_id = ?";
        PreparedStatement balanceStatement = connection.prepareStatement(balanceQuery);
        balanceStatement.setString(1, userId);
        ResultSet balanceResult = balanceStatement.executeQuery();
        double currentBalance;
        if (balanceResult.next()) {
            currentBalance = balanceResult.getDouble("balance");
            if (amountToTransfer > currentBalance) {
                System.out.println("Insufficient balance. Last Transaction failed.");
                return;
            }
        } else {
            System.out.println("Invalid user ID. Transaction failed.");
            return;
        }

        // Perform the transfer
        String transferQuery = "UPDATE accounts SET balance = balance - ? WHERE user_id = ?";
        PreparedStatement transferStatement = connection.prepareStatement(transferQuery);
        transferStatement.setDouble(1, amountToTransfer);
        transferStatement.setString(2, userId);
        int rowsAffected = transferStatement.executeUpdate();

        if (rowsAffected > 0) {
            // Update recipient's balance
            String updateRecipientQuery = "UPDATE accounts SET balance = balance + ? WHERE user_id = ?";
            PreparedStatement updateRecipientStatement = connection.prepareStatement(updateRecipientQuery);
            updateRecipientStatement.setDouble(1, amountToTransfer);
            updateRecipientStatement.setString(2, recipientUserId);
            int recipientRowsAffected = updateRecipientStatement.executeUpdate();

            if (recipientRowsAffected > 0) {
                // Insert a transaction record for the sender
                String senderTransactionQuery = "INSERT INTO transactions (user_id, amount, type, datetime) VALUES (?, ?, ?, NOW())";
                PreparedStatement senderTransactionStatement = connection.prepareStatement(senderTransactionQuery);
                senderTransactionStatement.setString(1, userId);
                senderTransactionStatement.setDouble(2, amountToTransfer);
                senderTransactionStatement.setString(3, "Transfer (To: " + recipientUserId + ")");
                senderTransactionStatement.executeUpdate();

                // Insert a transaction record for the recipient
                String recipientTransactionQuery = "INSERT INTO transactions (user_id, amount, type, datetime) VALUES (?, ?, ?, NOW())";
                PreparedStatement recipientTransactionStatement = connection.prepareStatement(recipientTransactionQuery);
                recipientTransactionStatement.setString(1, recipientUserId);
                recipientTransactionStatement.setDouble(2, amountToTransfer);
                recipientTransactionStatement.setString(3, "Transfer (From: " + userId + ")");
                recipientTransactionStatement.executeUpdate();

                System.out.println("Transaction successful. Updated balance: " + (currentBalance - amountToTransfer));
            } else {
                System.out.println("Recipient user ID not found. Transfer failed.");
            }
        } else {
            System.out.println("Last Transaction failed");
        }

        balanceResult.close();
        balanceStatement.close();
        transferStatement.close();
    }


}