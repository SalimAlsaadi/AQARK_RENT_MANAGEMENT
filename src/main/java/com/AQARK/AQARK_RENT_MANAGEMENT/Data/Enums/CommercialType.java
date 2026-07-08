package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums;

import lombok.Getter;

@Getter
public enum CommercialType {

    SHOP(
            "SHOP",
            "Shop",
            "محل تجاري",
            "General retail shop",
            "storefront"
    ),

    OFFICE(
            "OFFICE",
            "Office",
            "مكتب",
            "Office or administrative business",
            "business_center"
    ),

    RESTAURANT(
            "RESTAURANT",
            "Restaurant",
            "مطعم",
            "Restaurant, cafe, food service",
            "restaurant"
    ),

    CAFE(
            "CAFE",
            "Cafe",
            "مقهى",
            "Coffee shop or cafe",
            "local_cafe"
    ),

    CLINIC(
            "CLINIC",
            "Clinic",
            "عيادة",
            "Medical clinic or healthcare business",
            "local_hospital"
    ),

    PHARMACY(
            "PHARMACY",
            "Pharmacy",
            "صيدلية",
            "Pharmacy or medicine shop",
            "medication"
    ),

    SALON(
            "SALON",
            "Salon",
            "صالون",
            "Salon, barber, beauty services",
            "content_cut"
    ),

    GYM(
            "GYM",
            "Gym",
            "نادي رياضي",
            "Fitness center or gym",
            "fitness_center"
    ),

    WAREHOUSE(
            "WAREHOUSE",
            "Warehouse",
            "مخزن",
            "Storage or warehouse space",
            "warehouse"
    ),

    SHOWROOM(
            "SHOWROOM",
            "Showroom",
            "معرض",
            "Display or showroom business",
            "store"
    ),

    WORKSHOP(
            "WORKSHOP",
            "Workshop",
            "ورشة",
            "Workshop, repair, maintenance business",
            "construction"
    ),

    SUPERMARKET(
            "SUPERMARKET",
            "Supermarket",
            "سوبرماركت",
            "Grocery or supermarket",
            "shopping_cart"
    ),

    BANK_BRANCH(
            "BANK_BRANCH",
            "Bank Branch",
            "فرع بنك",
            "Banking or financial services branch",
            "account_balance"
    ),

    EDUCATION_CENTER(
            "EDUCATION_CENTER",
            "Education Center",
            "مركز تعليمي",
            "Training center, institute, tuition center",
            "school"
    ),

    OTHER(
            "OTHER",
            "Other",
            "أخرى",
            "Other commercial activity",
            "category"
    );

    private final String code;
    private final String nameEn;
    private final String nameAr;
    private final String description;
    private final String icon;

    CommercialType(
            String code,
            String nameEn,
            String nameAr,
            String description,
            String icon
    ) {
        this.code = code;
        this.nameEn = nameEn;
        this.nameAr = nameAr;
        this.description = description;
        this.icon = icon;
    }
}