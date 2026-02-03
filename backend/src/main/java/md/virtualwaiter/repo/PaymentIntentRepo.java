package md.virtualwaiter.repo;

import java.util.Optional;
import md.virtualwaiter.domain.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIntentRepo extends JpaRepository<PaymentIntent, Long> {
  Optional<PaymentIntent> findByProviderAndProviderRef(String provider, String providerRef);
}
