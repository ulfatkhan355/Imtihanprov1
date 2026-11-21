-- Backfill deterministic valid 64-char hex SHA-256 checksums for all seeded PDF assets
-- replacing empty strings ('') and placeholder labels ('seed_pending_b2_upload_*').
-- Matches deterministic seed digests in Android MockSeedDataSource until live PDFs are uploaded via Admin.

UPDATE pdf_assets
SET checksum_sha256 = '388c7ab24ee8dd3e074eaf5f2154a7f593a2471bb65ca9cc4faf31c254603bcb',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_001' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = 'ef3c19b7af9974707c9c75ddc17fed60c15e6a15d17ea701f5eddba4c1367e26',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_002' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = 'f3fb589f8d6ea3e25f067081a8b342e0315c6bf4a321464121684faa48fc9f35',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_9_phy_tb' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = 'c487bebaebbad7755fe677c7a6dfde5c2af66105ea2a2816718816f288810154',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_9_math_tb' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = 'b1ed8c223906bb8b1bdde16637ac16c245e1fb682cf7ff2408da46652ed64f00',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_9_phy_pp23' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = 'c8b5a5446c7b69bd8152000382065a8ed93c78f50cbefe4ec2196594be8500e9',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_tb_phy11' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = 'e8325e07e8b35cabe5aaaf24b0a6727d90b572a2039682864af9da9b788bab3a',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_pp_phy11_2025' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');

UPDATE pdf_assets
SET checksum_sha256 = '567e437d2278a0abeb89685f88e6a9aa19f7cebeea48ed33dee4bcd33528d621',
    updated_at_iso = '2026-11-21T00:00:00Z'
WHERE id = 'pdf_pp_math11_2025' AND (checksum_sha256 = '' OR checksum_sha256 LIKE 'seed_pending_%');
