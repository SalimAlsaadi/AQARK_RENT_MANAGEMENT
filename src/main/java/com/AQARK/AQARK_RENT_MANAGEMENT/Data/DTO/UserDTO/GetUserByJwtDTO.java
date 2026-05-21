package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO;

import lombok.Data;

import java.util.List;

@Data
public class GetUserByJwtDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private List<String> userType;
    private List<String> roles;

}
