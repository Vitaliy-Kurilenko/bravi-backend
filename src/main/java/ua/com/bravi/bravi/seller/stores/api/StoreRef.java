package ua.com.bravi.bravi.seller.stores.api;

/** Lightweight store identity for context resolution: internal store id + its owning seller account id. */
public record StoreRef(Long storeId, Long sellerAccountId) {
}
