package md.virtualwaiter.repo;

import md.virtualwaiter.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentTransactionRepo extends JpaRepository<PaymentTransaction, Long> {
  Optional<PaymentTransaction> findTopByIntentIdOrderByIdDesc(Long intentId);
}
