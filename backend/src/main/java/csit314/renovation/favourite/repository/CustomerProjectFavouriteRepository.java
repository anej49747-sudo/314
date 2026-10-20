package csit314.renovation.favourite.repository;

import csit314.renovation.account.entity.UserRole;
import csit314.renovation.favourite.entity.CustomerProjectFavourite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerProjectFavouriteRepository extends JpaRepository<CustomerProjectFavourite, Long> {
    boolean existsByCustomer_IdAndProject_Id(Long customerId, Long projectId);

    // Return only the customer's currently public projects, newest save first.
    @Query("SELECT favourite FROM CustomerProjectFavourite favourite "
            + "JOIN FETCH favourite.project project "
            + "JOIN FETCH project.designer designer "
            + "JOIN FETCH project.category "
            + "WHERE favourite.customer.id = :customerId "
            + "AND designer.role = :role AND designer.active = true "
            + "ORDER BY favourite.createdAt DESC, favourite.id DESC")
    List<CustomerProjectFavourite> findVisibleForCustomer(
            @Param("customerId") Long customerId, @Param("role") UserRole role);

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
