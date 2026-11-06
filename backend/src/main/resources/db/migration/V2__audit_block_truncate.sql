-- Row triggers cannot intercept TRUNCATE; protect that path separately.
create trigger audit_events_no_truncate
    before truncate on audit_events
    for each statement execute function audit_events_block_mutation();
