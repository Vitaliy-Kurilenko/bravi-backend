package ua.com.bravi.bravi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.component.InvocationContext;
import ua.com.bravi.bravi.domain.user.User;
import ua.com.bravi.bravi.exception.UserProvisioningException;
import ua.com.bravi.bravi.persistance.IUserEntityRepository;
import ua.com.bravi.bravi.persistance.mapper.UserEntityMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserEntityRepository userRepository;
    private final UserEntityMapper userEntityMapper;
    private final InvocationContext invocationContext;

    @Transactional
    public void resolveCurrentUser() {
        UUID extId = invocationContext.getUserExtId();
        if (extId == null) {
            return;
        }

        User user = userRepository.findByExtId(extId)
                .map(userEntityMapper::toDomain)
                .orElseGet(() -> createUser(extId));

        applyToContext(user);
    }


    public User getUserContext() {
        return new User(
                invocationContext.getUserId(),
                invocationContext.getUserExtId(),
                invocationContext.getUserType(),
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail(),
                invocationContext.getUserStatus()
        );
    }


    private User createUser(UUID extId) {
        if (invocationContext.getUserType() == null) {
            throw new UserProvisioningException(
                    "Cannot provision user: 'user_type' claim is missing or invalid");
        }

        User toCreate = User.provisionNew(
                extId,
                invocationContext.getFirstName(),
                invocationContext.getLastName(),
                invocationContext.getEmail(),
                invocationContext.getUserType()
        );

        try {
            return userEntityMapper.toDomain(
                    userRepository.save(userEntityMapper.toEntity(toCreate)));
        } catch (DataIntegrityViolationException concurrentInsert) {
            return userRepository.findByExtId(extId)
                    .map(userEntityMapper::toDomain)
                    .orElseThrow(() -> concurrentInsert);
        }
    }

    private void applyToContext(User user) {
        invocationContext.setUserId(user.id());
        invocationContext.setUserType(user.type());
        invocationContext.setUserStatus(user.status());
        invocationContext.setFirstName(user.firstName());
        invocationContext.setLastName(user.lastName());
        invocationContext.setEmail(user.email());
    }
}
