package csit314.renovation.favourite.repository;

import csit314.renovation.favourite.entity.CustomerProjectFavourite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerProjectFavouriteRepository extends JpaRepository<CustomerProjectFavourite, Long> {
    boolean existsByCustomer_IdAndProject_Id(Long customerId, Long projectId);

    // ON CONFLICT makes repeated or concurrent Save requests safe and idempotent.
    @Modifying
    @Query(value = "INSERT INTO customer_project_favourites (customer_id, project_id) "
            + "VALUES (:customerId, :projectId) "
            + "ON CONFLICT (customer_id, project_id) DO NOTHING", nativeQuery = true)
    int saveIfAbsent(@Param("customerId") Long customerId, @Param("projectId") Long projectId);

    @Modifying
    @Query(value = "DELETE FROM customer_project_favourites "
            + "WHERE customer_id = :customerId AND project_id = :projectId", nativeQuery = true)
    int removeIfPresent(@Param("customerId") Long customerId, @Param("projectId") Long projectId);
}
