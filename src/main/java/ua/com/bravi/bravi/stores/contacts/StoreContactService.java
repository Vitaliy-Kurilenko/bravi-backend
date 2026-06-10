package ua.com.bravi.bravi.stores.contacts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.stores.contacts.api.StoreContactView;
import ua.com.bravi.bravi.stores.contacts.api.StoreContactsApi;
import ua.com.bravi.bravi.stores.contacts.domain.StoreContact;
import ua.com.bravi.bravi.stores.contacts.domain.StoreContactPolicy;
import ua.com.bravi.bravi.stores.contacts.persistence.IStoreContactEntityRepository;
import ua.com.bravi.bravi.stores.contacts.persistence.entity.StoreContactEntity;
import ua.com.bravi.bravi.stores.contacts.persistence.mapper.StoreContactEntityMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreContactService implements StoreContactsApi {

    private final IStoreContactEntityRepository contactRepository;
    private final StoreContactEntityMapper contactEntityMapper;

    @Override
    public List<StoreContactView> findByStoreId(Long storeId) {
        return contactEntityMapper.toViews(contactRepository.findByStoreId(storeId));
    }

    @Override
    @Transactional
    public void addContacts(Long storeId, List<StoreContact> contacts) {
        contacts.forEach(c -> StoreContactPolicy.validate(c.type(), c.value()));

        List<StoreContactEntity> entities = contacts.stream()
                .map(contact -> {
                    StoreContactEntity entity = contactEntityMapper.toEntity(contact);
                    entity.setStoreId(storeId);
                    return entity;
                })
                .toList();

        contactRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public void updateContact(Long storeId, Long contactId, StoreContact patch) {
        StoreContactEntity entity = contactRepository.findById(contactId)
                .orElseThrow(() -> new NotFoundException("Contact not found"));

        entity.requireOwnedBy(storeId);

        contactEntityMapper.updateEntity(entity, patch);

        StoreContactPolicy.validate(entity.getType(), entity.getValue());
    }

    @Override
    @Transactional
    public void deleteContact(Long storeId, Long contactId) {
        StoreContactEntity entity = contactRepository.findById(contactId)
                .orElseThrow(() -> new NotFoundException("Contact not found"));

        entity.requireOwnedBy(storeId);

        contactRepository.delete(entity);
    }
}
