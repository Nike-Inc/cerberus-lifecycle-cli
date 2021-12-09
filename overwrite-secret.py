import boto3
import base64
import argparse
import time
import logging
from botocore.exceptions import ClientError
from pprint import pprint

logging.basicConfig(level=logging.INFO)
logging.Formatter("%(asctime)s;%(levelname)s;%(message)s", "%Y-%m-%d %H:%M:%S")
logger = logging.getLogger()

def arg_parser():
    parser = argparse.ArgumentParser(description='Delete and replace Cerberus certificate in secrets manager')
    parser.add_argument('--region', '-r', type=str, required=True,
                        help="Secrets Manager region")
    parser.add_argument('--cert', '-c', type=str, required=True,
                        help="Certificate file to upload")
    parser.add_argument('--secret-name', '-s', type=str, required=True,
                        help="Name of Secret store to look for in AWS Secrets Manager")
    parser.add_argument('--path', '-p', type=str, required=True, default="certs/")
    return parser

# Uploads the SSL Cert
def upload_secret(client, secret_name, cert_name, path):
    cert_path = path + cert_name
    secret_binary = open(cert_path, 'rb').read()

    client.create_secret(Name=secret_name, Description="ssl cert for cerberus",
                         SecretBinary=secret_binary)

# Deletes the SSL Cert
def delete_secret(client, secret_name):
    logger.info(f'Beginning secret deletion for {secret_name}...')

    response = client.delete_secret(
        SecretId=secret_name,
        ForceDeleteWithoutRecovery=True
    )
    pprint(response)
    logger.info(f'Finished deleting secrets!')


# Checks if the old cert still exists
def does_secret_exist(client, secret_name):
    try:
        logger.info(f"Checking if {secret_name} exists...")
        get_secret_value_response = client.get_secret_value(
            SecretId=secret_name
        )
    except ClientError as e:
        if e.response['Error']['Code'] == 'ResourceNotFoundException':
            logger.info(f"{secret_name} does not exist!")
            return False
        elif e.response['Error']['Code'] == 'InvalidRequestException':
            logger.info(f"{secret_name} is scheduled for deletion exists!")
            return True
        else:
            logger.info(f"{secret_name} does not exist!")
            return True

def main():
    parser = arg_parser()
    args = parser.parse_args()

    region = args.region
    cert_name = args.cert
    path = args.path
    secret_name = args.secret_name

    session = boto3.session.Session()
    client = session.client(
        service_name='secretsmanager',
        region_name=region
    )

    logger.info(f'Looking for secret: {secret_name} in {region}!')
    delete_secret(client, secret_name)

    logger.info(f'Waiting for secret to finish deleting...')
    while does_secret_exist(client, secret_name):
        logger.info("Sleeping for 30 seconds")
        time.sleep(30)

    upload_secret(client, secret_name, cert_name, path)
    logger.info(f"Uploaded {cert_name} with name {secret_name}")

if __name__ == "__main__":
	main()
