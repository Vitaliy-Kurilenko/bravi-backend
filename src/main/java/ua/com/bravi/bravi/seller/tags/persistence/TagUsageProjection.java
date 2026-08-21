package ua.com.bravi.bravi.seller.tags.persistence;

/** How many owners carry one tag. */
public interface TagUsageProjection {

    Long getTagId();

    long getUsages();
}
