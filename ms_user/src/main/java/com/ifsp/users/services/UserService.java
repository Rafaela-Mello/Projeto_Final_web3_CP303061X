package com.ifsp.users.services;

import com.ifsp.users.dtos.CreateUserDto;
import com.ifsp.users.dtos.LoginUserDto;
import com.ifsp.users.dtos.RecoveryJwtTokenDto;
import com.ifsp.users.dtos.UpdateProfileDto;
import com.ifsp.users.dtos.UserProfileDto;
import com.ifsp.users.entities.Role;
import com.ifsp.users.entities.User;
import com.ifsp.users.enums.RoleName;
import com.ifsp.users.repositories.RoleRepository;
import com.ifsp.users.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private RoleRepository roleRepository;

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

        return toUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(String email, UpdateProfileDto dto) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        var role = roleRepository.findByName(dto.role())
                .orElseGet(() -> roleRepository.save(Role.builder().name(dto.role()).build()));

        user.setName(dto.name());
        user.setRoles(new ArrayList<>(List.of(role)));
        userRepository.save(user);

        var updated = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return toUserProfileDto(updated);
    }

    private UserProfileDto toUserProfileDto(User user) {
        var roles = user.getRoles().stream()
                .filter(role -> role.getName() != null)
                .map(role -> role.getName().name())
                .toList();

        return new UserProfileDto(user.getId(), user.getEmail(), user.getName(), roles);
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
