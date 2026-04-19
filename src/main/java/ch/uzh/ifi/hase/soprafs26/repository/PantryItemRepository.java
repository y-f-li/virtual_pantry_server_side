package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("pantryItemRepository")
public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {
  java.util.List<PantryItem> findAllByCountGreaterThan(int minCount);
}