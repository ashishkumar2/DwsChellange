package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    private TransferService transferService;

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transferService = new TransferService(accountsRepository);
        transferService.setNotificationService(notificationService);
    }

    @Test
    void testTransferMoney_SuccessfulTransfer() {
        // Arrange
        Account accountFrom = new Account("123", BigDecimal.valueOf(100.00));
        Account accountTo = new Account("456", BigDecimal.valueOf(50.00));
        BigDecimal amount = BigDecimal.valueOf(30.00);

        when(accountsRepository.getAccount("123")).thenReturn(accountFrom);
        when(accountsRepository.getAccount("456")).thenReturn(accountTo);

        // Act
        boolean result = transferService.transferMoney("123", "456", amount);

        // Assert
        assertTrue(result);
        assertEquals(BigDecimal.valueOf(70.00), accountFrom.getBalance());
        assertEquals(BigDecimal.valueOf(80.00), accountTo.getBalance());
    }

    @Test
    void testTransferMoney_InvalidTransferAmount() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO; // Invalid amount

        // Act
        boolean result = transferService.transferMoney("123", "456", amount);

        // Assert
        assertFalse(result);
        verify(accountsRepository, never()).getAccount(any());
        verify(notificationService, never()).notifyAboutTransfer(any(), anyString());
        verify(accountsRepository, never()).save(any());
    }

    //This test case simulates the scenario you described and
    // checks that no deadlocks occur when two opposite transfers are executed in parallel.
    @Test
    void testTransferMoneyAndTestOppositeTransferCheck() throws InterruptedException {
        String accountA = "A";
        String accountB = "B";
        BigDecimal amount = BigDecimal.valueOf(100);

        // Mock accountsRepository
        AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
        Account account1 = new Account(accountA, BigDecimal.valueOf(200));
        Account account2 = new Account(accountB, BigDecimal.valueOf(200));

        Mockito.when(accountsRepository.getAccount(accountA)).thenReturn(account1);
        Mockito.when(accountsRepository.getAccount(accountB)).thenReturn(account2);

        TransferService transferService = new TransferService(accountsRepository);

        // Simulate two opposite transfers in parallel
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(() -> transferService.transferMoney(accountA, accountB, amount));
        executorService.submit(() -> transferService.transferMoney(accountB, accountA, amount));

        executorService.shutdown();

        // Wait for both threads to finish
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        // Check that balances are as expected
        assertEquals(BigDecimal.valueOf(200), account1.getBalance());
        assertEquals(BigDecimal.valueOf(200), account2.getBalance());
    }

    // Add more test cases for different scenarios (e.g., insufficient balance, non-existing accounts, etc.)
}
