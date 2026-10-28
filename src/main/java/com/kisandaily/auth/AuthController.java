package com.kisandaily.auth;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api")
class AuthController {
    private final AuthService auth;
    AuthController(AuthService auth) {this.auth=auth;}
    @GetMapping("/health") Map<String,String> health() {return Map.of("status","UP");}
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    AuthDtos.Session register(@Valid @RequestBody AuthDtos.Register request) {return auth.register(request);}
    @PostMapping("/auth/login") AuthDtos.Session login(@Valid @RequestBody AuthDtos.Login request) {return auth.login(request);}
    @GetMapping("/auth/me") AuthDtos.User me(@AuthenticationPrincipal AuthDtos.Principal user) {return auth.me(user.id());}
    @PutMapping("/users/me/profile") AuthDtos.User profile(@AuthenticationPrincipal AuthDtos.Principal user,@Valid @RequestBody AuthDtos.Profile request) {return auth.update(user.id(),request);}
    @PostMapping("/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {auth.logout(authorization.substring(7));}
}
