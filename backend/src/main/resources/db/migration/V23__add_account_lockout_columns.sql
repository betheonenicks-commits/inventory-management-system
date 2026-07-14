-- US-SEC-09: lock an account after 5 consecutive failed logins. failed_login_count
-- resets to 0 on any successful login; locked_until is null unless currently
-- locked. No separate "is locked" boolean - locked_until being in the future
-- IS the locked state, so there's only one fact to keep consistent, not two.
ALTER TABLE app_user
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ;
