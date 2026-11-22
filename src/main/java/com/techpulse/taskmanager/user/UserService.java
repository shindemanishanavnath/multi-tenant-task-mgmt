package com.techpulse.taskmanager.user;

import com.techpulse.taskmanager.common.ApiException;
import com.techpulse.taskmanager.security.TenantContext;
import com.techpulse.taskmanager.user.UserDtos.CreateUserRequest;
import com.techpulse.taskmanager.user.UserDtos.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates a user inside the caller's tenant (taken from the JWT via TenantContext). */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("A user with this email already exists");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.role(), tenantId);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findAllByTenantIdOrderByIdAsc(tenantId).stream()
                .map(UserResponse::from)
                .toList();
    }
}
