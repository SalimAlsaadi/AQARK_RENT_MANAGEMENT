package com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "folder")
@Getter
@Setter
public class FolderProperties {

    private String buildings;
    private String building;
    private String rooms;
    private String flat;
    private String home;
}
