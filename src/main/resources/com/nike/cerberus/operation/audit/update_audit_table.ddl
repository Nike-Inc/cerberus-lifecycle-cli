CREATE EXTERNAL TABLE IF NOT EXISTS @@TABLE_NAME@@ (
  event_timestamp TIMESTAMP,
  principal_name string,
  principal_type string,
  principal_token_created TIMESTAMP,
  principal_token_expires TIMESTAMP,
  principal_is_admin string,
  ip_address string,
  x_forwarded_for string,
  client_version string,
  http_method string,
  path string,
  action string,
  was_success string,
  name string,
  sdb_name_slug string,
  originating_class string,
  trace_id string,
  status_code string,
  cerberus_version string
) PARTITIONED BY (year INT, month INT, day INT, hour INT)
ROW FORMAT serde 'org.apache.hive.hcatalog.data.JsonSerDe'
with serdeproperties (
    "ignore.malformed.json"="true"
)
LOCATION 's3://@@BUCKET_NAME@@/audit-logs/partitioned/';
