package ua.com.bravi.bravi.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.component.InvocationContext;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final InvocationContext invocationContext;

    @GetMapping("/test")
    public ResponseEntity<String> Test() {


        return ResponseEntity.ok(invocationContext.toString());

    }

}
