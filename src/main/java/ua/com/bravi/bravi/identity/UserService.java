package ua.com.bravi.bravi.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.IdentityApi;
import ua.com.bravi.bravi.identity.api.event.UserProvisionedEvent;
import ua.com.bravi.bravi.identity.domain.User;
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository;
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository.UserContextProjection;
import ua.com.bravi.bravi.identity.persistence.entity.UserEntity;
import ua.com.bravi.bravi.identity.persistence.mapper.UserEntityMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IdentityApi {

    private final IUserEntityRepository userRepository;
    private final UserEntityMapper userEntityMapper;
    private final InvocationContext invocationContext;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Lookup-only: if the JWT subject maps to a known user, populate the InvocationContext and return it.
     * Users are created explicitly via {@link #provisionUser} (registration), never implicitly here.
     */
    @Override
    @Transactional(readOnly = true)
    public CurrentUserView resolveCurrentUser() {
        UUID extId = invocationContext.getUserExtId();
        if (extId == null) {
            return null;
        }
        return userRepository.findContextByExtId(extId)
                .map(projection -> {
                    applyToContext(projection);
                    return toView(projection);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public CurrentUserView provisionUser(UUID keycloakUserId, String email, String firstName, String lastName) {
        return userRepository.findByExtId(keycloakUserId)
                .map(userEntityMapper::toDomain)
                .map(this::toView)
                .orElseGet(() -> createUser(keycloakUserId, email, firstName, lastName));
    }

    @Override
    public Optional<CurrentUserView> findByExtId(UUID extId) {
        return userRepository.findByExtId(extId)
                .map(userEntityMapper::toDomain)
                .map(this::toView);
    }

    @Override
    public Optional<CurrentUserView> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userEntityMapper::toDomain)
                .map(this::toView);
    }

    @Override
    public CurrentUserView getById(Long id) {
        return userRepository.findById(id)
                .map(userEntityMapper::toDomain)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    @Transactional
    public CurrentUserView syncEmailVerified(Long userId, boolean tokenEmailVerified) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (tokenEmailVerified && !entity.isEmailVerified()) {
            entity.setEmailVerified(true);
            userRepository.save(entity);
            log.info("User email verified userId={}", userId);
        }
        return toView(userEntityMapper.toDomain(entity));
    }

    public CurrentUserView getCurrentUserContext() {
        return new CurrentUserView(
                invocationContext.getUserId(),
                invocationContext.getUserPublicId(),
                invocationContext.getUserExtId(),
                invocationContext.getUserStatus(),
                invocationContext.isEmailVerified(),
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail()
        );
    }

    private CurrentUserView createUser(UUID extId, String email, String firstName, String lastName) {
        User toCreate = User.register(PublicIdGenerator.userId(), extId, firstName, lastName, email);
        try {
            User saved = userEntityMapper.toDomain(
                    userRepository.save(userEntityMapper.toEntity(toCreate)));
            eventPublisher.publishEvent(new UserProvisionedEvent(
                    saved.id(), saved.extId(), Instant.now()));
            // Identifiers only: no email or name reaches the logs.
            log.info("User provisioned userId={} publicId={} extId={}",
                    saved.id(), saved.publicId(), saved.extId());
            return toView(saved);
        } catch (DataIntegrityViolationException concurrentInsert) {
            log.debug("Concurrent user provisioning detected extId={}, falling back to lookup", extId);
            return userRepository.findByExtId(extId)
                    .map(userEntityMapper::toDomain)
                    .map(this::toView)
                    .orElseThrow(() -> concurrentInsert);
        }
    }

    private void applyToContext(UserContextProjection projection) {
        invocationContext.setUserId(projection.getUserId());
        invocationContext.setUserPublicId(projection.getUserPublicId());
        invocationContext.setUserStatus(projection.getUserStatus() == null ? null : projection.getUserStatus().name());
        invocationContext.setEmailVerified(projection.isEmailVerified());
        invocationContext.setFirstName(projection.getFirstName());
        invocationContext.setLastName(projection.getLastName());
        invocationContext.setEmail(projection.getEmail());
    }

    private CurrentUserView toView(UserContextProjection projection) {
        return new CurrentUserView(
                projection.getUserId(),
                projection.getUserPublicId(),
                projection.getUserExtId(),
                projection.getUserStatus() == null ? null : projection.getUserStatus().name(),
                projection.isEmailVerified(),
                projection.getFirstName(),
                projection.getLastName(),
                projection.getEmail()
        );
    }

    private CurrentUserView toView(User user) {
        return new CurrentUserView(
                user.id(),
                user.publicId(),
                user.extId(),
                user.status() == null ? null : user.status().name(),
                user.emailVerified(),
                user.firstName(),
                user.lastName(),
                user.email()
        );
    }
}
