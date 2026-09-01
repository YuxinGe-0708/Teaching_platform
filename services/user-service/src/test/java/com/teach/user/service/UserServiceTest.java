package com.teach.user.service;

import com.teach.user.entity.User;
import com.teach.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Test void loginRejectsDisabledUserWithoutCheckingPassword() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User(); user.setStatus(0); user.setPassword("encoded");
        when(mapper.findByUsername("disabled")).thenReturn(user);
        assertNull(new UserService(mapper, encoder).login("disabled", "secret"));
        verify(encoder, never()).matches(anyString(), anyString());
    }

    @Test void registerEncodesPasswordAndPersistsOwnedUserRecord() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("123456")).thenReturn("encoded");
        doAnswer(invocation -> { ((User) invocation.getArgument(0)).setId(7L); return 1; }).when(mapper).insert(any(User.class));
        User saved = new UserService(mapper, encoder).register("student7", "123456", "student", "Student 7");
        assertEquals(7L, saved.getId());
        assertEquals("encoded", saved.getPassword());
        verify(mapper).insert(saved);
    }
}
