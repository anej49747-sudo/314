package csit314.renovation.account.control;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() { super("Account not found"); }
}
