package uk.gubin.budgetpilot.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import uk.gubin.budgetpilot.entity.Transaction;

@Getter
public class TransactionEvent extends ApplicationEvent {

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    private final Transaction transaction;
    private final Action action;
    private final Long userId;

    public TransactionEvent(Object source, Transaction transaction, Action action) {
        super(source);
        this.transaction = transaction;
        this.action = action;
        this.userId = transaction.getUserId();
    }
}
