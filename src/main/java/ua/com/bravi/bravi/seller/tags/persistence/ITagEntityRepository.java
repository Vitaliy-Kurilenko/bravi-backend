package ua.com.bravi.bravi.seller.tags.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.persistence.entity.TagEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ITagEntityRepository extends JpaRepository<TagEntity, Long>, JpaSpecificationExecutor<TagEntity> {

    Optional<TagEntity> findByStoreIdAndTargetAndPublicId(Long storeId, TagTarget target, String publicId);

    List<TagEntity> findByStoreIdAndTargetAndPublicIdIn(Long storeId, TagTarget target, Collection<String> publicIds);

    List<TagEntity> findByStoreIdAndTargetOrderByNameAsc(Long storeId, TagTarget target);

    @Query("select t from TagEntity t where t.storeId = :storeId and t.target = :target "
            + "and lower(t.name) in :keys")
    List<TagEntity> findByStoreIdAndTargetAndNameKeyIn(@Param("storeId") Long storeId,
                                                       @Param("target") TagTarget target,
                                                       @Param("keys") Collection<String> keys);

    /**
     * Race-free auto-create. Two owner saves naming the same new tag both reach this statement; the
     * loser inserts nothing instead of raising a violation that would doom the surrounding
     * transaction, and the following re-read hands both of them the winner's row.
     *
     * <p>The conflict target must repeat the expression of
     * {@code uq_store_tags_store_target_name_lower} exactly, or PostgreSQL cannot infer the index.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO store_tags (public_id, store_id, target, name, color, status, created_at)
            VALUES (:publicId, :storeId, :target, :name, :color, :status, :createdAt)
            ON CONFLICT (store_id, target, lower(name)) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("publicId") String publicId,
                       @Param("storeId") Long storeId,
                       @Param("target") String target,
                       @Param("name") String name,
                       @Param("color") String color,
                       @Param("status") String status,
                       @Param("createdAt") Instant createdAt);
}
