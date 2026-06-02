package com.ifsp.users.services;

import com.ifsp.users.dtos.CreateUserDto;
import com.ifsp.users.dtos.LoginUserDto;
import com.ifsp.users.dtos.RecoveryJwtTokenDto;
import com.ifsp.users.dtos.UserProfileDto;
import com.ifsp.users.entities.Role;
import com.ifsp.users.entities.User;
import com.ifsp.users.enums.RoleName;
import com.ifsp.users.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public RecoveryJwtTokenDto authenticateUser(LoginUserDto loginDto) {
        var authToken = new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password());
        var authentication = authenticationManager.authenticate(authToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(userDetails);
        return new RecoveryJwtTokenDto(token);
    }

    public void createUser(CreateUserDto createDto) {
        User newUser = User.builder()
                .email(createDto.email())
                .password(passwordEncoder.encode(createDto.password()))
                .roles(List.of(Role.builder().name(createDto.role()).build()))
                .build();
        userRepository.save(newUser);
    }

    public UserProfileDto getUserInformation(Authentication authentication) {
        String email = authentication.getName();

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        var roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return new UserProfileDto(user.getId(), user.getEmail(), roles);
    }

    public User getOrCreateUserForCode(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            var randomPassword = UUID.randomUUID().toString();

            var newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(randomPassword))
                    .roles(List.of(Role.builder().name(RoleName.ROLE_CUSTOMER).build()))
                    .build();

            return userRepository.save(newUser);
        });
    }
}
