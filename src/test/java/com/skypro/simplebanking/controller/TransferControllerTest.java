package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransferControllerTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;


    @Test
    @DirtiesContext
    @WithMockUser(username = "testuser", roles = "USER")
    void testTransfer() throws Exception{
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("testuser1");
        user1.setPassword("password");
        userRepository.save(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("testuser2");
        user2.setPassword("password");
        userRepository.save(user2);


        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setAccountCurrency(AccountCurrency.USD);
        fromAccount.setAmount(100000L);
        fromAccount.setUser(user1);
        accountRepository.save(fromAccount);


        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountCurrency(AccountCurrency.USD);
        toAccount.setAmount(50000L);
        toAccount.setUser(user2);
        accountRepository.save(toAccount);


        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromAccount.getId());
        transferRequest.setToUserId(user2.getId());
        transferRequest.setToAccountId(toAccount.getId());
        transferRequest.setAmount(50000L);


        BankingUserDetails bankingUserDetails1 = new BankingUserDetails(
                user1.getId(), user1.getUsername(), user1.getPassword(), false
        );


        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(bankingUserDetails1, null, bankingUserDetails1.getAuthorities())
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(transferRequest)))
                .andExpect(status().isOk());

        // Проверяем успешность перевода и обновление балансов
        fromAccount = accountRepository.findById(fromAccount.getId()).orElse(null);
        toAccount = accountRepository.findById(toAccount.getId()).orElse(null);

        assertNotNull(fromAccount);
        assertNotNull(toAccount);

        assertEquals(50000L, fromAccount.getAmount());
        assertEquals(100000L, toAccount.getAmount());
    }


    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}