package ua.com.bravi.bravi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ua.com.bravi.bravi.component.InvocationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest {

    @Test
    void testEndpointReturnsInvocationContextString() {
        InvocationContext context = new InvocationContext();
        context.setRequestId("corr-1");
        context.setUserExtId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        context.setUsername("john");
        UserController controller = new UserController(context);

        ResponseEntity<String> response = controller.Test();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(context.toString());
        assertThat(response.getBody()).contains("corr-1", "john");
    }
}
