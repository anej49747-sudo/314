-- Story #30: deleting a category must never delete projects or their attached data.
-- The FK action atomically unsets category_id for all affected projects.
ALTER TABLE interior_design_projects
    DROP CONSTRAINT interior_design_projects_category_id_fkey;

ALTER TABLE interior_design_projects
    ALTER COLUMN category_id DROP NOT NULL;

ALTER TABLE interior_design_projects
    ADD CONSTRAINT interior_design_projects_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES project_categories(id) ON DELETE SET NULL;
