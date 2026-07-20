package ua.com.bravi.bravi.seller.stores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ua.com.bravi.bravi.dictionaries.api.DictionariesApi;
import ua.com.bravi.bravi.seller.stores.api.LogoUpload;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.api.StoreDraft;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.event.StoreCreatedEvent;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreEntityRepository;
import ua.com.bravi.bravi.seller.stores.persistence.IStoreSettingsRepository;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;
import ua.com.bravi.bravi.seller.stores.persistence.mapper.StoreEntityMapper;
import ua.com.bravi.bravi.shared.media.MediaCategory;
import ua.com.bravi.bravi.shared.media.MediaStorage;
import ua.com.bravi.bravi.shared.media.MediaUploadRequest;
import ua.com.bravi.bravi.shared.media.PresignedUpload;
import ua.com.bravi.bravi.shared.media.StoredObject;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;
import ua.com.bravi.bravi.shared.media.exception.MediaObjectNotFoundException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    private static final Long ACCOUNT_ID = 42L;
    private static final Long STORE_ID = 7L;

    private final IStoreEntityRepository storeRepository = mock(IStoreEntityRepository.class);
    private final IStoreSettingsRepository storeSettingsRepository = mock(IStoreSettingsRepository.class);
    private final StoreEntityMapper storeEntityMapper = mock(StoreEntityMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final MediaStorage mediaStorage = mock(MediaStorage.class);
    private final DictionariesApi dictionariesApi = mock(DictionariesApi.class);

    private StoreService service;

    @BeforeEach
    void setUp() {
        service = new StoreService(storeRepository, storeSettingsRepository, storeEntityMapper,
                eventPublisher, mediaStorage, dictionariesApi);
    }

    /** Views join the store row with its settings row, so both must be stubbed. */
    private StoreView stubView(StoreEntity entity) {
        StoreSettingsEntity settings = new StoreSettingsEntity();
        StoreView view = mock(StoreView.class);
        when(storeSettingsRepository.findById(STORE_ID)).thenReturn(Optional.of(settings));
        when(storeEntityMapper.toView(entity, settings)).thenReturn(view);
        return view;
    }

    private static Store newStore() {
        return new Store(
                null, null, "Shop", null, null, null, null,
                null, null, null,
                ZoneId.of("UTC"), null, null,
                Currency.getInstance("UAH"), true,
                null, null, null
        );
    }

    @Test
    void findFirstStoreIdByAccountIdReturnsId() {
        StoreEntity entity = new StoreEntity();
        entity.setId(STORE_ID);
        when(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(ACCOUNT_ID)).thenReturn(Optional.of(entity));

        assertThat(service.findFirstStoreIdByAccountId(ACCOUNT_ID)).contains(STORE_ID);
    }

    @Test
    void findFirstStoreIdByAccountIdReturnsEmptyWhenAbsent() {
        when(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThat(service.findFirstStoreIdByAccountId(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void createDraftStorePersistsDraftWithDefaultsSettingsAndPublishesEvent() {
        StoreDraft draft = new StoreDraft("Shop", "desc", "UA");
        StoreEntity saved = new StoreEntity();
        saved.setId(STORE_ID);
        saved.setSellerAccountId(ACCOUNT_ID);
        StoreSettingsEntity savedSettings = new StoreSettingsEntity();
        StoreView view = mock(StoreView.class);

        when(storeRepository.save(any(StoreEntity.class))).thenReturn(saved);
        when(storeSettingsRepository.save(any(StoreSettingsEntity.class))).thenReturn(savedSettings);
        when(storeEntityMapper.toView(saved, savedSettings)).thenReturn(view);

        StoreView result = service.createDraftStore(ACCOUNT_ID, draft);

        assertThat(result).isSameAs(view);

        ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
        verify(storeRepository).save(captor.capture());
        StoreEntity persisted = captor.getValue();
        assertThat(persisted.getSellerAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(persisted.getName()).isEqualTo("Shop");
        assertThat(persisted.getCountry()).isEqualTo("UA");
        assertThat(persisted.getPublicId()).isNotBlank();
        assertThat(persisted.getStatus()).isEqualTo(StoreStatus.DRAFT);

        // Currency/timezone defaults now land on the settings row, not the store row.
        ArgumentCaptor<StoreSettingsEntity> settingsCaptor = ArgumentCaptor.forClass(StoreSettingsEntity.class);
        verify(storeSettingsRepository).save(settingsCaptor.capture());
        StoreSettingsEntity persistedSettings = settingsCaptor.getValue();
        assertThat(persistedSettings.getDefaultCurrency()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(persistedSettings.getTimezone()).isEqualTo(ZoneId.of("Europe/Lisbon"));
        assertThat(persistedSettings.getDefaultLanguage()).isEqualTo(Locale.ENGLISH);
        assertThat(persistedSettings.getDefaultWeightUnit()).isEqualTo("KG");
        assertThat(persistedSettings.getDefaultDimensionUnit()).isEqualTo("CM");

        verify(eventPublisher).publishEvent(any(StoreCreatedEvent.class));
    }

    @Test
    void updateStoreInvokesMapperPatch() {
        Store patch = new Store(
                null, null, "New name", null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null
        );
        StoreEntity entity = new StoreEntity();
        entity.setName("Old");
        entity.setStatus(StoreStatus.ACTIVE);
        StoreSettingsEntity settings = new StoreSettingsEntity();
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));
        when(storeSettingsRepository.findById(STORE_ID)).thenReturn(Optional.of(settings));

        service.updateStore(STORE_ID, patch);

        verify(storeEntityMapper).updateEntity(entity, patch);
        verify(storeEntityMapper).updateSettings(settings, patch);
    }

    @Test
    void updateStoreThrowsNotFoundWhenStoreMissing() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStore(STORE_ID, newStore()))
                .isInstanceOf(NotFoundException.class);

        verify(storeEntityMapper, never()).updateEntity(any(), any());
    }

    @Test
    void presignLogoUploadValidatesAndKeysUnderStorePrefix() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(new StoreEntity()));
        PresignedUpload presigned = new PresignedUpload("http://put", "store-logos/7/a.png", Map.of(), Instant.now());
        when(mediaStorage.presignUpload(any())).thenReturn(presigned);

        PresignedUpload result = service.presignLogoUpload(STORE_ID, new LogoUpload("image/png", 1024, "logo.png"));

        assertThat(result).isSameAs(presigned);
        ArgumentCaptor<MediaUploadRequest> captor = ArgumentCaptor.forClass(MediaUploadRequest.class);
        verify(mediaStorage).presignUpload(captor.capture());
        assertThat(captor.getValue().category()).isEqualTo(MediaCategory.STORE_LOGO);
        assertThat(captor.getValue().scope()).isEqualTo("7");
    }

    @Test
    void presignLogoUploadSweepsOrphansButKeepsAttachedLogo() {
        StoreEntity entity = new StoreEntity();
        entity.setLogoKey("store-logos/7/attached.png");
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));
        when(mediaStorage.list("store-logos/7/")).thenReturn(List.of(
                "store-logos/7/attached.png",
                "store-logos/7/orphan-a.png",
                "store-logos/7/orphan-b.png"));
        when(mediaStorage.presignUpload(any()))
                .thenReturn(new PresignedUpload("http://put", "store-logos/7/new.png", Map.of(), Instant.now()));

        service.presignLogoUpload(STORE_ID, new LogoUpload("image/png", 1024, "logo.png"));

        verify(mediaStorage).delete("store-logos/7/orphan-a.png");
        verify(mediaStorage).delete("store-logos/7/orphan-b.png");
        verify(mediaStorage, never()).delete("store-logos/7/attached.png");
    }

    @Test
    void presignLogoUploadRejectsUnsupportedType() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(new StoreEntity()));

        assertThatThrownBy(() -> service.presignLogoUpload(STORE_ID, new LogoUpload("application/pdf", 10, "f.pdf")))
                .isInstanceOf(InvalidMediaUploadException.class);

        verify(mediaStorage, never()).presignUpload(any());
    }

    @Test
    void confirmLogoAttachesObjectAndDeletesPrevious() {
        StoreEntity entity = new StoreEntity();
        entity.setLogoKey("store-logos/7/old.png");
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));
        when(mediaStorage.stat("store-logos/7/new.png"))
                .thenReturn(Optional.of(new StoredObject("store-logos/7/new.png", "image/png", 1000)));
        when(mediaStorage.publicUrl("store-logos/7/new.png")).thenReturn("http://pub/new.png");
        stubView(entity);

        service.confirmLogo(STORE_ID, "store-logos/7/new.png");

        assertThat(entity.getLogoKey()).isEqualTo("store-logos/7/new.png");
        assertThat(entity.getLogoUrl()).isEqualTo("http://pub/new.png");
        verify(mediaStorage).delete("store-logos/7/old.png");
    }

    @Test
    void confirmLogoRejectsKeyOfAnotherStore() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(new StoreEntity()));

        assertThatThrownBy(() -> service.confirmLogo(STORE_ID, "store-logos/99/x.png"))
                .isInstanceOf(InvalidMediaUploadException.class);

        verify(mediaStorage, never()).stat(any());
    }

    @Test
    void confirmLogoThrowsWhenObjectMissing() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(new StoreEntity()));
        when(mediaStorage.stat("store-logos/7/x.png")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmLogo(STORE_ID, "store-logos/7/x.png"))
                .isInstanceOf(MediaObjectNotFoundException.class);
    }

    @Test
    void removeLogoClearsColumnsAndDeletesObject() {
        StoreEntity entity = new StoreEntity();
        entity.setLogoKey("store-logos/7/x.png");
        entity.setLogoUrl("http://pub/x.png");
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(entity));
        stubView(entity);

        service.removeLogo(STORE_ID);

        assertThat(entity.getLogoKey()).isNull();
        assertThat(entity.getLogoUrl()).isNull();
        verify(mediaStorage).delete("store-logos/7/x.png");
    }
}
