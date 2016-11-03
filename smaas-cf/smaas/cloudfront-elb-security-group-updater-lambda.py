from io import open
from troposphere import Ref, Template, GetAtt, AWS_ACCOUNT_ID, Parameter, Output
from troposphere.iam import Role, Policy
from troposphere.awslambda import Function, Code, Permission

CF_FILE = '../../src/main/resources/cloudformation/cloudfront-elb-security-group-updater-lambda.json'
CLOUD_FRONT_PREFIX = 'cf-logs/'

# Create Template
template = Template()
template.description = "Launches the gateway cluster in the Cerberus VPC"
template.version = '2010-09-09'

###
#
# Inputs
#
###

lambda_bucket = template.add_parameter(Parameter(
    "lambdaBucket",
    Type="String",
    Description="S3 Bucket for lambda function artifact"
))

lambda_key = template.add_parameter(Parameter(
    "lambdaKey",
    Type="String",
    Description="Key for lambda function artifact"
))


###
#
# IAM roles
#
###

cloud_front_origin_elb_sg_ip_sync_lambda_iam_role = template.add_resource(Role(
    "CloudFrontOriginElbSgIpSyncLambdaIamRole",
    AssumeRolePolicyDocument={
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {
                "Service": ["lambda.amazonaws.com"]
            },
            "Action": ["sts:AssumeRole"]
        }]
    },
    Policies=[
        Policy(
            PolicyName="cloud_front_origin_elb_sg_ip_sync_lambda_iam_role_policy",
            PolicyDocument={
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Action": [
                            "logs:CreateLogGroup",
                            "logs:CreateLogStream",
                            "logs:PutLogEvents"
                        ],
                        "Resource": "arn:aws:logs:*:*:*"
                    },
                    {
                        "Effect": "Allow",
                        "Action": [
                            "ec2:DescribeSecurityGroups",
                            "ec2:AuthorizeSecurityGroupIngress",
                            "ec2:RevokeSecurityGroupIngress"
                        ],
                        "Resource": "*"
                    }
                ]
            }
        )
    ],
    Path="/"
))

###
#
# Lambda Function
#
###

cloud_front_origin_elb_sg_ip_sync_function = template.add_resource(Function(
    "CloudFrontOriginElbSgIpSyncFunction",
    Description="Lambda for syncing AWSs IPs for Cloud Front to origin ELB SGs",
    Handler="update_security_groups.lambda_handler",
    Role=GetAtt(cloud_front_origin_elb_sg_ip_sync_lambda_iam_role, "Arn"),
    Code=Code(
        S3Bucket=Ref(lambda_bucket),
        S3Key=Ref(lambda_key)
    ),
    Runtime="python2.7",
    MemorySize="128",
    Timeout="5"
))

lambda_invoke_permission = template.add_resource(Permission(
    "LambdaInvokePermission",
    DependsOn="CloudFrontOriginElbSgIpSyncFunction",
    FunctionName=GetAtt(cloud_front_origin_elb_sg_ip_sync_function, "Arn"),
    Action="lambda:*",
    Principal="sns.amazonaws.com",
    SourceAccount=Ref(AWS_ACCOUNT_ID)
))

###
#
# Outputs
#
###

template.add_output(Output(
    "cloudFrontOriginElbSgIpSyncFunctionArn",
    Value=GetAtt(cloud_front_origin_elb_sg_ip_sync_function, "Arn"),
))

###
#
# Print!
#
###

cf_file = open(CF_FILE, 'w')
cf_file.truncate()
cf_file.write(template.to_json(indent=0))
cf_file.close()

print("CloudFormation template written to: {0}".format(CF_FILE))
