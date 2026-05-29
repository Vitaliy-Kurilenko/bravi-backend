package ua.com.bravi.bravi.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import ua.com.bravi.bravi.domain.user.UserStatus;
import ua.com.bravi.bravi.domain.user.UserType;

import java.util.Set;
import java.util.UUID;

@Component
@RequestScope
@Getter
@Setter
public class InvocationContext {

    private String requestId;
    private UUID userExtId;
    private String username;
    private String email;
    private Set<String> roles;
    private DeviceInfo device;

    private Long userId;
    private String firstName;
    private String lastName;
    private UserType userType;
    private UserStatus userStatus;

    public String toString(){
        return "InvocationContext{" +
            "requestId='" + requestId + '\'' +
            ", userExtId=" + userExtId +
            ", username='" + username + '\'' +
            ", email='" + email + '\'' +
            ", roles=" + roles +
            ", device=" + device +
            ", userId=" + userId +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", userType=" + userType +
            ", userStatus=" + userStatus +
            '}';
    }
}
