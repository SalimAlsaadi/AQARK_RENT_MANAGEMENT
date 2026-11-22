package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatUpdateRequestDTO {

    @NotNull(message = "Flat id is required")
    private Long flatId;

    @NotBlank(message = "Flat number is required")
    @Size(max = 50, message = "Flat number must not exceed 50 characters")
    private String flatNumber;

    private boolean hasAC;

    private boolean hasWIFI;

    @JsonProperty("isFurnished")
    private boolean isFurnished;

    private String statusCode;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 100, message = "Tenant type must not exceed 100 characters")
    private String tenantType;

    @Size(max = 100, message = "Rent time type must not exceed 100 characters")
    private String rentTimeType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.000", message = "Price cannot be negative")
    private BigDecimal price;

    @Size(max = 30, message = "Mobile number must not exceed 30 characters")
    private String mobileNumber;
}