package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransferService {
    @Getter
    private final AccountsRepository accountsRepository;
    private NotificationService notificationService;

    public TransferService(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    private final Map<String, Object> accountLocks = new ConcurrentHashMap<>();

    public boolean transferMoney(String accountFromNumber, String accountToNumber, BigDecimal amount) {
        if (isInvalidTransferAmount(amount)) {
            return false;
        }

        Account accountFrom = getAccount(accountFromNumber);
        Account accountTo = getAccount(accountToNumber);

        if (accountFrom == null || accountTo == null) {
            return false; // Account(s) not found
        }

        Object lock1 = getAccountLock(accountFromNumber);
        Object lock2 = getAccountLock(accountToNumber);

        synchronized (lock1) {
            synchronized (lock2) {
                if (canTransfer(accountFrom, accountTo, amount)) {
                    performTransfer(accountFrom, accountTo, amount);
                    return true; // Transfer successful
                }
            }
        }

        return false; // Transfer failed
    }

    private boolean isInvalidTransferAmount(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private Account getAccount(String accountNumber) {
        return accountsRepository.getAccount(accountNumber);
    }

    private Object getAccountLock(String accountNumber) {
        return accountLocks.computeIfAbsent(accountNumber, k -> new Object());
    }

    private boolean canTransfer(Account accountFrom, Account accountTo, BigDecimal amount) {
        BigDecimal balanceFrom = accountFrom.getBalance();
        return balanceFrom.compareTo(amount) >= 0;
    }

    private void performTransfer(Account accountFrom, Account accountTo, BigDecimal amount) {

        BigDecimal newBalanceFrom = accountFrom.getBalance().subtract(amount);
        BigDecimal newBalanceTo = accountTo.getBalance().add(amount);

        accountFrom.setBalance(newBalanceFrom);
        accountTo.setBalance(newBalanceTo);

        String messageFrom = "Transferred $" + amount + " to account " + accountTo.getAccountId();
        String messageTo = "Received $" + amount + " from account " + accountFrom.getAccountId();

        notificationService.notifyAboutTransfer(accountFrom, messageFrom);
        notificationService.notifyAboutTransfer(accountTo, messageTo);

        accountsRepository.save(accountFrom);
        accountsRepository.save(accountTo);
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
