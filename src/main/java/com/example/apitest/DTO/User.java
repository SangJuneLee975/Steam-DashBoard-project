// User.java
package com.example.apitest.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private String userId;
    private String password;
    private String name;
    private String nickname;


}
