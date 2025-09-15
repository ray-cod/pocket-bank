package com.banking.cli;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.models.User;
import com.banking.repositories.AccountRepository;
import com.banking.repositories.TransactionRepository;
import com.banking.repositories.UserRepository;
import com.banking.config.DbConnection;
import com.banking.services.AccountService;
import com.banking.services.AuthService;
import com.banking.services.TransactionService;
import com.banking.utils.enums.TransactionType;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * CliApp - application entrypoint that wires repositories, services and the MenuRenderer.
 *
 * Responsibilities:
 *  - Create DB tables
 *  - Wire repositories and services
 *  - Optionally seed demo data
 *  - Start the MenuRenderer loop
 */
public class ATM {

    public static void start(String[] args){
        boolean demo = false;
        for (String a : args) {
            if ("--demo".equalsIgnoreCase(a) || "-d".equalsIgnoreCase(a)) {
                demo = true;
            }
        }

        // Initialize repositories
        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        TransactionRepository transactionRepo = new TransactionRepository();

        // Ensure tables exist
        userRepo.createTable();
        accountRepo.createTable();
        transactionRepo.createTable();

        // Seed demo data if requested
        if (demo) {
            seedDemoData(userRepo, accountRepo, transactionRepo);
        }

        // Wire services
        AuthService authService = new AuthService(userRepo);
        AccountService accountService = new AccountService(accountRepo, transactionRepo);
        TransactionService transactionService = new TransactionService(accountRepo, transactionRepo);

        // Start CLI
        MenuRenderer renderer = new MenuRenderer(authService, accountService, transactionService);
        renderer.run();

        // Graceful shutdown
        try {
            // Close DB (H2 in-memory shuts down when JVM exits; explicit shutdown not required)
            DbConnection.getConnection().close();
        } catch (SQLException ignored) {
        }
    }

    private static void seedDemoData(UserRepository userRepo, AccountRepository accountRepo, TransactionRepository txRepo) {
        try {
            // Demo user 1
            User u1 = new User("raimi", "1234");
            userRepo.save(u1);
            Account a1 = new Account(u1);
            a1.setBalance(BigDecimal.valueOf(2450.00));
            accountRepo.save(a1);
            Transaction t1 = new Transaction(a1.getAccountId(), TransactionType.DEPOSIT, BigDecimal.valueOf(2450.00), a1.getBalance(), "Initial deposit");
            txRepo.save(t1);

            // Demo user 2
            User u2 = new User("jane", "4321");
            userRepo.save(u2);
            Account a2 = new Account(u2);
            a2.setBalance(BigDecimal.valueOf(10000.00));
            accountRepo.save(a2);
            Transaction t2 = new Transaction(a2.getAccountId(), TransactionType.DEPOSIT, BigDecimal.valueOf(10000.00), a2.getBalance(), "Initial deposit");
            txRepo.save(t2);

            System.out.println("✔ Demo data seeded (users: raimi/1234, jane/4321)");
        } catch (Exception e) {
            System.err.println("Failed to seed demo data: " + e.getMessage());
        }
    }
}

