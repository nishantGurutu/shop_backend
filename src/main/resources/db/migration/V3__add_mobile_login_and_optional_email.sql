-- Legacy users remain intact. Their mobile credential is linked only after a
-- successful password login when the old contact phone identifies one user.
ALTER TABLE farmer_users ADD COLUMN mobile_number VARCHAR(10);
ALTER TABLE farmer_users ADD COLUMN email VARCHAR(254);
CREATE UNIQUE INDEX uk_farmer_mobile ON farmer_users(mobile_number);
