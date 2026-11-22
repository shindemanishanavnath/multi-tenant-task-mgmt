package com.techpulse.taskmanager.auth;

import com.techpulse.taskmanager.auth.AuthDtos.LoginRequest;
import com.techpulse.taskmanager.auth.AuthDtos.LoginResponse;
import com.techpulse.taskmanager.common.ApiException;
import com.techpulse.taskmanager.security.JwtService;
import com.techpulse.taskmanager.user.User;
import com.techpulse.taskmanager.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash compared against when the email is unknown, so both failure paths take similar time. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> user = userRepository.findByEmail(email);

        String hash = user.map(User::getPassword).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }

        User u = user.get();
        return new LoginResponse(jwtService.generateToken(u), "Bearer", jwtService.getExpirationSeconds(),
                u.getId(), u.getTenantId(), u.getRole());
    }
}
