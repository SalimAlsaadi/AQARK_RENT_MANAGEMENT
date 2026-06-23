-- ============================================================
-- Manual migration for tenant/landlord self-registration + CR number.
-- Run once against the AQARK_RENT_MANAGEMENT database (SQL Server).
--
-- Required because spring.jpa.hibernate.ddl-auto=validate: the app will
-- refuse to start until the users.cr_number column exists.
-- ============================================================

-- 1) Add the CR (Commercial Registration) number column to users.
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'cr_number'
)
BEGIN
    ALTER TABLE dbo.users ADD cr_number VARCHAR(50) NULL;
END
GO

-- 2) Enforce uniqueness of CR numbers across landlords only (a filtered
--    unique index allows many NULLs, unlike a plain UNIQUE constraint).
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = 'UX_users_cr_number' AND object_id = OBJECT_ID('dbo.users')
)
BEGIN
    CREATE UNIQUE INDEX UX_users_cr_number
        ON dbo.users (cr_number)
        WHERE cr_number IS NOT NULL;
END
GO

-- 3) Seed the two self-registration roles if missing. The service matches
--    the landlord role by the code 'LANDLORD' (case-insensitive).
IF NOT EXISTS (SELECT 1 FROM dbo.user_types WHERE code = 'TENANT')
    INSERT INTO dbo.user_types (code, description, is_active) VALUES ('TENANT', 'Tenant', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.user_types WHERE code = 'LANDLORD')
    INSERT INTO dbo.user_types (code, description, is_active) VALUES ('LANDLORD', 'Landlord', 1);
GO
