package com.example.heritage_sharing_api.unit;

import com.example.heritage_sharing_api.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for EmailVerificationService")
class EmailVerificationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService();
        // Use reflection to inject the mocked redisTemplate
        try {
            java.lang.reflect.Field field = EmailVerificationService.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            field.set(emailVerificationService, redisTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject redisTemplate", e);
        }
    }

    @Test
    @DisplayName("UT-EV-01: generateVerificationCode generates a 4-digit code and stores it in Redis")
    void generateVerificationCodeGeneratesCodeAndStoresInRedis() {
        String email = "test@example.com";
        String expectedKey = "verification_code:test@example.com";
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String code = emailVerificationService.generateVerificationCode(email);

        assertEquals(4, code.length());
        assertTrue(code.matches("\\d{4}"));
        verify(valueOperations).set(expectedKey, code, 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("UT-EV-02: verifyCode returns true when code matches")
    void verifyCodeReturnsTrueWhenCodeMatches() {
        String email = "test@example.com";
        String code = "1234";
        String key = "verification_code:test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(code);

        boolean result = emailVerificationService.verifyCode(email, code);

        assertTrue(result);
        verify(valueOperations).get(key);
    }

    @Test
    @DisplayName("UT-EV-03: verifyCode returns false when code does not match")
    void verifyCodeReturnsFalseWhenCodeDoesNotMatch() {
        String email = "test@example.com";
        String storedCode = "1234";
        String providedCode = "5678";
        String key = "verification_code:test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(storedCode);

        boolean result = emailVerificationService.verifyCode(email, providedCode);

        assertFalse(result);
    }

    @Test
    @DisplayName("UT-EV-04: verifyCode returns false when code is null")
    void verifyCodeReturnsFalseWhenCodeIsNull() {
        String email = "test@example.com";
        String key = "verification_code:test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("1234");

        boolean result = emailVerificationService.verifyCode(email, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("UT-EV-05: verifyCode returns false when stored code is null")
    void verifyCodeReturnsFalseWhenStoredCodeIsNull() {
        String email = "test@example.com";
        String code = "1234";
        String key = "verification_code:test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        boolean result = emailVerificationService.verifyCode(email, code);

        assertFalse(result);
    }

    @Test
    @DisplayName("UT-EV-06: removeCode deletes the verification code from Redis")
    void removeCodeDeletesCodeFromRedis() {
        String email = "test@example.com";
        String key = "verification_code:test@example.com";

        emailVerificationService.removeCode(email);

        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("UT-EV-07: generateVerificationCode throws exception when Redis connection fails")
    void generateVerificationCodeThrowsExceptionOnRedisFailure() {
        String email = "test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(any(), any(), anyLong(), any());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailVerificationService.generateVerificationCode(email));

        assertTrue(exception.getMessage().contains("Failed to connect to Redis"));
    }

    @Test
    @DisplayName("UT-EV-08: verifyCode throws exception when Redis connection fails")
    void verifyCodeThrowsExceptionOnRedisFailure() {
        String email = "test@example.com";
        String key = "verification_code:test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis connection failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailVerificationService.verifyCode(email, "1234"));

        assertTrue(exception.getMessage().contains("Failed to connect to Redis"));
    }
}