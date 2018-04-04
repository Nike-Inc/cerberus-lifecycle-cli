CREATE EXTERNAL TABLE IF NOT EXISTS @@TABLE_NAME@@ (
  `type` string COMMENT '',
  `time` string COMMENT '',
  `elb` string COMMENT '',
  `client_ip` string COMMENT '',
  `client_port` int COMMENT '',
  `target_ip` string COMMENT '',
  `target_port` int COMMENT '',
  `request_processing_time` double COMMENT '',
  `target_processing_time` double COMMENT '',
  `response_processing_time` double COMMENT '',
  `elb_status_code` string COMMENT '',
  `target_status_code` string COMMENT '',
  `received_bytes` bigint COMMENT '',
  `sent_bytes` bigint COMMENT '',
  `request_verb` string COMMENT '',
  `request_url` string COMMENT '',
  `request_proto` string COMMENT '',
  `user_agent` string COMMENT '',
  `ssl_cipher` string COMMENT '',
  `ssl_protocol` string COMMENT '',
  `target_group_arn` string COMMENT '',
  `trace_id` string COMMENT '',
  `domain_name` string COMMENT '',
  `chosen_cert_arn` string COMMENT '')
PARTITIONED BY (
  `log_time` string)
ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES (
  'input.regex'='([^ ]*) ([^ ]*) ([^ ]*) ([^ ]*):([0-9]*) ([^ ]*)[:-]([0-9]*) ([-.0-9]*) ([-.0-9]*) ([-.0-9]*) (|[-0-9]*) (-|[-0-9]*) ([-0-9]*) ([-0-9]*) \"([^ ]*) ([^ ]*) (- |[^ ]*)\" (\"[^\"]*\") ([A-Z0-9-]+) ([A-Za-z0-9.-]*) ([^ ]*) (.*) (.*) (.*)')
STORED AS INPUTFORMAT
  'org.apache.hadoop.mapred.TextInputFormat'
OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://@@BUCKET_NAME@@/AWSLogs/@@ACCOUNT_ID@@/elasticloadbalancing'