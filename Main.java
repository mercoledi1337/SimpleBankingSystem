package banking;

import org.sqlite.SQLiteDataSource;

import java.math.BigInteger;
import java.util.*;
import java.sql.*;

public class Main {

    static Statement statement = null;
    static Connection con = null;
    static int count = 0;

    public static boolean luhn(String card) {

        int nDigits = card.length();

        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--)
        {

            int d = card.charAt(i) - '0';

            if (isSecond == true)
                d = d * 2;

            // We add two digits to handle
            // cases that make two digits
            // after doubling
            nSum += d / 10;
            nSum += d % 10;

            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    public static void connect() {
        try {
            String url = "jdbc:sqlite:card.s3db";
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(url);
            con = DriverManager.getConnection(url);
            statement = con.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTable() {
        try {
            String table = "create table if not exists card (" +
                    "id INTEGER," +
                    "number TEXT," +
                    "pin TEXT," +
                    "balance INTEGER DEFAULT 0);";
            statement.executeUpdate(table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createAnAccount() {
        Random random = new Random();
        String card = "";
        // generating PIN
        int pin4 = random.nextInt(10);
        int pin3 = random.nextInt(10);
        int pin2 = random.nextInt(10);
        int pin1 = random.nextInt(10);
        String pin = "" + pin4 + pin3 + pin2 + pin1;
        // generating card Number
        BigInteger maxLimit = new BigInteger("4000009999999999");
        BigInteger minLimit = new BigInteger("4000000000000000");
        do {
            BigInteger bigInteger = maxLimit.subtract(minLimit);
            int len = maxLimit.bitLength();
            BigInteger cardNumber = new BigInteger(len, random);
            if (cardNumber.compareTo(minLimit) < 0)
                cardNumber = cardNumber.add(minLimit);
            if (cardNumber.compareTo(bigInteger) >= 0)
                cardNumber = cardNumber.mod(bigInteger).add(minLimit);
            card = cardNumber.toString();

        } while (!luhn(card));

        // printing
        System.out.println("");
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(card);
        System.out.println("Your card PIN:");
        System.out.println(pin + "\n");
        // creating acc
        String acc = "INSERT INTO card (id, number, pin) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = con.prepareStatement(acc)) {
            preparedStatement.setInt(1,count);
            preparedStatement.setString(2, card);
            preparedStatement.setString(3, pin);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("coś nie tak");
        }
        count++;
    }

    public static void logIntoAccount () throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your card number: ");
        String card = scanner.next();
        System.out.println("Enter your PIN: ");
        String pin = scanner.next();
        System.out.println();
        String logging = "SELECT count(*) FROM card WHERE number = '" + card + "' AND pin = '" + pin + "'";
        ResultSet rs = statement.executeQuery(logging);
        if (rs.getInt(1) == 0) {
            System.out.println("Wrong card number or PIN! \n");
        } else {
            System.out.println("You have successfully logged in! \n");
            insideAccount(card);
        }


    }

    public static void insideAccount (String card) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1. Balance");
            System.out.println("2. Add income");
            System.out.println("3. Do transfer");
            System.out.println("4. Close account");
            System.out.println("5. Log out");
            System.out.println("0. Exit");
            int choice = scanner.nextInt();
            if (choice == 1) {
                String balance = "SELECT balance FROM card WHERE number = '" + card + "'";
                try (ResultSet rs = statement.executeQuery(balance);){
                    System.out.println(rs.getString(1));
                } catch (SQLException e) {
                    System.out.println("Problem with checking balance");
                }

            } else if (choice == 2) {
                System.out.println("Enter income:");
                int moneyToAdd = scanner.nextInt();
                String add = "UPDATE card SET balance = balance + '" + moneyToAdd + "' WHERE number = '" + card + "'";
                try (PreparedStatement preparedStatement = con.prepareStatement(add)) {
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Problem with adding money");
                }
                System.out.println("Income was added!");
            } else if (choice == 3) {
                System.out.println("\nTransfer\n" +
                        "Enter card number:");
                String whereSendMoney = scanner.next();
                System.out.println();
                if (card.equals(whereSendMoney)) {
                    System.out.println("You can't transfer money to the same account!");
                } else {
                    String numberToEqual = "UPDATE card SET balance = balance + ? WHERE number = '" + whereSendMoney + "'";
                    String sub = "UPDATE card SET balance = balance - ? WHERE number ='" + card + "'";
                    String currentBalance = "SELECT * FROM card WHERE number = '" + card + "'";
                    String checkingNumber = "SELECT count(number) FROM card WHERE number = '" + whereSendMoney + "'";
                    String checkingNumber1 = "SELECT number FROM card WHERE number = '" + whereSendMoney + "'";
                    ResultSet rs = statement.executeQuery(checkingNumber);
                    if (luhn(whereSendMoney) && rs.getInt(1) >= 0){
                        try {
                            ResultSet rs2 = statement.executeQuery(checkingNumber1);
                            if (whereSendMoney.equals(rs2.getString("number"))) {
                                ResultSet rs1 = statement.executeQuery(currentBalance);
                                System.out.println("Enter how much money you want to transfer:");
                                int moneyToSend = scanner.nextInt();
                                if (rs1.getInt("balance") > moneyToSend) {
                                    PreparedStatement preparedStatement = con.prepareStatement(numberToEqual);
                                    PreparedStatement preparedStatement1 = con.prepareStatement(sub);
                                    preparedStatement.setInt(1, moneyToSend);
                                    preparedStatement1.setInt(1, moneyToSend);
                                    preparedStatement.executeUpdate();
                                    preparedStatement1.executeUpdate();
                                    System.out.println("Success!");
                                } else {
                                    System.out.println("Not enough money!");
                                }
                            } else if (!luhn(whereSendMoney)) {
                                System.out.println("Probably you made a mistake in the card number. Please try again! \n");

                            } else {
                                System.out.println("Such a card does not exist.");
                            }
                        } catch (SQLException e) {
                            System.out.println("Such a card does not exist.\n");
                        }
                    } else if (!luhn(whereSendMoney)) {
                        System.out.println("Probably you made a mistake in the card number. Please try again! \n");
                    } else {
                        System.out.println("Such a card does not exist.");
                    }
                }
            } else if (choice == 4) {
                String drop  = "DELETE FROM card WHERE number = '" + card +"'";
                statement.executeUpdate(drop);
                System.out.println("The account has been closed!");
            } else if (choice == 5) {
                break;
            } else if (choice == 0) {
                System.out.println("Bye!");
                if (con != null) con.close();
                System.exit(0);
                scanner.close();


            } else {
                System.out.println("\nWrong number!\n");
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        connect();
        createTable();
            Scanner scanner = new Scanner(System.in);
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
        int i = 0;
        while (i == 0) {
            int choose = scanner.nextInt();

            if (choose == 1) {
                createAnAccount();
            } else if (choose == 2){ logIntoAccount();
            } else if (choose == 0) {
                System.out.println("Bye!");
                if (con != null) con.close();
                System.exit(0);
                scanner.close();
                i = 5;

            } else {
                System.out.println("Wrong choice!");
            }
            System.out.println("1. Create an account");
            System.out.println("2. Log into account");
            System.out.println("0. Exit\n");
        }
    }
}