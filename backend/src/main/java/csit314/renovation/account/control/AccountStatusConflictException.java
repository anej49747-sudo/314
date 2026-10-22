package csit314.renovation.account.control;

public class AccountStatusConflictException extends RuntimeException {
    public AccountStatusConflictException() { super("Account status already changed"); }
}
