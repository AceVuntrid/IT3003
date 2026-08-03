package com.university.assets.auth;

import com.university.assets.auth.dto.AuthDtos;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final Pattern pattern = Pattern.compile(AuthDtos.PASSWORD_PATTERN);

    @Test
    void acceptsCompliantPassword() {
        assertThat(pattern.matcher("Admin@1234").matches()).isTrue();
        assertThat(pattern.matcher("aB3$efgh").matches()).isTrue();
    }

    @Test
    void rejectsMissingUppercase() {
        assertThat(pattern.matcher("admin@1234").matches()).isFalse();
    }

    @Test
    void rejectsMissingLowercase() {
        assertThat(pattern.matcher("ADMIN@1234").matches()).isFalse();
    }

    @Test
    void rejectsMissingDigit() {
        assertThat(pattern.matcher("Admin@abcd").matches()).isFalse();
    }

    @Test
    void rejectsMissingSpecialCharacter() {
        assertThat(pattern.matcher("Admin12345").matches()).isFalse();
    }

    @Test
    void rejectsTooShort() {
        assertThat(pattern.matcher("Ab1$xyz").matches()).isFalse();
    }
}
