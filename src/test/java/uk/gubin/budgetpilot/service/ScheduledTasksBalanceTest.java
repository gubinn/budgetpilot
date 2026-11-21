package uk.gubin.budgetpilot.service;

import org.junit.jupiter.api.Test;
import uk.gubin.budgetpilot.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduledTasksBalanceTest {

    private static final Long ACCOUNT_ID = 10L;

    @Test
    void noTransactionsKeepsInitialBalance() {
        assertEquals(new BigDecimal("100.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, List.of()));
    }

    @Test
    void incomeAndExpense() {
        List<Transaction> txs = List.of(
                tx(2, "50.00", ACCOUNT_ID, null),
                tx(1, "30.00", ACCOUNT_ID, null)
        );
        assertEquals(new BigDecimal("120.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, txs));
    }

    @Test
    void outgoingTransferReducesBalance() {
        List<Transaction> txs = List.of(tx(3, "40.00", ACCOUNT_ID, 20L));
        assertEquals(new BigDecimal("60.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, txs));
    }

    @Test
    void incomingTransferIncreasesBalance() {
        List<Transaction> txs = List.of(tx(3, "40.00", 20L, ACCOUNT_ID));
        assertEquals(new BigDecimal("140.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, txs));
    }

    @Test
    void refundIncreasesBalance() {
        List<Transaction> txs = List.of(tx(4, "20.00", ACCOUNT_ID, null));
        assertEquals(new BigDecimal("120.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, txs));
    }

    @Test
    void mixedTransactionsUseAmountNotAmountBase() {
        Transaction income = tx(2, "50.00", ACCOUNT_ID, null);
        income.setAmountBase(new BigDecimal("999.00"));
        Transaction incoming = tx(3, "25.00", 20L, ACCOUNT_ID);
        incoming.setAmountBase(new BigDecimal("888.00"));
        List<Transaction> txs = List.of(
                income,
                tx(1, "30.00", ACCOUNT_ID, null),
                tx(3, "40.00", ACCOUNT_ID, 20L),
                tx(4, "20.00", ACCOUNT_ID, null),
                incoming
        );
        assertEquals(new BigDecimal("125.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, txs));
    }

    @Test
    void selfTransferIsNotDoubleCounted() {
        List<Transaction> txs = List.of(tx(3, "10.00", ACCOUNT_ID, ACCOUNT_ID));
        assertEquals(new BigDecimal("90.00"),
                ScheduledTasks.calculateCalibratedBalance(new BigDecimal("100.00"), ACCOUNT_ID, txs));
    }

    private static Transaction tx(int type, String amount, Long accountId, Long targetAccountId) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setAmountBase(new BigDecimal(amount));
        tx.setAccountId(accountId);
        tx.setTargetAccountId(targetAccountId);
        return tx;
    }
}