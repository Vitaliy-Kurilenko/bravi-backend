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
import ua.com.bravi.bravi.identity.persistence.IUserEntityRepository.UserContextProjection;
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
        return new User(id, "usr_" + id, EXT_ID, "John", "Doe", "john@example.com", true, status);
    }

    @Test
    void resolveSkipsWhenExtIdMissing() {
        context.setUserExtId(null);

        CurrentUserView result = service.resolveCurrentUser();

        assertThat(result).isNull();
        verify(repository, never()).findContextByExtId(any());
    }

    @Test
    void resolveDoesNotCreateWhenUserUnknown() {
        when(repository.findContextByExtId(EXT_ID)).thenReturn(Optional.empty());

        CurrentUserView result = service.resolveCurrentUser();

        assertThat(result).isNull();
        assertThat(context.getUserId()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void resolvePopulatesContextWhenUserExists() {
        UserContextProjection projection = mock(UserContextProjection.class);
        when(projection.getUserId()).thenReturn(9L);
        when(projection.getUserPublicId()).thenReturn("usr_9");
        when(projection.getUserExtId()).thenReturn(EXT_ID);
        when(projection.getUserStatus()).thenReturn(UserStatus.ACTIVE);
        when(projection.isEmailVerified()).thenReturn(true);
        when(projection.getFirstName()).thenReturn("Jane");
        when(projection.getLastName()).thenReturn("Roe");
        when(projection.getEmail()).thenReturn("jane@example.com");
        when(repository.findContextByExtId(EXT_ID)).thenReturn(Optional.of(projection));

        CurrentUserView view = service.resolveCurrentUser();

        assertThat(view.id()).isEqualTo(9L);
        assertThat(view.publicId()).isEqualTo("usr_9");
        assertThat(view.emailVerified()).isTrue();
        assertThat(context.getUserId()).isEqualTo(9L);
        assertThat(context.getUserPublicId()).isEqualTo("usr_9");
        assertThat(context.isEmailVerified()).isTrue();
    }

    @Test
    void getCurrentUserContextReadsFromInvocationContextWithoutQuery() {
        context.setUserId(9L);
        context.setUserPublicId("usr_9");
        context.setUserStatus("ACTIVE");
        context.setEmailVerified(true);
        context.setFirstName("Jane");
        context.setLastName("Roe");
        context.setEmail("jane@example.com");

        CurrentUserView view = service.getCurrentUserContext();

        assertThat(view.id()).isEqualTo(9L);
        assertThat(view.publicId()).isEqualTo("usr_9");
        assertThat(view.extId()).isEqualTo(EXT_ID);
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.emailVerified()).isTrue();
        assertThat(view.firstName()).isEqualTo("Jane");
        assertThat(view.lastName()).isEqualTo("Roe");
        assertThat(view.email()).isEqualTo("jane@example.com");
        verify(repository, never()).findContextByExtId(any());
    }

    @Test
    void provisionUserCreatesWhenMissingAndPublishesEvent() {
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.empty());
        UserEntity toSave = new UserEntity();
        UserEntity saved = new UserEntity();
        when(mapper.toEntity(any(User.class))).thenReturn(toSave);
        when(repository.save(toSave)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(domainUser(15L, UserStatus.ACTIVE));

        CurrentUserView result = service.provisionUser(EXT_ID, "john@example.com", "John", "Doe");

        verify(repository).save(toSave);
        verify(eventPublisher).publishEvent(any(UserProvisionedEvent.class));
        assertThat(result.id()).isEqualTo(15L);
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void provisionUserReturnsExistingWhenAlreadyRegistered() {
        UserEntity existing = new UserEntity();
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.of(existing));
        when(mapper.toDomain(existing)).thenReturn(domainUser(7L, UserStatus.ACTIVE));

        CurrentUserView result = service.provisionUser(EXT_ID, "john@example.com", "John", "Doe");

        assertThat(result.id()).isEqualTo(7L);
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void provisionUserRecoversFromConcurrentInsert() {
        UserEntity existing = new UserEntity();
        when(repository.findByExtId(EXT_ID)).thenReturn(Optional.empty(), Optional.of(existing));
        when(mapper.toEntity(any(User.class))).thenReturn(new UserEntity());
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup ext_id"));
        when(mapper.toDomain(existing)).thenReturn(domainUser(21L, UserStatus.ACTIVE));

        CurrentUserView result = service.provisionUser(EXT_ID, "john@example.com", "John", "Doe");

        assertThat(result.id()).isEqualTo(21L);
    }
}
