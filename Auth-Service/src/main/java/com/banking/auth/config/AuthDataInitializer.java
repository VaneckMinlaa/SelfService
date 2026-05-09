package com.banking.auth.config;

import com.banking.auth.entity.User;
import com.banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthDataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Initializing demo users from Java code...");
            String hash = passwordEncoder.encode("password123"); 
            createUser("dongmomintidem@gmail.com", hash, "Vaneck", "Minlaa", "+237680434667");
            createUser("demo@vaneck.com", hash, "Demo", "User", "+237000000000");
            createUser("minlaaalex@gmail.com.com", hash, "Bilogue", "Eric", "+237691831919");
            createUser("esther.nguedia@saintjeaningenieur.org", hash, "Nguedia", "Lucrecce", "+237672538262");
        }
    }

    private void createUser(String email, String hash, String fName, String lName, String phone) {
        User user = new User();
        user.setEmail(email); user.setPasswordHash(hash); user.setFirstName(fName);
        user.setLastName(lName); user.setPhoneNumber(phone); user.setIsActive(true);
        userRepository.save(user);
    }
}
