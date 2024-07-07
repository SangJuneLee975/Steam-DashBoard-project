// UserService.java
package com.example.apitest.service;

import com.example.apitest.DTO.User;
import java.util.List;

public interface UserService {
   public List<User> getAllUsers();
   public void insertUser(User user);
   public User findByUserId(String userId);
   public boolean authenticateUser(String userId, String password);
}
