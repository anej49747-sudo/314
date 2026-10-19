-- Story #15: one optional company record for each Interior Designer account.
-- Deleting a designer later also deletes its owned company information.
CREATE TABLE designer_companies (
    designer_id BIGINT PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    company_name VARCHAR(150) NOT NULL,
    address VARCHAR(300),
    description VARCHAR(3000),
    CONSTRAINT designer_companies_name_nonblank CHECK (length(btrim(company_name)) > 0)
);
