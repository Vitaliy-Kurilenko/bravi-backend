package ua.com.bravi.bravi.seller.tags.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.persistence.entity.ProductTagEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductTagLinkStore implements TagLinkStore {

    private final IProductTagEntityRepository linkRepository;

    @Override
    public TagTarget target() {
        return TagTarget.PRODUCT;
    }

    @Override
    public List<TagLink> findByOwnerIds(Collection<Long> ownerIds) {
        if (ownerIds.isEmpty()) {
            return List.of();
        }
        return linkRepository.findByProductIdIn(ownerIds).stream()
                .map(link -> new TagLink(link.getId(), link.getProductId(), link.getTagId()))
                .toList();
    }

    @Override
    public void link(Long ownerId, Collection<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        List<ProductTagEntity> rows = tagIds.stream().map(tagId -> {
            ProductTagEntity link = new ProductTagEntity();
            link.setProductId(ownerId);
            link.setTagId(tagId);
            return link;
        }).toList();
        linkRepository.saveAll(rows);
    }

    @Override
    public void unlink(Long ownerId, Collection<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        linkRepository.deleteByProductIdAndTagIdIn(ownerId, tagIds);
    }

    @Override
    public Map<Long, Long> countByTagIds(Collection<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        return linkRepository.countUsagesByTagIds(tagIds).stream()
                .collect(Collectors.toMap(TagUsageProjection::getTagId, TagUsageProjection::getUsages));
    }

    @Override
    public int repointToTag(Collection<Long> sourceIds, Long targetId) {
        return sourceIds.isEmpty() ? 0 : linkRepository.repointToTag(sourceIds, targetId);
    }

    @Override
    public int deleteByTagIds(Collection<Long> tagIds) {
        return tagIds.isEmpty() ? 0 : linkRepository.deleteByTagIdIn(tagIds);
    }

    @Override
    public Class<?> linkEntity() {
        return ProductTagEntity.class;
    }

    @Override
    public String ownerProperty() {
        return "productId";
    }
}
