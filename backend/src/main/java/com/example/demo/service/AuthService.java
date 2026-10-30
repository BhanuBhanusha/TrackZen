package com.example.demo.service;

import com.example.demo.dto.AuthRequestDto;
import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.RegisterRequestDto;
import com.example.demo.entity.AgentProfile;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Role;
import com.example.demo.exception.BusinessValidationException;
import com.example.demo.repository.AgentProfileRepository;
import com.example.demo.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(AppUserRepository appUserRepository,
                       AgentProfileRepository agentProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.appUserRepository = appUserRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto dto) {
        if (appUserRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BusinessValidationException("Email already registered: " + dto.getEmail());
        }

        AppUser user = new AppUser();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        user.setActive(true);

        AppUser savedUser = appUserRepository.save(user);

        if (dto.getRole() == Role.ROLE_SUPPORT_AGENT) {
            AgentProfile profile = new AgentProfile();
            profile.setUser(savedUser);
            profile.setDepartment("General Support");
            profile.setCurrentLoad(0);
            profile.setMaxCapacity(5);
            profile.setAvailable(true);
            agentProfileRepository.save(profile);
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", savedUser.getFullName());
        extraClaims.put("role", savedUser.getRole().name());

        String token = jwtService.generateToken(extraClaims, savedUser);
        return new AuthResponseDto(token, savedUser.getRole().name(), savedUser.getFullName());
    }

    public AuthResponseDto authenticate(AuthRequestDto dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        AppUser user = appUserRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BusinessValidationException("User not found with email: " + dto.getEmail()));

        if (!user.isActive()) {
            throw new BusinessValidationException("User account is deactivated");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("role", user.getRole().name());

        String token = jwtService.generateToken(extraClaims, user);
        return new AuthResponseDto(token, user.getRole().name(), user.getFullName());
    }
}
