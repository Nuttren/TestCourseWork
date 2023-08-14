package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;


    @Test
    void testGetUserAccount() throws Exception{
        User user = createUser("testuser");
        Account account = createAccount(user, 1L, 100000L);

        BankingUserDetails bankingUserDetails = new BankingUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), false
        );

        // Use the custom UserDetails as the authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(bankingUserDetails, null, bankingUserDetails.getAuthorities())
        );

        mockMvc.perform(get("/account/{id}", account.getId())
                        .with(request -> {
                            request.setRemoteUser("testuser");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(account.getId()));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void testGetUserAccountAsAdmin() throws Exception {
        User user = createUser("testuser");
        Account account = createAccount(user, 1L, 100000L);

        mockMvc.perform(get("/account/{id}", account.getId()))
                .andExpect(status().isForbidden()); // Expecting a 403 Forbidden response
    }

    @Test
    void testDepositToAccount() throws Exception {
        User user = createUser("testuser");
        Account account = createAccount(user, 1L, 100000L);

        BankingUserDetails bankingUserDetails = new BankingUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), false
        );

        // Use the custom UserDetails as the authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(bankingUserDetails, null, bankingUserDetails.getAuthorities())
        );

        mockMvc.perform(post("/account/deposit/{id}", account.getId())
                        .with(request -> {
                            request.setRemoteUser("testuser");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createBalanceChangeRequest(100000L))))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(account.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(account.getAmount() + 100000L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(account.getAccountCurrency().toString()));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void testDepositToOtherUsersAccountAsAdmin() throws Exception {
        User user = createUser("testuser");
        Account account = createAccount(user, 1L, 100000L);

        mockMvc.perform(post("/account/deposit/{id}", account.getId())
                        .with(request -> {
                            request.setRemoteUser("adminuser"); // Setting an admin user
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createBalanceChangeRequest(100000L))))
                .andExpect(status().isForbidden()); // Expecting a 403 Forbidden response
    }

    @Test
    void testWithdrawFromAccount() throws Exception {
        User user = createUser("testuser");
        Account account = createAccount(user, 1L, 100000L);

        BankingUserDetails bankingUserDetails = new BankingUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), false
        );

        // Use the custom UserDetails as the authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(bankingUserDetails, null, bankingUserDetails.getAuthorities())
        );

        mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                        .with(request -> {
                            request.setRemoteUser("testuser");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createBalanceChangeRequest(50000L))))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(account.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(account.getAmount() - 50000L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(account.getAccountCurrency().toString()));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void testWithdrawFromOtherUsersAccountAsAdmin() throws Exception {
        User user = createUser("testuser");
        Account account = createAccount(user, 1L, 100000L);

        mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                        .with(request -> {
                            request.setRemoteUser("adminuser");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createBalanceChangeRequest(50000L))))
                .andExpect(status().isForbidden()); // Expecting a 403 Forbidden response
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("password");
        userRepository.save(user);
        return user;
    }

    private Account createAccount(User user, Long accountId, long amount) {
        Account account = new Account();
        account.setId(accountId);
        account.setAccountCurrency(AccountCurrency.USD);
        account.setAmount(amount);
        account.setUser(user);
        accountRepository.save(account);
        return account;
    }

    private BalanceChangeRequest createBalanceChangeRequest(long amount) {
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amount);
        return balanceChangeRequest;
    }
}