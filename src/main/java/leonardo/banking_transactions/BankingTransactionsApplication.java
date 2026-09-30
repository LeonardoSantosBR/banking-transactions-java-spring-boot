package leonardo.banking_transactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankingTransactionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingTransactionsApplication.class, args);
	}

}
