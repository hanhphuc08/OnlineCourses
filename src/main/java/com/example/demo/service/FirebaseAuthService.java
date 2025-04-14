package com.example.demo.service;

import com.example.demo.model.users;
import com.example.demo.util.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public FirebaseAuthService(FirebaseAuth firebaseAuth, @Lazy UserService userService, JwtUtil jwtUtil) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> registerUser(users user) throws FirebaseAuthException {
        // Create user in Firebase Authentication
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
            .setEmail(user.getEmail())
            .setPassword(user.getPassword())
            .setPhoneNumber(user.getPhoneNumber())
            .setDisplayName(user.getFullname())
            .setEmailVerified(false);

        UserRecord userRecord = firebaseAuth.createUser(request);
        
        // Save user information to database (role will be set as CUSTOMER by UserService)
        users savedUser = userService.saveUser(user);
        
        // Create custom token for client-side authentication
        String customToken = firebaseAuth.createCustomToken(userRecord.getUid());
        
        // Create JWT token
        String jwtToken = jwtUtil.generateToken(user.getEmail());
        
        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("uid", userRecord.getUid());
        response.put("customToken", customToken);
        response.put("jwtToken", jwtToken);
        response.put("user", savedUser);
        
        return response;
    }

    public UserDetails verifyIdToken(String idToken) throws FirebaseAuthException {
        // Verify Firebase ID token
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        String email = decodedToken.getEmail();
        
        // Get user from database
        users user = userService.findByEmailOrPhone(email);
        
        // Create authorities list with user's role
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()));
        }
        
        // Create UserDetails object
        return new User(user.getEmail(), user.getPassword(), authorities);
    }

    public String createCustomToken(String uid) throws FirebaseAuthException {
        return firebaseAuth.createCustomToken(uid);
    }

    public void deleteUser(String uid) throws FirebaseAuthException {
        firebaseAuth.deleteUser(uid);
    }
} 