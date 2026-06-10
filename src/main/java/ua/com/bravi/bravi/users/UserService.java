package ua.com.bravi.bravi.users;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.users.api.CurrentUserView;
import ua.com.bravi.bravi.users.api.UsersApi;
import ua.com.bravi.bravi.users.api.event.UserProvisionedEvent;
import ua.com.bravi.bravi.users.domain.User;
import ua.com.bravi.bravi.users.domain.UserType;
import ua.com.bravi.bravi.users.exception.UserProvisioningException;
import ua.com.bravi.bravi.users.persistence.IUserEntityRepository;
import ua.com.bravi.bravi.users.persistence.IUserEntityRepository.UserContextProjection;
import ua.com.bravi.bravi.users.persistence.mapper.UserEntityMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UsersApi {

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
                invocationContext.getUserType(),
                invocationContext.getUserStatus(),
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail()
        );
    }

    private User createUser(UUID extId) {
        UserType userType = parseUserType(invocationContext.getUserType());
        if (userType == null) {
            throw new UserProvisioningException(
                    "Cannot provision user: 'user_type' claim is missing or invalid");
        }

        User toCreate = User.provisionNew(
                extId,
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail(),
                userType
        );

        try {
            User saved = userEntityMapper.toDomain(
                    userRepository.save(userEntityMapper.toEntity(toCreate)));
            eventPublisher.publishEvent(new UserProvisionedEvent(
                    saved.id(), saved.extId(), saved.type().name(), Instant.now()));
            return saved;
        } catch (DataIntegrityViolationException concurrentInsert) {
            return userRepository.findByExtId(extId)
                    .map(userEntityMapper::toDomain)
                    .orElseThrow(() -> concurrentInsert);
        }
    }

    private void applyToContext(UserContextProjection projection) {
        invocationContext.setUserId(projection.getUserId());
        invocationContext.setUserType(projection.getUserType() == null ? null : projection.getUserType().name());
        invocationContext.setUserStatus(projection.getUserStatus() == null ? null : projection.getUserStatus().name());
        invocationContext.setFirstName(projection.getFirstName());
        invocationContext.setLastName(projection.getLastName());
        invocationContext.setEmail(projection.getEmail());
    }

    private void applyToContext(User user) {
        invocationContext.setUserId(user.id());
        invocationContext.setUserType(user.type() == null ? null : user.type().name());
        invocationContext.setUserStatus(user.status() == null ? null : user.status().name());
        invocationContext.setFirstName(user.firstName());
        invocationContext.setLastName(user.lastName());
        invocationContext.setEmail(user.email());
    }

    private CurrentUserView toView(UserContextProjection projection) {
        return new CurrentUserView(
                projection.getUserId(),
                projection.getUserExtId(),
                projection.getUserType() == null ? null : projection.getUserType().name(),
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
                user.type() == null ? null : user.type().name(),
                user.status() == null ? null : user.status().name(),
                user.firstName(),
                user.lastName(),
                user.email()
        );
    }

    private UserType parseUserType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UserType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
