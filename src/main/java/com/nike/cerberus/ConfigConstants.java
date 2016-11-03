package com.nike.cerberus;

public class ConfigConstants {

    public static final String ENV_PREFIX = "cerberus-";

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final int MINIMUM_AZS = 3;

    public static final String CONFIG_BUCKET_KEY = "cerberusconfigbucket";

    public static final String DEFAULT_CMS_DB_NAME = "cms";

    public static final String ENV_DATA_FILE = "config/environment.json";

    public static final String CF_LOG_PROCESSOR_LAMBDA_CONFIG_FILE = "data/cloud-front-log-processor/lambda-config.json";

    public static final String SECRETS_DATA_FILE = "config/secrets.json";

    public static final String CERT_PART_CA = "ca.pem";

    public static final String CERT_PART_CERT = "cert.pem";

    public static final String CERT_PART_KEY = "key.pem";

    public static final String CERT_PART_PUBKEY = "pubkey.pem";

    public static final String BASE_STACK_NAME = "base";

    public static final String BASE_STACK_TEMPLATE_PATH = "/cloudformation/vpc-and-base.json";

    public static final String CONSUL_DATACENTER = "cerberus";

    public static final String CONSUL_SERVER_CONFIG_FILE = "data/consul/consul-server-config.json";

    public static final String CONSUL_CLIENT_CONFIG_FILE = "data/consul/consul-client-config.json";

    public static final String CONSUL_STACK_TEMPLATE_PATH = "/cloudformation/consul-cluster.json";

    public static final String VAULT_ACL_ENTRY_FILE = "data/consul/vault-acl.json";

    public static final String VAULT_CONFIG_FILE = "data/vault/vault-config.json";

    public static final int VAULT_SECRET_SHARES = 5;

    public static final int VAULT_SECRET_THRESHOLD = 3;

    public static final String VAULT_STACK_TEMPLATE_PATH = "/cloudformation/vault-cluster.json";

    public static final String CMS_STACK_TEMPLATE_PATH = "/cloudformation/cms-cluster.json";

    public static final String GATEWAY_STACK_TEMPLATE_PATH = "/cloudformation/gateway-cluster.json";

    public static final String GATEWAY_SITE_CONFIG_FILE = "data/gateway/gateway.conf";

    public static final String GATEWAY_GLOBAL_CONFIG_FILE = "data/gateway/nginx.conf";

    public static final String CMS_ENV_CONFIG_PATH = "data/cms/environment.properties";

    public static final String RDSBACKUP_STACK_TEMPLATE_PATH = "/cloudformation/rds-backup-cluster.json";

    public static final String CF_ELB_IP_SYNC_STACK_TEMPLATE_PATH = "/cloudformation/cloudfront-elb-security-group-updater-lambda.json";
}