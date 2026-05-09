package com.kidsbank.service;

import com.kidsbank.model.*;
import com.kidsbank.storage.JsonStorage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for accounts, deposits, withdrawals, and transaction history.
 * Group SE-24 - Virtual Bank for Kids
 */
public class AccountService {

    /**
     * Creates a new account for a user.
     */
    public Account createAccount(String userId, AccountType type) {
        Account account = new Account(userId, type);
        JsonStorage.saveAccount(account);
        return account;
    }

    /**
     * Returns all accounts for a given user.
     */
    public List<Account> getAccountsForUser(String userId) {
        return JsonStorage.findAccountsByUserId(userId);
    }

    /**
     * Returns account by ID, or throws if not found.
     */
    public Account getAccountById(String accountId) {
        return JsonStorage.findAccountById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    /**
     * Deposits money into an account and logs the transaction.
     * @throws IllegalArgumentException if amount <= 0
     */
    public Transaction deposit(String accountId, double amount, String description) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        Account account = getAccountById(accountId);
        account.deposit(amount);
        JsonStorage.saveAccount(account);

        Transaction tx = new Transaction(accountId, TransactionType.DEPOSIT,
                amount, account.getBalance(), description);
        JsonStorage.saveTransaction(tx);
        return tx;
    }

    /**
     * Withdraws money from an account and logs the transaction.
     * @throws IllegalArgumentException if amount <= 0 or insufficient funds
     */
    /**
     * Updates weekly spending limit persisted on the account.
     */
    public void setSpendingLimitForAccount(String accountId, double limit) {
        if (limit < 0) throw new IllegalArgumentException("Spending limit cannot be negative.");
        Account account = getAccountById(accountId);
        account.setSpendingLimit(limit);
        JsonStorage.saveAccount(account);
    }

    /**
     * Applies monthly interest to a savings account only.
     */
    public Transaction applyMonthlyInterest(String accountId, double ratePercent) {
        if (ratePercent <= 0) throw new IllegalArgumentException("Interest rate must be greater than zero.");
        Account account = getAccountById(accountId);
        if (account.getAccountType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException("Interest can only be applied to savings accounts.");
        }
        double interest = account.getBalance() * (ratePercent / 100.0);
        if (interest <= 0) throw new IllegalArgumentException("No interest to apply on zero balance.");
        String desc = String.format("Monthly Interest @ %.2f%%", ratePercent);
        return deposit(accountId, interest, desc);
    }

    public Transaction withdraw(String accountId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        Account account = getAccountById(accountId);
        double limit = account.getSpendingLimit();
        if (limit > 0) {
            double weeklyTotal = getRollingWeekWithdrawalTotal(accountId);
            if (weeklyTotal + amount > limit) {
                throw new IllegalArgumentException(
                        "Weekly spending limit of $" + String.format("%.2f", limit)
                                + " reached. Spent $" + String.format("%.2f", weeklyTotal) + " this week.");
            }
        }
        boolean success = account.withdraw(amount);
        if (!success) throw new IllegalArgumentException("Insufficient funds. Balance: $"
                + String.format("%.2f", account.getBalance()));
        JsonStorage.saveAccount(account);

        Transaction tx = new Transaction(accountId, TransactionType.WITHDRAWAL,
                amount, account.getBalance(), "Withdrawal");
        JsonStorage.saveTransaction(tx);
        return tx;
    }

    /**
     * Credits a task reward to an account (internal — called by TaskService).
     */
    public Transaction creditTaskReward(String accountId, double amount, String taskTitle) {
        Account account = getAccountById(accountId);
        account.deposit(amount);
        JsonStorage.saveAccount(account);

        Transaction tx = new Transaction(accountId, TransactionType.TASK_REWARD,
                amount, account.getBalance(), "Task Reward: " + taskTitle);
        JsonStorage.saveTransaction(tx);
        return tx;
    }

    /**
     * Records a savings transfer transaction (internal — called by SavingsGoalService).
     */
    public Transaction savingsTransfer(String accountId, double amount, String goalName) {
        Account account = getAccountById(accountId);
        boolean success = account.withdraw(amount);
        if (!success) throw new IllegalArgumentException("Insufficient funds for savings transfer.");
        JsonStorage.saveAccount(account);

        Transaction tx = new Transaction(accountId, TransactionType.SAVINGS_TRANSFER,
                amount, account.getBalance(), "Savings: " + goalName);
        JsonStorage.saveTransaction(tx);
        return tx;
    }

    /**
     * Returns transaction history for an account, sorted newest-first.
     */
    public List<Transaction> getTransactions(String accountId) {
        return JsonStorage.findTransactionsByAccountId(accountId);
    }

    /** Rolling 7-day sum of withdrawals (for alerts and limits). */
    public double getRollingWeekWithdrawalTotal(String accountId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        return getTransactions(accountId).stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .filter(t -> t.getDateTime().isAfter(cutoff))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }
}
