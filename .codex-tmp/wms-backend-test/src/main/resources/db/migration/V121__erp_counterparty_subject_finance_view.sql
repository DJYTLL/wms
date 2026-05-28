CREATE OR REPLACE VIEW erp_counterparty_subject_finance_summary_v AS
WITH customer_links AS (
    SELECT l.tenant_id,
           l.subject_id,
           l.target_id AS customer_id
    FROM erp_counterparty_subject_link l
    WHERE l.target_type = 'CUSTOMER'
      AND l.role_type = 'CUSTOMER'
),
supplier_links AS (
    SELECT l.tenant_id,
           l.subject_id,
           l.target_id AS supplier_id
    FROM erp_counterparty_subject_link l
    WHERE l.target_type = 'SUPPLIER'
      AND l.role_type = 'SUPPLIER'
),
receivable_totals AS (
    SELECT cl.tenant_id,
           cl.subject_id,
           COALESCE(SUM(ar.unpaid_amount), 0) AS receivable_total
    FROM customer_links cl
    LEFT JOIN erp_accounts_receivable ar
      ON ar.tenant_id = cl.tenant_id
     AND ar.customer_id = cl.customer_id
     AND ar.status <> 'RED_FLUSHED'
     AND ar.deleted_at IS NULL
    GROUP BY cl.tenant_id, cl.subject_id
),
payable_totals AS (
    SELECT sl.tenant_id,
           sl.subject_id,
           COALESCE(SUM(ap.unpaid_amount), 0) AS payable_total
    FROM supplier_links sl
    LEFT JOIN erp_accounts_payable ap
      ON ap.tenant_id = sl.tenant_id
     AND ap.supplier_id = sl.supplier_id
     AND ap.status <> 'RED_FLUSHED'
     AND ap.deleted_at IS NULL
    GROUP BY sl.tenant_id, sl.subject_id
),
customer_counts AS (
    SELECT tenant_id,
           subject_id,
           COUNT(DISTINCT customer_id) AS customer_count
    FROM customer_links
    GROUP BY tenant_id, subject_id
),
supplier_counts AS (
    SELECT tenant_id,
           subject_id,
           COUNT(DISTINCT supplier_id) AS supplier_count
    FROM supplier_links
    GROUP BY tenant_id, subject_id
)
SELECT s.tenant_id,
       s.id AS subject_id,
       s.name AS subject_name,
       COALESCE(rt.receivable_total, 0) AS receivable_total,
       COALESCE(pt.payable_total, 0) AS payable_total,
       COALESCE(rt.receivable_total, 0) - COALESCE(pt.payable_total, 0) AS net_amount,
       COALESCE(cc.customer_count, 0) AS customer_count,
       COALESCE(sc.supplier_count, 0) AS supplier_count
FROM erp_counterparty_subject s
LEFT JOIN receivable_totals rt
  ON rt.tenant_id = s.tenant_id
 AND rt.subject_id = s.id
LEFT JOIN payable_totals pt
  ON pt.tenant_id = s.tenant_id
 AND pt.subject_id = s.id
LEFT JOIN customer_counts cc
  ON cc.tenant_id = s.tenant_id
 AND cc.subject_id = s.id
LEFT JOIN supplier_counts sc
  ON sc.tenant_id = s.tenant_id
 AND sc.subject_id = s.id
WHERE s.deleted_at IS NULL;
