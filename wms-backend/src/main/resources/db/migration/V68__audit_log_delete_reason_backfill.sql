UPDATE app_audit_log
SET delete_reason = COALESCE(
        NULLIF(BTRIM((regexp_match(detail, '(?:^|\s\|\s)deleteReason=([^|]+)'))[1]), ''),
        delete_reason
    ),
    detail = NULLIF(
        BTRIM(
            regexp_replace(detail, '(?:^|\s\|\s)deleteReason=[^|]+', '', 'g'),
            ' |'
        ),
        ''
    )
WHERE detail LIKE '%deleteReason=%';
