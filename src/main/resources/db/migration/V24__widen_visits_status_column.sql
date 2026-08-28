-- VARCHAR(20) was too narrow for VisitStatus.AWAITING_DOCTOR_CONFIRMATION (28 chars),
-- causing every employee-confirm to fail with "value too long for type character varying(20)".
ALTER TABLE visits ALTER COLUMN status TYPE VARCHAR(40);
