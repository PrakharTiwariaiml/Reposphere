package com.Notes.rep.service;

import com.Notes.rep.config.UserRepository;
import com.Notes.rep.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder; // This hashes the password

    @Autowired
    JwtUtils jwtUtils;

    // The Signup Logic
    public String registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Error: Email is already in use!";
        }

        // IMPORTANT: Encrypt the password before saving!
        user.setPassword(encoder.encode(user.getPassword()));
        userRepository.save(user);

        return "User registered successfully!";
    }

    // The Login Logic
    public String authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the typed password matches the hashed password in DB
        if (encoder.matches(password, user.getPassword())) {
            // If match, give them their ID Card (Token)
            return jwtUtils.generateJwtToken(user.getUsername());
        } else {
            return "Error: Invalid credentials!";
        }
    }
}
