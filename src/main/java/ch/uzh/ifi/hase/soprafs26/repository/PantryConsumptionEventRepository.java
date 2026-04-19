package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.PantryConsumptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PantryConsumptionEventRepository extends JpaRepository<PantryConsumptionEvent, Long> {
  List<PantryConsumptionEvent> findByConsumedAtGreaterThanEqual(Instant from);
}
