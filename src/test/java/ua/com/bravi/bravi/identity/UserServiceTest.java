package ua.com.bravi.bravi.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.shared.component.InvocationContext;
import ua.com.bravi.bravi.identity.api.CurrentUserView;
import ua.com.bravi.bravi.identity.api.event.UserProvisionedEvent;
import ua.com.bravi.bravi.identity.domain.User;
import ua.com.bravi.bravi.identity.domain.UserStatus;
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository;
import ua.com.bravi.bravi.identity.persistence.entity.UserEntity;
import ua.com.bravi.bravi.identity.persistence.mapper.UserEntityMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private InvocationContext context;
    private UserService service;

    @BeforeEach
    void setUp() {
        context = new InvocationContext();
        context.setUserExtId(EXT_ID);
        service = new UserService(repository, mapper, context, eventPublisher);
    }

    private static User domainUser(Long id, UserStatus status) {
        return new User(id, EXT_ID, "John", "Doe", "john@example.com", status);
    }

    @Test
    void skipsWhenExtIdMissing() {
        context.setUserExtId(null);

        CurrentUserView result = service.resolveCurrentUser();

        assertThat(result).isNull();
        verify(repository, never()).findContextByExtId(any());
    }

    @Test
    void getCurrentUserContextReadsFromInvocationContextWithoutQuery() {
        context.setUserId(9L);
        context.setUserStatus("ACTIVE");
        context.setFirstName("Jane");
        context.setLastName("Roe");
        context.setEmail("jane@example.com");

        CurrentUserView view = service.getCurrentUserContext();

        assertThat(view.id()).isEqualTo(9L);
        assertThat(view.extId()).isEqualTo(EXT_ID);
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.firstName()).isEqualTo("Jane");
        assertThat(view.lastName()).isEqualTo("Roe");
        assertThat(view.email()).isEqualTo("jane@example.com");
        verify(repository, never()).findContextByExtId(any());
    }

    @Test
    void createsUserWhenMissingAndPublishesEvent() {
        context.setFirstName("John");
        context.setLastName("Doe");
        context.setEmail("john@example.com");
        when(repository.findContextByExtId(EXT_ID)).thenReturn(Optional.empty());

        UserEntity toSave = new UserEntity();
        UserEntity saved = new UserEntity();
        when(mapper.toEntity(any(User.class))).thenReturn(toSave);
        when(repository.save(toSave)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(domainUser(15L, UserStatus.ACTIVE));

        CurrentUserView result = service.resolveCurrentUser();

        verify(repository).save(toSave);
        verify(eventPublisher).publishEvent(any(UserProvisionedEvent.class));
        assertThat(result.id()).isEqualTo(15L);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(context.getUserId()).isEqualTo(15L);
        assertThat(context.getUserStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void recoversFromConcurrentInsert() {
        UserEntity existing = new UserEntity();
        when(repository.findContextByExtId(EXT_ID)).thenReturn(Optional.empty());
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.of(existing));
        when(mapper.toEntity(any(User.class))).thenReturn(new UserEntity());
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup ext_id"));
        when(mapper.toDomain(existing)).thenReturn(domainUser(21L, UserStatus.ACTIVE));

        CurrentUserView result = service.resolveCurrentUser();

        assertThat(result.id()).isEqualTo(21L);
        assertThat(context.getUserId()).isEqualTo(21L);
    }
}
