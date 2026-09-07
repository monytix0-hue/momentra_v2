-- V078: Expand collaboration.booking for Hotel/Flight quick-add (stays, segments, places, attachments).

BEGIN;

DO $$
BEGIN
  IF to_regclass('collaboration.booking') IS NULL THEN
    RAISE EXCEPTION 'V078 requires collaboration.booking';
  END IF;
END $$;

-- Booking type enum
ALTER TABLE collaboration.booking
  DROP CONSTRAINT IF EXISTS ck_booking__type;

ALTER TABLE collaboration.booking
  ADD CONSTRAINT ck_booking__type
  CHECK (booking_type IN ('HOTEL','FLIGHT','TRANSPORT','ACTIVITY','RESTAURANT','OTHER'));

-- Participant FKs (composite with moment for safety)
ALTER TABLE collaboration.booking
  ADD COLUMN IF NOT EXISTS booked_by_participant_id UUID,
  ADD COLUMN IF NOT EXISTS paid_by_participant_id UUID,
  ADD COLUMN IF NOT EXISTS linked_expense_id UUID;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_booking__booked_by_participant'
  ) THEN
    ALTER TABLE collaboration.booking
      ADD CONSTRAINT fk_booking__booked_by_participant
      FOREIGN KEY (booked_by_participant_id, moment_id)
      REFERENCES collaboration.moment_participant(participant_id, moment_id)
      ON DELETE RESTRICT;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_booking__paid_by_participant'
  ) THEN
    ALTER TABLE collaboration.booking
      ADD CONSTRAINT fk_booking__paid_by_participant
      FOREIGN KEY (paid_by_participant_id, moment_id)
      REFERENCES collaboration.moment_participant(participant_id, moment_id)
      ON DELETE RESTRICT;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_booking__linked_expense'
  ) THEN
    ALTER TABLE collaboration.booking
      ADD CONSTRAINT fk_booking__linked_expense
      FOREIGN KEY (linked_expense_id)
      REFERENCES finance.expense(expense_id)
      ON DELETE SET NULL;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_booking__linked_expense
  ON collaboration.booking (linked_expense_id)
  WHERE linked_expense_id IS NOT NULL;

-- Hotel stays
CREATE TABLE IF NOT EXISTS collaboration.booking_stay (
  stay_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id UUID NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  hotel_name TEXT NOT NULL,
  reference_code TEXT,
  amount NUMERIC(19,4),
  currency_code CHAR(3),
  start_at TIMESTAMPTZ,
  end_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_booking_stay__booking
    FOREIGN KEY (booking_id) REFERENCES collaboration.booking(booking_id) ON DELETE CASCADE,
  CONSTRAINT ck_booking_stay__amount CHECK (amount IS NULL OR amount >= 0),
  CONSTRAINT ck_booking_stay__currency CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$'),
  CONSTRAINT ck_booking_stay__time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
);

CREATE INDEX IF NOT EXISTS ix_booking_stay__booking_sort
  ON collaboration.booking_stay (booking_id, sort_order);

-- Flight segments
CREATE TABLE IF NOT EXISTS collaboration.booking_flight_segment (
  segment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id UUID NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  leg_label TEXT NOT NULL DEFAULT 'OUTBOUND',
  airline TEXT,
  flight_number TEXT,
  origin_code TEXT,
  destination_code TEXT,
  seat_class TEXT,
  seat_number TEXT,
  depart_at TIMESTAMPTZ,
  arrive_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_booking_flight_segment__booking
    FOREIGN KEY (booking_id) REFERENCES collaboration.booking(booking_id) ON DELETE CASCADE,
  CONSTRAINT ck_booking_flight_segment__leg
    CHECK (leg_label IN ('OUTBOUND','RETURN','CONNECTING','OTHER')),
  CONSTRAINT ck_booking_flight_segment__time
    CHECK (arrive_at IS NULL OR depart_at IS NULL OR arrive_at >= depart_at)
);

CREATE INDEX IF NOT EXISTS ix_booking_flight_segment__booking_sort
  ON collaboration.booking_flight_segment (booking_id, sort_order);

-- Places linked to booking
CREATE TABLE IF NOT EXISTS collaboration.booking_place (
  booking_id UUID NOT NULL,
  place_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (booking_id, place_id),
  CONSTRAINT fk_booking_place__booking
    FOREIGN KEY (booking_id) REFERENCES collaboration.booking(booking_id) ON DELETE CASCADE,
  CONSTRAINT fk_booking_place__place
    FOREIGN KEY (place_id) REFERENCES collaboration.shared_experience_place(place_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_booking_place__place
  ON collaboration.booking_place (place_id);

-- Document / media attachments
CREATE TABLE IF NOT EXISTS collaboration.booking_attachment (
  booking_id UUID NOT NULL,
  upload_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (booking_id, upload_id),
  CONSTRAINT fk_booking_attachment__booking
    FOREIGN KEY (booking_id) REFERENCES collaboration.booking(booking_id) ON DELETE CASCADE,
  CONSTRAINT fk_booking_attachment__upload
    FOREIGN KEY (upload_id) REFERENCES platform.media_upload(media_upload_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_booking_attachment__upload
  ON collaboration.booking_attachment (upload_id);

-- Grants
GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.booking_stay TO momentra_app;
GRANT SELECT ON collaboration.booking_stay TO momentra_projection_worker, momentra_analytics_worker, momentra_memory_worker;

GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.booking_flight_segment TO momentra_app;
GRANT SELECT ON collaboration.booking_flight_segment TO momentra_projection_worker, momentra_analytics_worker, momentra_memory_worker;

GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.booking_place TO momentra_app;
GRANT SELECT ON collaboration.booking_place TO momentra_projection_worker, momentra_analytics_worker, momentra_memory_worker;

GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.booking_attachment TO momentra_app;
GRANT SELECT ON collaboration.booking_attachment TO momentra_projection_worker, momentra_analytics_worker, momentra_memory_worker;

-- RLS
ALTER TABLE collaboration.booking_stay ENABLE ROW LEVEL SECURITY;
ALTER TABLE collaboration.booking_flight_segment ENABLE ROW LEVEL SECURITY;
ALTER TABLE collaboration.booking_place ENABLE ROW LEVEL SECURITY;
ALTER TABLE collaboration.booking_attachment ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_booking_stay__select_participant ON collaboration.booking_stay
FOR SELECT USING (
  (EXISTS (
    SELECT 1 FROM collaboration.booking b
    WHERE b.booking_id = booking_stay.booking_id
      AND security.is_active_group_participant(b.moment_id)
  ))
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_booking_stay__backend_write ON collaboration.booking_stay
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_booking_flight_segment__select_participant ON collaboration.booking_flight_segment
FOR SELECT USING (
  (EXISTS (
    SELECT 1 FROM collaboration.booking b
    WHERE b.booking_id = booking_flight_segment.booking_id
      AND security.is_active_group_participant(b.moment_id)
  ))
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_booking_flight_segment__backend_write ON collaboration.booking_flight_segment
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_booking_place__select_participant ON collaboration.booking_place
FOR SELECT USING (
  (EXISTS (
    SELECT 1 FROM collaboration.booking b
    WHERE b.booking_id = booking_place.booking_id
      AND security.is_active_group_participant(b.moment_id)
  ))
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_booking_place__backend_write ON collaboration.booking_place
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_booking_attachment__select_participant ON collaboration.booking_attachment
FOR SELECT USING (
  (EXISTS (
    SELECT 1 FROM collaboration.booking b
    WHERE b.booking_id = booking_attachment.booking_id
      AND security.is_active_group_participant(b.moment_id)
  ))
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
CREATE POLICY rls_booking_attachment__backend_write ON collaboration.booking_attachment
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMENT ON TABLE collaboration.booking_stay IS 'Hotel stay legs for a booking (multi-stay).';
COMMENT ON TABLE collaboration.booking_flight_segment IS 'Flight segments for a booking (outbound/return/connecting).';
COMMENT ON TABLE collaboration.booking_place IS 'Shared Experience places tagged on a booking.';
COMMENT ON TABLE collaboration.booking_attachment IS 'Media uploads (tickets/confirmations) linked to a booking.';
COMMENT ON COLUMN collaboration.booking.linked_expense_id IS 'Optional EQUAL-split group expense created with the booking.';

COMMIT;
