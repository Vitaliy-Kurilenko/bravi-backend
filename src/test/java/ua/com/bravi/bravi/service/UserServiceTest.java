package ua.com.bravi.bravi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.component.InvocationContext;
import ua.com.bravi.bravi.domain.user.User;
import ua.com.bravi.bravi.domain.user.UserStatus;
import ua.com.bravi.bravi.domain.user.UserType;
import ua.com.bravi.bravi.exception.UserProvisioningException;
import ua.com.bravi.bravi.persistance.IUserEntityRepository;
import ua.com.bravi.bravi.persistance.entity.UserEntity;
import ua.com.bravi.bravi.persistance.mapper.UserEntityMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID EXT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final IUserEntityRepository repository = mock(IUserEntityRepository.class);
    private final UserEntityMapper mapper = mock(UserEntityMapper.class);
    private InvocationContext context;
    private UserService service;

    @BeforeEach
    void setUp() {
        context = new InvocationContext();
        context.setUserExtId(EXT_ID);
        service = new UserService(repository, mapper, context);
    }

    private static User domainUser(Long id, UserStatus status) {
        return new User(id, EXT_ID, UserType.SELLER, "John", "Doe", "john@example.com", status);
    }

    @Test
    void skipsWhenExtIdMissing() {
        context.setUserExtId(null);

        service.resolveCurrentUser();

        verify(repository, never()).findByExtId(any());
        assertThat(context.getUserId()).isNull();
    }

    @Test
    void loadsExistingUserIntoContextWithoutSaving() {
        UserEntity entity = new UserEntity();
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainUser(7L, UserStatus.BLOCKED));

        service.resolveCurrentUser();

        verify(repository, never()).save(any());
        assertThat(context.getUserId()).isEqualTo(7L);
        assertThat(context.getUserStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(context.getUserType()).isEqualTo(UserType.SELLER);
        assertThat(context.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void createsUserWhenMissing() {
        context.setUserType(UserType.SELLER);
        context.setFirstName("John");
        context.setLastName("Doe");
        context.setEmail("john@example.com");
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.empty());

        UserEntity toSave = new UserEntity();
        UserEntity saved = new UserEntity();
        when(mapper.toEntity(any(User.class))).thenReturn(toSave);
        when(repository.save(toSave)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(domainUser(15L, UserStatus.ACTIVE));

        service.resolveCurrentUser();

        verify(repository).save(toSave);
        assertThat(context.getUserId()).isEqualTo(15L);
        assertThat(context.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void recoversFromConcurrentInsert() {
        context.setUserType(UserType.SELLER);
        UserEntity existing = new UserEntity();
        when(repository.findByExtId(EXT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(mapper.toEntity(any(User.class))).thenReturn(new UserEntity());
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup ext_id"));
        when(mapper.toDomain(existing)).thenReturn(domainUser(21L, UserStatus.ACTIVE));

        service.resolveCurrentUser();

        assertThat(context.getUserId()).isEqualTo(21L);
    }

    @Test
    void rejectsCreationWhenUserTypeMissing() {
        context.setUserType(null);
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveCurrentUser())
                .isInstanceOf(UserProvisioningException.class)
                .hasMessageContaining("user_type");

        verify(repository, never()).save(any());
    }
}
