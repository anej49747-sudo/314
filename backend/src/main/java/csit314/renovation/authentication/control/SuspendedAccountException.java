package csit314.renovation.authentication.control;

import org.springframework.security.core.AuthenticationException;

/** Presented only when the supplied password matches a suspended account. */
public class SuspendedAccountException extends AuthenticationException {
    public SuspendedAccountException() { super("Account suspended"); }
}
