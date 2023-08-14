package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.configuration.TestDatabaseConfiguration;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private UserRepository userRepository;

    @Value("${app.security.admin-token}")
    private String adminToken; // Inject the admin token value

    @Test
    @DirtiesContext
    void testCreateUserAsAdmin() throws Exception {
        String username = "testuser";
        String password = "password";

        mockMvc.perform(MockMvcRequestBuilders.post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-SECURITY-ADMIN-KEY", adminToken) // Use admin token for authentication
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isForbidden()); // Expect 403 (Forbidden)

        // Verify that the user was not saved to the database
        Optional<User> createdUser = userRepository.findByUsername(username);
        assertFalse(createdUser.isPresent());
    }

    @Test
    @DirtiesContext
    void testCreateUserAsUser() throws Exception {
            String username = "testuser";
            String password = "password";

            mockMvc.perform(MockMvcRequestBuilders.post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(user("testuser").password("password").roles("USER")) // Simulate USER role
                            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                    .andExpect(status().isForbidden()); // Ожидаем ошибку 403 (Запрещено)

            // Проверяем, что пользователь не был сохранен в базе данных
            Optional<User> createdUser = userRepository.findByUsername(username);
            assertFalse(createdUser.isPresent());
    }

    @Test
    @DirtiesContext
    @WithMockUser(username = "testuser", roles = "USER")
    public void testGetAllUsers() throws Exception {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password1");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password2");
        userRepository.save(user2);

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(user1.getUsername()))
                .andExpect(jsonPath("$[1].username").value(user2.getUsername()));
    }

    @Test
    @DirtiesContext
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void testGetAllUsersAsAdmin() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isForbidden()); // Expecting a 403 Forbidden response
    }

    @Test
    @DirtiesContext
    void testGetMyProfile() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        userRepository.save(user);

        BankingUserDetails bankingUserDetails = new BankingUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), false
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(bankingUserDetails, null, bankingUserDetails.getAuthorities())
        );

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()));
    }

    @Test
    @DirtiesContext
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void testGetMyProfileAsAdmin() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isForbidden()); // Expecting a 403 Forbidden response
    }
}