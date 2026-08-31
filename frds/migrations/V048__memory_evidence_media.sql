BEGIN;

-- Allow MEDIA uploads as memory evidence (group Capture Memory attach).
ALTER TABLE memory.memory_evidence
  DROP CONSTRAINT IF EXISTS ck_memory_evidence__source;

ALTER TABLE memory.memory_evidence
  ADD CONSTRAINT ck_memory_evidence__source CHECK (
    source_type IN (
      'MOMENT',
      'EXPENSE',
      'TASK',
      'DECISION',
      'METRIC_OBSERVATION',
      'EVENT',
      'LIFE_OBSERVATION',
      'RELATIONSHIP_ACTIVITY',
      'BOOKING',
      'MEDIA',
      'OTHER'
    )
  );

COMMIT;
