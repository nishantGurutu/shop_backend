package com.kisandaily.auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
@Service
class AuthService {
    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;
    private final long sessionHours;
    AuthService(UserRepository users,SessionRepository sessions,PasswordEncoder passwords,@Value("${app.session-hours}") long sessionHours) {
        this.users=users; this.sessions=sessions; this.passwords=passwords; this.sessionHours=sessionHours;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
        if(sessionHours<1 || sessionHours>720) throw new IllegalArgumentException("SESSION_HOURS must be 1..720");
    }
    static String normalize(String value) {
        String id=value.strip().toLowerCase(Locale.ROOT);
        if(!id.matches("[a-z0-9][a-z0-9._-]{2,29}")) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_USER_ID","User ID 3–30 अक्षर: a-z, 0-9, dot, underscore या hyphen लिखें");
        return id;
    }
    static void validatePassword(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_PASSWORD","Password UTF-8 में 72 bytes से छोटा रखें");
    }
    static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private AuthDtos.Session issue(FarmerUser user) {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry=Instant.now().plus(Duration.ofHours(sessionHours));
        sessions.save(new LoginSession(hash(token),user,expiry));
        return new AuthDtos.Session(token,expiry,AuthDtos.User.from(user));
    }
    @Transactional
    public AuthDtos.Session register(AuthDtos.Register r) {
        String id=normalize(r.userId()); validatePassword(r.password());
        if(users.existsByUserId(id)) throw new ApiException(HttpStatus.CONFLICT,"USER_ID_TAKEN","यह User ID पहले से इस्तेमाल हो रही है");
        FarmerUser u=new FarmerUser(); u.userId=id; u.passwordHash=passwords.encode(r.password()); u.name=r.name().strip(); u.phone=r.phone()==null ? "" : r.phone();
        users.saveAndFlush(u);
        return issue(u);
    }
    @Transactional
    public AuthDtos.Session login(AuthDtos.Login r) {
        String id=normalize(r.userId()); validatePassword(r.password());
        Optional<FarmerUser> found=users.findByUserId(id);
        boolean valid=passwords.matches(r.password(),found.map(u->u.passwordHash).orElse(dummyHash));
        if(found.isEmpty() || !valid) throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS","User ID या password गलत है");
        return issue(found.get());
    }
    @Transactional(readOnly=true)
    public Optional<AuthDtos.Principal> authenticate(String token) {
        if(!token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return sessions.findById(hash(token)).filter(s->s.expiresAt.isAfter(Instant.now())).map(s->new AuthDtos.Principal(s.user.id,s.user.userId));
    }
    @Transactional(readOnly=true)
    public AuthDtos.User me(UUID id) { return AuthDtos.User.from(users.findById(id).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","फिर login करें"))); }
    @Transactional
    public AuthDtos.User update(UUID id,AuthDtos.Profile p) {
        FarmerUser u=users.findById(id).orElseThrow();
        u.name=p.name().strip();u.phone=p.phone()==null?"":p.phone();u.village=p.village().strip();u.district=p.district().strip();u.state=p.state().strip();u.language=p.language().strip();u.landArea=p.landArea()==null?"":p.landArea().strip();
        u.selectedCrops=new ArrayList<>(new LinkedHashSet<>(p.selectedCrops().stream().map(String::strip).toList()));u.profileCompleted=true;
        return AuthDtos.User.from(u);
    }
    @Transactional
    public void logout(String token) { sessions.deleteById(hash(token)); }
}
