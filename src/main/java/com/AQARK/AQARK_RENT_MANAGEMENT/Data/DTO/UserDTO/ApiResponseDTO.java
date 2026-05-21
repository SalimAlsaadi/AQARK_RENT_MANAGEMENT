package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO;

import lombok.Data;

@Data
public class ApiResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
}