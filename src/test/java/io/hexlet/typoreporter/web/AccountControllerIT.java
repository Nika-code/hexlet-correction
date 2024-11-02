package io.hexlet.typoreporter.web;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.spring.api.DBRider;
import io.hexlet.typoreporter.domain.account.Account;
import io.hexlet.typoreporter.repository.AccountRepository;
import io.hexlet.typoreporter.service.dto.account.CustomUserDetails;
import io.hexlet.typoreporter.test.DBUnitEnumPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.github.database.rider.core.api.configuration.Orthography.LOWERCASE;
import static io.hexlet.typoreporter.test.Constraints.POSTGRES_IMAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DBRider
@DBUnit(caseInsensitiveStrategy = LOWERCASE, dataTypeFactoryClass = DBUnitEnumPostgres.class, cacheConnection = false)
public class AccountControllerIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE)
        .withPassword("inmemory")
        .withUsername("inmemory");

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void updateAccountWithWrongEmailDomain() throws Exception {
        String userName = "testUser";
        String correctEmailDomain = "test@test.test";
        String password = "_Qwe1234";
        mockMvc.perform(post("/signup")
            .param("username", userName)
            .param("email", correctEmailDomain)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", userName)
            .param("lastName", userName)
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(correctEmailDomain)).isNotEmpty();
        assertThat(accountRepository.findAccountByEmail(correctEmailDomain).orElseThrow().getEmail())
            .isEqualTo(correctEmailDomain);

        String wrongEmailDomain = "test@test";
        mockMvc.perform(post("/account/update")
            .param("username", userName)
            .param("email", wrongEmailDomain)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", userName)
            .param("lastName", userName)
            .with(user(new CustomUserDetails(correctEmailDomain, "password", "SampleNickname",
                List.of(new SimpleGrantedAuthority("USER")))))
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(wrongEmailDomain)).isEmpty();
        assertThat(accountRepository.findAccountByEmail(correctEmailDomain).orElseThrow().getEmail())
            .isEqualTo(correctEmailDomain);
    }

    @Test
    void updateAccountEmailUsingDifferentCase() throws Exception {
        final String username = "testUser";
        final String emailUpperCase = "TEST@TEST.RU";
        final String emailMixedCase = "TEST@test.Ru";
        final String emailLowerCase = "test@test.ru";
        final String password = "_Qwe1234";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", emailMixedCase)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", username)
            .param("lastName", username)
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(emailLowerCase)).isNotEmpty();

        mockMvc.perform(put("/account/update")
            .param("firstName", username)
            .param("lastName", username)
            .param("username", username)
            .param("email", emailUpperCase)
            .with(user(new CustomUserDetails(emailLowerCase, "password", "SampleNickname",
                List.of(new SimpleGrantedAuthority("USER")))))
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(emailUpperCase)).isEmpty();
        assertThat(accountRepository.findAccountByEmail(emailLowerCase)).isNotEmpty();
    }

    @Test
    void getAccountInfoPageTest() throws Exception {
        final String email = "testUser@test.com";
        final String username = "testUser";
        final String password = "_Qwe1234";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", "Test")
            .param("lastName", "User")
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isNotEmpty();

        mockMvc.perform(get("/account")
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER"))))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("workspaceRoleInfoList"))
            .andExpect(model().attributeExists("accInfo"))
            .andExpect(view().name("account/acc-info"));
    }

    @Test
    void getProfilePageTest() throws Exception {
        final String email = "testUser@test.com";
        final String username = "testUser";
        final String password = "_Qwe1234";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", "Test")
            .param("lastName", "User")
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isNotEmpty();

        mockMvc.perform(get("/account/update")
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER"))))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("formModified"))
            .andExpect(model().attribute("formModified", false))
            .andExpect(model().attributeExists("updateProfile"))
            .andExpect(view().name("account/prof-update"));
    }

    @Test
    void getPasswordPageTest() throws Exception {
        final String email = "testUser@test.com";
        final String username = "testUser";
        final String password = "_Qwe1234";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", "Test")
            .param("lastName", "User")
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isNotEmpty();

        mockMvc.perform(get("/account/password")
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER"))))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("formModified"))
            .andExpect(model().attribute("formModified", false))
            .andExpect(model().attributeExists("updatePassword"))
            .andExpect(view().name("account/pass-update"));
    }

    @Test
    void updatePasswordSuccessfully() throws Exception {
        final String username = "testUser";
        final String email = "testUser@test.com";
        final String password = "_Qwe1234";
        final String newPassword = "_Asd5678";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", "Test")
            .param("lastName", "User")
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isNotEmpty();

        mockMvc.perform(put("/account/password")
                .param("oldPassword", password)
                .param("newPassword", newPassword)
                .param("confirmNewPassword", newPassword)
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER")))))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account"));

        assertThat(accountRepository.findAccountByEmail(email.toLowerCase()))
            .isPresent()
            .get()
            .extracting(Account::getPassword).isNotEqualTo(password);
    }

    @Test
    void updatePasswordWithOldPasswordWrong() throws Exception {
        final String username = "testUser";
        final String email = "testUser@test.com";
        final String password = "_Qwe1234";
        final String wrongOldPassword = "wrongPassword";
        final String newPassword = "_Asd5678";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", "Test")
            .param("lastName", "User")
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isNotEmpty();

        mockMvc.perform(put("/account/password")
                .param("oldPassword", wrongOldPassword)
                .param("newPassword", newPassword)
                .param("confirmNewPassword", newPassword)
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER")))))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("updatePassword", "oldPassword"))
            .andExpect(view().name("account/pass-update"));
    }

    @Test
    void updatePasswordWithValidationErrors() throws Exception {
        final String username = "testUser";
        final String email = "testUser@test.com";
        final String password = "_Qwe1234";
        final String invalidNewPassword = "";

        mockMvc.perform(post("/signup")
            .param("username", username)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", "Test")
            .param("lastName", "User")
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isPresent();

        mockMvc.perform(put("/account/password")
                .param("oldPassword", password)
                .param("newPassword", invalidNewPassword)
                .param("confirmNewPassword", invalidNewPassword)
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER")))))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("updatePassword", "newPassword"))
            .andExpect(view().name("account/pass-update"));
    }

    @Test
    void handleAccountNotFoundException() throws Exception {
        final String email = "nonexistent@test.com";
        final String username = "testUser";
        final String password = "_Qwe1234";

        assertThat(accountRepository.findAccountByEmail(email.toLowerCase())).isEmpty();

        mockMvc.perform(put("/account/update")
                .param("firstName", "Test")
                .param("lastName", "User")
                .param("username", username)
                .param("email", email)
                .with(user(new CustomUserDetails(email.toLowerCase(), password, username,
                    List.of(new SimpleGrantedAuthority("USER")))))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("/error-general"));
    }
}
