-- US-AUD-20: optional statistical sampling for a large-scope audit.
-- All three columns are nullable; NULL means a full 100% verification audit -
-- sampling is opt-in and is never silently assumed (AC-AUD-20).
ALTER TABLE audit
    ADD COLUMN sampling_confidence_level INTEGER,
    ADD COLUMN sampling_margin_of_error  NUMERIC(4, 1),
    ADD COLUMN sampling_population_size   INTEGER;
