package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeChangeStatusRequestDTO {

    @NotNull
    private Long homeId;

    @NotNull(message = "Status ID is required.")
    private Long statusId;
}