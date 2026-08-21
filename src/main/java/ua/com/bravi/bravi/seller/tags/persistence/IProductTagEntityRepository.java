package ua.com.bravi.bravi.seller.tags.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.bravi.bravi.seller.tags.persistence.entity.ProductTagEntity;

import java.util.Collection;
import java.util.List;

public interface IProductTagEntityRepository extends JpaRepository<ProductTagEntity, Long> {

    List<ProductTagEntity> findByProductIdIn(Collection<Long> productIds);

    void deleteByProductIdAndTagIdIn(Long productId, Collection<Long> tagIds);

    @Query("select l.tagId as tagId, count(l.productId) as usages from ProductTagEntity l "
            + "where l.tagId in :tagIds group by l.tagId")
    List<TagUsageProjection> countUsagesByTagIds(@Param("tagIds") Collection<Long> tagIds);

    /** Moves assignments, skipping owners that already carry the target, whose pair is unique. */
    @Modifying(clearAutomatically = true)
    @Query("update ProductTagEntity l set l.tagId = :targetId where l.tagId in :sourceIds "
            + "and not exists (select 1 from ProductTagEntity o "
            + "where o.productId = l.productId and o.tagId = :targetId)")
    int repointToTag(@Param("sourceIds") Collection<Long> sourceIds, @Param("targetId") Long targetId);

    @Modifying(clearAutomatically = true)
    @Query("delete from ProductTagEntity l where l.tagId in :tagIds")
    int deleteByTagIdIn(@Param("tagIds") Collection<Long> tagIds);
}
