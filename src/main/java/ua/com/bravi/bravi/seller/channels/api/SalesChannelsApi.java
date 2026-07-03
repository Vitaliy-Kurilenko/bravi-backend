package ua.com.bravi.bravi.seller.channels.api;

public interface SalesChannelsApi {

    /** Creates the default MANUAL sales channel for a store (idempotent — no-op if it already exists). */
    void createManualChannel(Long storeId);

    boolean hasManualChannel(Long storeId);
}
