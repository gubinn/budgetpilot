package uk.gubin.budgetpilot.common;

import uk.gubin.budgetpilot.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户余额计算：交易表的 amount 是账户币种金额，amountBase 是 CNY 金额；
 * 账户余额必须用 amount。收入/退款增加余额，支出/转出减少余额；
 * 作为转账目标账户时增加转入金额。
 */
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    public static BigDecimal balanceAt(BigDecimal initialBalance, Long accountId, List<Transaction> txs) {
        BigDecimal balance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
        if (accountId == null || txs == null) {
            return balance;
        }
        for (Transaction tx : txs) {
            if (tx.getAmount() == null || tx.getType() == null) {
                continue;
            }
            boolean outgoing = accountId.equals(tx.getAccountId());
            if (outgoing) {
                switch (tx.getType()) {
                    case 1 -> balance = balance.subtract(tx.getAmount());
                    case 2 -> balance = balance.add(tx.getAmount());
                    case 3 -> balance = balance.subtract(tx.getAmount());
                    case 4 -> balance = balance.add(tx.getAmount());
                    default -> { }
                }
            } else if (tx.getType() == 3 && accountId.equals(tx.getTargetAccountId())) {
                balance = balance.add(tx.getAmount());
            }
        }
        return balance;
    }
}