package md.virtualwaiter.repo;

import md.virtualwaiter.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CurrencyRepo extends JpaRepository<Currency, String> {
  List<Currency> findByIsActiveTrueOrderByCodeAsc();
  List<Currency> findAllByOrderByCodeAsc();
}
