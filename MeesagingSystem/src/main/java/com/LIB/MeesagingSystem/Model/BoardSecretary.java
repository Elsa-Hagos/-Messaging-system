package com.LIB.MeesagingSystem.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

import java.util.Date;
import java.util.List;
@Data
@Document(collection = "BoardSecretary")
@NoArgsConstructor
@AllArgsConstructor
public class BoardSecretary implements UserDetails {
    @Id
    private String id;
    @NotNull
    private String firstName;
    @NotNull
    private String middleName;
    private String lastName;
    @NotNull
    private String password;
    @NotNull
    private String email;
    private String mobile;
    private List<String> groupID;
    private boolean isActive;
//    private LocalDateTime createdDate;
//    private LocalDateTime  updatedDate;
private Date createdDate;
private Date  updatedDate;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }
}
