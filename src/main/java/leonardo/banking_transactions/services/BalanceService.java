package leonardo.banking_transactions.services;

import leonardo.banking_transactions.entities.AccountsEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BalanceService {

    @Transactional
    public boolean transfer(AccountsEntity payer, AccountsEntity payee, BigDecimal amount) {
        if (payer.getBalance().compareTo(amount) < 0)
            return false;
        payer.setBalance(payer.getBalance().subtract(amount));
        payee.setBalance(payee.getBalance().add(amount));
        return true;
    }
}
