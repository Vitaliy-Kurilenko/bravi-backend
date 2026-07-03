package ua.com.bravi.bravi.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.IdentityApi;
import ua.com.bravi.bravi.identity.api.event.UserProvisionedEvent;
import ua.com.bravi.bravi.identity.domain.User;
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository;
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository.UserContextProjection;
import ua.com.bravi.bravi.identity.persistence.mapper.UserEntityMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IdentityApi {

    private final IUserEntityRepository userRepository;
    private final UserEntityMapper userEntityMapper;
    private final InvocationContext invocationContext;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CurrentUserView resolveCurrentUser() {
        UUID extId = invocationContext.getUserExtId();
        if (extId == null) {
            return null;
        }

        Optional<UserContextProjection> existing = userRepository.findContextByExtId(extId);
        if (existing.isPresent()) {
            applyToContext(existing.get());
            return toView(existing.get());
        }

        User created = createUser(extId);
        applyToContext(created);
        return toView(created);
    }

    @Override
    public Optional<CurrentUserView> findByExtId(UUID extId) {
        return userRepository.findByExtId(extId)
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

    public CurrentUserView getCurrentUserContext() {
        return new CurrentUserView(
                invocationContext.getUserId(),
                invocationContext.getUserExtId(),
                invocationContext.getUserStatus(),
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail()
        );
    }

    private User createUser(UUID extId) {
        User toCreate = User.provisionNew(
                extId,
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail()
        );

        try {
            User saved = userEntityMapper.toDomain(
                    userRepository.save(userEntityMapper.toEntity(toCreate)));
            eventPublisher.publishEvent(new UserProvisionedEvent(
                    saved.id(), saved.extId(), Instant.now()));
            return saved;
        } catch (DataIntegrityViolationException concurrentInsert) {
            return userRepository.findByExtId(extId)
                    .map(userEntityMapper::toDomain)
                    .orElseThrow(() -> concurrentInsert);
        }
    }

    private void applyToContext(UserContextProjection projection) {
        invocationContext.setUserId(projection.getUserId());
        invocationContext.setUserStatus(projection.getUserStatus() == null ? null : projection.getUserStatus().name());
        invocationContext.setFirstName(projection.getFirstName());
        invocationContext.setLastName(projection.getLastName());
        invocationContext.setEmail(projection.getEmail());
    }

    private void applyToContext(User user) {
        invocationContext.setUserId(user.id());
        invocationContext.setUserStatus(user.status() == null ? null : user.status().name());
        invocationContext.setFirstName(user.firstName());
        invocationContext.setLastName(user.lastName());
        invocationContext.setEmail(user.email());
    }

    private CurrentUserView toView(UserContextProjection projection) {
        return new CurrentUserView(
                projection.getUserId(),
                projection.getUserExtId(),
                projection.getUserStatus() == null ? null : projection.getUserStatus().name(),
                projection.getFirstName(),
                projection.getLastName(),
                projection.getEmail()
        );
    }

    private CurrentUserView toView(User user) {
        return new CurrentUserView(
                user.id(),
                user.extId(),
                user.status() == null ? null : user.status().name(),
                user.firstName(),
                user.lastName(),
                user.email()
        );
    }
}
