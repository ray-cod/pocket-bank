package com.banking.cli;

import com.banking.repositories.AccountRepository;
import com.banking.repositories.TransactionRepository;
import com.banking.repositories.UserRepository;
import com.banking.config.DbConnection;
import com.banking.services.AccountService;
import com.banking.services.AuthService;
import com.banking.services.TransactionService;

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

    public static void start(){
        // Initialize repositories
        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        TransactionRepository transactionRepo = new TransactionRepository();

        // Ensure tables exist
        userRepo.createTable();
        accountRepo.createTable();
        transactionRepo.createTable();

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
}

