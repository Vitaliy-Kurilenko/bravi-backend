package ua.com.bravi.bravi.seller.stores;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.stores.api.LogoUpload;
import ua.com.bravi.bravi.seller.stores.api.StoreDraft;
import ua.com.bravi.bravi.seller.stores.api.StoreSettings;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.event.StoreCreatedEvent;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreEntityRepository;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreSettingsRepository;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;
import ua.com.bravi.bravi.seller.stores.persistence.mapper.StoreEntityMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.shared.media.MediaCategory;
import ua.com.bravi.bravi.shared.media.MediaStorage;
import ua.com.bravi.bravi.shared.media.MediaUploadRequest;
import ua.com.bravi.bravi.shared.media.PresignedUpload;
import ua.com.bravi.bravi.shared.media.StoredObject;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;
import ua.com.bravi.bravi.shared.media.exception.MediaObjectNotFoundException;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoreService implements StoresApi {

    // Onboarding defaults (see spec §5.2).
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("EUR");
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Lisbon");
    private static final String DEFAULT_WEIGHT_UNIT = "KG";
    private static final String DEFAULT_DIMENSION_UNIT = "CM";
    private static final String DEFAULT_LANGUAGE = "en";

    private final IStoreEntityRepository storeRepository;
    private final IStoreSettingsRepository storeSettingsRepository;
    private final StoreEntityMapper storeEntityMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaStorage mediaStorage;

    @Override
    public Optional<Long> findFirstStoreIdByAccountId(Long sellerAccountId) {
        return storeRepository.findFirstBySellerAccountIdOrderByIdAsc(sellerAccountId).map(StoreEntity::getId);
    }

    @Override
    public Optional<StoreView> getStoreById(Long storeId) {
        return storeRepository.findById(storeId).map(storeEntityMapper::toView);
    }

    @Override
    @Transactional
    public StoreView createDraftStore(Long sellerAccountId, StoreDraft draft) {
        StoreEntity entity = new StoreEntity();
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.STORE_PREFIX));
        entity.setSellerAccountId(sellerAccountId);
        entity.setName(draft.name());
        entity.setDescription(draft.description());
        entity.setCountry(draft.country());
        entity.setCurrency(DEFAULT_CURRENCY);
        entity.setTimezone(DEFAULT_TIMEZONE);
        entity.setAllowReturn(false);
        entity.setStatus(StoreStatus.DRAFT);

        StoreEntity saved = storeRepository.save(entity);
        createDefaultSettings(saved.getId());
        eventPublisher.publishEvent(new StoreCreatedEvent(saved.getId(), sellerAccountId, Instant.now()));
        return storeEntityMapper.toView(saved);
    }

    @Override
    @Transactional
    public void updateDraftStore(Long storeId, StoreDraft draft) {
        StoreEntity entity = requireStore(storeId);
        if (draft.name() != null) {
            entity.setName(draft.name());
        }
        entity.setDescription(draft.description());
        if (draft.country() != null) {
            entity.setCountry(draft.country());
        }
    }

    @Override
    @Transactional
    public void updateStore(Long storeId, Store patch) {
        storeEntityMapper.updateEntity(requireStore(storeId), patch);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreSettings getSettings(Long storeId) {
        return storeSettingsRepository.findById(storeId)
                .map(s -> new StoreSettings(
                        s.getDefaultCurrency(), s.getDefaultLanguage(),
                        s.getDefaultWeightUnit(), s.getDefaultDimensionUnit(), s.getTimezone()))
                .orElseThrow(() -> new NotFoundException("Store settings not found"));
    }

    @Override
    @Transactional
    public void updateSettings(Long storeId, StoreSettings patch) {
        StoreSettingsEntity settings = storeSettingsRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store settings not found"));
        if (patch.defaultCurrency() != null) {
            settings.setDefaultCurrency(patch.defaultCurrency());
        }
        if (patch.defaultLanguage() != null) {
            settings.setDefaultLanguage(patch.defaultLanguage());
        }
        if (patch.defaultWeightUnit() != null) {
            settings.setDefaultWeightUnit(patch.defaultWeightUnit());
        }
        if (patch.defaultDimensionUnit() != null) {
            settings.setDefaultDimensionUnit(patch.defaultDimensionUnit());
        }
        if (patch.timezone() != null) {
            settings.setTimezone(patch.timezone());
        }
    }

    @Override
    @Transactional
    public void activateStore(Long storeId) {
        requireStore(storeId).setStatus(StoreStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUpload presignLogoUpload(Long storeId, LogoUpload upload) {
        requireStore(storeId);
        MediaCategory.STORE_LOGO.validate(upload.contentType(), upload.size());
        return mediaStorage.presignUpload(new MediaUploadRequest(
                MediaCategory.STORE_LOGO, logoScope(storeId), upload.contentType(), upload.size(), upload.originalFilename()));
    }

    @Override
    @Transactional
    public StoreView confirmLogo(Long storeId, String storageKey) {
        StoreEntity entity = requireStore(storeId);
        requireOwnedKey(storeId, storageKey);
        StoredObject object = mediaStorage.stat(storageKey)
                .orElseThrow(() -> new MediaObjectNotFoundException("Logo upload not found or expired; upload again"));
        MediaCategory.STORE_LOGO.validate(object.contentType(), object.size());

        String previousKey = entity.getLogoKey();
        entity.setLogoKey(storageKey);
        entity.setLogoUrl(mediaStorage.publicUrl(storageKey));
        if (previousKey != null && !previousKey.equals(storageKey)) {
            mediaStorage.delete(previousKey);
        }
        return storeEntityMapper.toView(entity);
    }

    @Override
    @Transactional
    public StoreView removeLogo(Long storeId) {
        StoreEntity entity = requireStore(storeId);
        String key = entity.getLogoKey();
        entity.setLogoKey(null);
        entity.setLogoUrl(null);
        if (key != null) {
            mediaStorage.delete(key);
        }
        return storeEntityMapper.toView(entity);
    }

    private StoreEntity requireStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store not found"));
    }

    private static String logoScope(Long storeId) {
        return String.valueOf(storeId);
    }

    /** Guards that a confirmed key was minted for this store (presign always keys under its prefix). */
    private static void requireOwnedKey(Long storeId, String storageKey) {
        String expectedPrefix = MediaCategory.STORE_LOGO.keyPrefix(logoScope(storeId)) + "/";
        if (storageKey == null || !storageKey.startsWith(expectedPrefix)) {
            throw new InvalidMediaUploadException("storage_key", "Storage key does not belong to this store");
        }
    }

    private void createDefaultSettings(Long storeId) {
        StoreSettingsEntity settings = new StoreSettingsEntity();
        settings.setStoreId(storeId);
        settings.setDefaultWeightUnit(DEFAULT_WEIGHT_UNIT);
        settings.setDefaultDimensionUnit(DEFAULT_DIMENSION_UNIT);
        settings.setDefaultCurrency(DEFAULT_CURRENCY.getCurrencyCode());
        settings.setDefaultLanguage(DEFAULT_LANGUAGE);
        settings.setTimezone(DEFAULT_TIMEZONE.getId());
        storeSettingsRepository.save(settings);
    }
}
