###
# Copyright (c) 2017 Nike Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###

from io import open
from troposphere import Ref, Template, Parameter, Output, GetAtt, Join, AWS_ACCOUNT_ID
from troposphere.autoscaling import AutoScalingGroup, LaunchConfiguration
from troposphere.cloudfront import Distribution, DefaultCacheBehavior, ForwardedValues, DistributionConfig, Origin, \
    CustomOrigin, ViewerCertificate, Logging
from troposphere.elasticloadbalancing import LoadBalancer, ConnectionDrainingPolicy, ConnectionSettings, HealthCheck, \
    Listener, Policy
from troposphere.policies import UpdatePolicy, AutoScalingRollingUpdate
from troposphere.route53 import RecordSetType
from troposphere.waf import WebACL, Action, Rule, SizeConstraintSet, SizeConstraint, FieldToMatch, Predicates, Rules, \
    IPSet, SqlInjectionMatchSet, SqlInjectionMatchTuples, XssMatchSet, XssMatchTuple
from troposphere.s3 import Bucket, BucketOwnerFullControl, NotificationConfiguration, LambdaConfigurations, Filter, \
    S3Key, Rules as S3Rules, BucketPolicy
from troposphere.awslambda import Function, Code, Permission

from smaas.support.tags import CerberusTags

CF_FILE = '../../src/main/resources/cloudformation/gateway-cluster.json'
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

cerberus_tags = CerberusTags()
cerberus_tags.add_tag_parameters(template)

cert_public_key_param = template.add_parameter(Parameter(
    "certPublicKey",
    Description="TLS certificate public key to be used for backend authentication",
    Type="String"
))

ssl_certificate_arn_param = template.add_parameter(Parameter(
    "sslCertificateArn",
    Description="TLS certificate ARN for the ELB",
    Type="String"
))

ssl_certificate_id_param = template.add_parameter(Parameter(
    "sslCertificateId",
    Description="TLS certificate ID for the CloudFront distribution and ELB",
    Type="String"
))

vpc_id_param = template.add_parameter(Parameter(
    "vpcId",
    Description="The ID of the VPC",
    Type="AWS::EC2::VPC::Id"
))

instance_profile_name_param = template.add_parameter(Parameter(
    "instanceProfileName",
    Description="The name for the instance profile",
    Type="String"
))

ami_id_param = template.add_parameter(Parameter(
    "amiId",
    Description="The AMI ID for the instances",
    Type="String"
))

instance_size_param = template.add_parameter(Parameter(
    "instanceSize",
    Description="The instance size",
    Type="String"
))

gateway_elb_sg_id_param = template.add_parameter(Parameter(
    "gatewayElbSgId",
    Description="Gateway Server ELB Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

gateway_server_sg_id_param = template.add_parameter(Parameter(
    "gatewayServerSgId",
    Description="Gateway Server Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

tools_ingress_sg_id_param = template.add_parameter(Parameter(
    "toolsIngressSgId",
    Description="Tools Ingress Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

cloud_front_log_processor_lambda_iam_role_param = template.add_parameter(Parameter(
    "cloudFrontLogProcessorLambdaIamRoleArn",
    Description="The IAM Role Arn for the lambda",
    Type="String"
))

subnet_id_refs = []
for zone_identifier in range(1, 4):
    vpc_subnet_id = template.add_parameter(Parameter(
        "vpcSubnetIdForAz{0}".format(zone_identifier),
        Description="VPC Subnet ID for Zone {0}".format(zone_identifier),
        Type="String"
    ))

    subnet_id_refs.append(Ref(vpc_subnet_id))

key_pair_name_param = template.add_parameter(Parameter(
    "keyPairName",
    Description="The key pair to be associated with the EC2 instances",
    Type="String",
    Default="cpe-cerberus"
))

gateway_user_data_param = template.add_parameter(Parameter(
    "userData",
    Description="Gateway User Data",
    Type="String"
))

hosted_zone_id_param = template.add_parameter(Parameter(
    "hostedZoneId",
    Description="The hosted zone id to associate the CNAME record with",
    Type="String"
))

hostname_param = template.add_parameter(Parameter(
    "hostname",
    Description="The base hostname for the public facing gateway / router",
    Type="String"
))

waf_lambda_bucket = template.add_parameter(Parameter(
    "wafLambdaBucket",
    Type="String",
    Description="S3 Bucket for waf lambda function artifact"
))

waf_lambda_key = template.add_parameter(Parameter(
    "wafLambdaKey",
    Type="String",
    Description="Key for waf lambda function artifact"
))

desired_instances_param = template.add_parameter(Parameter(
    "desiredInstances",
    Description="Desired Number of Auto Scaling Instances",
    Type="Number"
))

maximum_instances_param = template.add_parameter(Parameter(
    "maximumInstances",
    Description="Maximum Number of Auto Scaling Instances",
    Type="Number"
))

minimum_instances_param = template.add_parameter(Parameter(
    "minimumInstances",
    Description="Minimum Number of Auto Scaling Instances",
    Type="Number"
))

###
#
# Elastic Load Balancer
#
###

gateway_load_balancer = template.add_resource(LoadBalancer(
    "GatewayElasticLoadBalancer",
    ConnectionDrainingPolicy=ConnectionDrainingPolicy(
        Enabled=True,
        Timeout=10
    ),
    ConnectionSettings=ConnectionSettings(
        IdleTimeout=10
    ),
    CrossZone=True,
    HealthCheck=HealthCheck(
        HealthyThreshold=2,
        Interval=5,
        Target="HTTPS:443/sys/health",
        Timeout=2,
        UnhealthyThreshold=2
    ),
    Listeners=[
        Listener(
            InstancePort=443,
            InstanceProtocol="HTTPS",
            LoadBalancerPort=443,
            PolicyNames=[
                "GatewayTLSNegotiationPolicy"
            ],
            Protocol="HTTPS",
            SSLCertificateId=Ref(ssl_certificate_arn_param)
        )
    ],
    Policies=[
        Policy(
            PolicyName="GatewayTLSNegotiationPolicy",
            PolicyType="SSLNegotiationPolicyType",
            Attributes=[
                {
                    "Name": "Reference-Security-Policy",
                    "Value": "ELBSecurityPolicy-2015-05"
                }
            ]
        ),
        Policy(
            PolicyName="GatewayPublicKeyPolicy",
            PolicyType="PublicKeyPolicyType",
            Attributes=[
                {
                    "Name": "PublicKey",
                    "Value": Ref(cert_public_key_param)
                }
            ]
        ),
        Policy(
            PolicyName="GatewayBackendServerAuthenticationPolicy",
            PolicyType="BackendServerAuthenticationPolicyType",
            Attributes=[
                {
                    "Name": "PublicKeyPolicyName",
                    "Value": "GatewayPublicKeyPolicy"
                }
            ],
            InstancePorts=[
                "443"
            ]
        )
    ],
    Scheme="internet-facing",
    SecurityGroups=[
        Ref(gateway_elb_sg_id_param)
    ],
    Subnets=subnet_id_refs,
    Tags=cerberus_tags.get_tags_as_list()
))

###
#
# Launch Configuration and Auto Scaling Group for Gateway
#
###

gateway_launch_config = template.add_resource(LaunchConfiguration(
    "GatewayLaunchConfiguration",
    AssociatePublicIpAddress=True,
    IamInstanceProfile=Ref(instance_profile_name_param),
    ImageId=Ref(ami_id_param),
    InstanceMonitoring=True,
    InstanceType=Ref(instance_size_param),
    KeyName=Ref(key_pair_name_param),
    SecurityGroups=[
        Ref(tools_ingress_sg_id_param),
        Ref(gateway_server_sg_id_param),
    ],
    UserData=Ref(gateway_user_data_param)
))

gateway_autoscaling_group = template.add_resource(AutoScalingGroup(
    "GatewayAutoScalingGroup",
    DesiredCapacity=Ref(desired_instances_param),
    HealthCheckGracePeriod=300,
    HealthCheckType="ELB",
    LaunchConfigurationName=Ref(gateway_launch_config),
    LoadBalancerNames=[
        Ref(gateway_load_balancer)
    ],
    MaxSize=Ref(maximum_instances_param),
    MinSize=Ref(minimum_instances_param),
    UpdatePolicy=UpdatePolicy(
        AutoScalingRollingUpdate=AutoScalingRollingUpdate(
            MaxBatchSize=1,
            MinInstancesInService=2,
            PauseTime="PT15M",
            WaitOnResourceSignals=True
        )
    ),
    VPCZoneIdentifier=subnet_id_refs,
    Tags=cerberus_tags.get_autoscaling_tags_as_list()
))

###
#
# Record Set for public Cerberus CNAME
#
###

gateway_record_set = template.add_resource(RecordSetType(
    "OriginCerberusPublicRecordSet",
    HostedZoneId=Ref(hosted_zone_id_param),
    Name=Join(".", ["origin", Ref(hostname_param), ""]),
    TTL=30,
    Type="CNAME",
    ResourceRecords=[GetAtt(gateway_load_balancer, "CanonicalHostedZoneName")]
))

###
#
# WAF Web ACL
#
###

xss_match_set = template.add_resource(XssMatchSet(
    "CerberusWafXssMatchSet",
    Name="CerberusWafXssMatchSet",
    XssMatchTuples=[
        XssMatchTuple(
            "CerberusWafXssUriMatch",
            FieldToMatch=FieldToMatch(
                Type="URI"
            ),
            TextTransformation="NONE"
        ),
        XssMatchTuple(
            "CerberusWafXssQueryStringMatch",
            FieldToMatch=FieldToMatch(
                Type="QUERY_STRING"
            ),
            TextTransformation="NONE"
        ),
        XssMatchTuple(
            "CerberusWafXssBodyMatch",
            FieldToMatch=FieldToMatch(
                Type="BODY"
            ),
            TextTransformation="NONE"
        )
    ]
))

xss_rule = template.add_resource(Rule(
    "CerberusWafXssRule",
    Name="CerberusWafXssRule",
    MetricName="CerberusWafXss",
    Predicates=[
        Predicates(
            DataId=Ref(xss_match_set),
            Negated=False,
            Type="XssMatch"
        )
    ]
))

sql_injection_match_set = template.add_resource(SqlInjectionMatchSet(
    "CerberusWafSqlInjectionMatchSet",
    Name="CerberusWafSqlInjectionMatchSet",
    SqlInjectionMatchTuples=[
        SqlInjectionMatchTuples(
            "CerberusWafSqlInjectionUriMatch",
            FieldToMatch=FieldToMatch(
                Type="URI"
            ),
            TextTransformation="NONE"
        ),
        SqlInjectionMatchTuples(
            "CerberusWafSqlInjectionQueryStringMatch",
            FieldToMatch=FieldToMatch(
                Type="QUERY_STRING"
            ),
            TextTransformation="NONE"
        ),
        SqlInjectionMatchTuples(
            "CerberusWafSqlInjectionBodyMatch",
            FieldToMatch=FieldToMatch(
                Type="BODY"
            ),
            TextTransformation="NONE"
        )
    ]
))

sql_injection_rule = template.add_resource(Rule(
    "CerberusWafSqlInjectionRule",
    Name="CerberusWafSqlInjectionRule",
    MetricName="CerberusWafSqlInjection",
    Predicates=[
        Predicates(
            DataId=Ref(sql_injection_match_set),
            Negated=False,
            Type="SqlInjectionMatch"
        )
    ]
))

size_constraint_set = template.add_resource(SizeConstraintSet(
    "CerberusWafSizeConstraintSet",
    Name="CerberusWafSizeConstraintSet",
    SizeConstraints=[
        SizeConstraint(
            "CerberusWafSizeConstraint",
            ComparisonOperator="GE",
            FieldToMatch=FieldToMatch(
                Type="BODY"
            ),
            Size=256000,
            TextTransformation="NONE"
        )
    ]
))

size_constraint_rule = template.add_resource(Rule(
    "CerberusWafSizeConstraintRule",
    Name="CerberusWafSizeConstraintRule",
    MetricName="CerberusWafSizeConstraint",
    Predicates=[
        Predicates(
            DataId=Ref(size_constraint_set),
            Negated=False,
            Type="SizeConstraint"
        )
    ]
))

waf_white_list_set = template.add_resource(IPSet(
    "WAFWhiteListSet",
    Name="White List Set"
))

waf_manual_block_set = template.add_resource(IPSet(
    "WAFManualBlockSet",
    Name="Manual Block Set"
))

waf_auto_block_set = template.add_resource(IPSet(
    "WAFAutoBlockSet",
    Name="Auto Block Set"
))

waf_white_list_rule = template.add_resource(Rule(
    "WAFWhiteListRule",
    DependsOn="WAFWhiteListSet",
    Name="White List Rule",
    MetricName="WhiteListRule",
    Predicates=[
        Predicates(
            DataId=Ref(waf_white_list_set),
            Negated=False,
            Type="IPMatch"
        )
    ]
))

waf_manual_block_rule = template.add_resource(Rule(
    "WAFManualBlockRule",
    DependsOn="WAFManualBlockSet",
    Name="Manual Block Rule",
    MetricName="ManualBlockRule",
    Predicates=[
        Predicates(
            DataId=Ref(waf_manual_block_set),
            Negated=False,
            Type="IPMatch"
        )
    ]
))

waf_auto_block_rule = template.add_resource(Rule(
    "WAFAutoBlockRule",
    DependsOn="WAFAutoBlockSet",
    Name="Auto Block Rule",
    MetricName="AutoBlockRule",
    Predicates=[
        Predicates(
            DataId=Ref(waf_auto_block_set),
            Negated=False,
            Type="IPMatch"
        )
    ]
))

web_acl = template.add_resource(WebACL(
    "CerberusWAFWebAcl",
    DependsOn=["WAFManualBlockRule", "WAFAutoBlockRule"],
    MetricName="CerberusWAF",
    DefaultAction=Action(
        Type="ALLOW"
    ),
    Name=Join(".", ["waf", Ref(hostname_param)]),
    Rules=[
        Rules(
            Action=Action(
                Type="BLOCK"
            ),
            Priority=1,
            RuleId=Ref(size_constraint_rule)
        ),
        Rules(
            Action=Action(
                Type="BLOCK"
            ),
            Priority=2,
            RuleId=Ref(sql_injection_rule)
        ),
        Rules(
            Action=Action(
                Type="BLOCK"
            ),
            Priority=3,
            RuleId=Ref(xss_rule)
        ),
        Rules(
            Action=Action(
                Type="ALLOW"
            ),
            Priority=4,
            RuleId=Ref(waf_white_list_rule)
        ),
        Rules(
            Action=Action(
                Type="BLOCK"
            ),
            Priority=5,
            RuleId=Ref(waf_manual_block_rule)
        ),
        Rules(
            Action=Action(
                Type="BLOCK"
            ),
            Priority=6,
            RuleId=Ref(waf_auto_block_rule)
        )
    ]
))

###
#
# WAF Lambda Function
#
###

lambda_waf_blacklisting_function = template.add_resource(Function(
    "LambdaWAFBlacklistingFunction",
    Description="Function for auto black listing ips that are misbehaving",
    Handler="com.nike.cerberus.lambda.waf.handler.CloudFrontLogEventHandler::handleNewS3Event",
    Role=Ref(cloud_front_log_processor_lambda_iam_role_param),
    Code=Code(
        S3Bucket=Ref(waf_lambda_bucket),
        S3Key=Ref(waf_lambda_key)
    ),
    Runtime="java8",
    MemorySize="512",
    Timeout="60"
))

lambda_invoke_permission = template.add_resource(Permission(
    "LambdaInvokePermission",
    DependsOn="LambdaWAFBlacklistingFunction",
    FunctionName=GetAtt(lambda_waf_blacklisting_function, "Arn"),
    Action="lambda:*",
    Principal="s3.amazonaws.com",
    SourceAccount=Ref(AWS_ACCOUNT_ID)
))

###
#
# Bucket for Cloud Front Logs
#
###

cloud_front_logs_bucket = template.add_resource(Bucket(
    "CloudFrontBucket",
    AccessControl=BucketOwnerFullControl,
    Tags=cerberus_tags.get_tags(),
    NotificationConfiguration=NotificationConfiguration(
        LambdaConfigurations=[LambdaConfigurations(
            Event="s3:ObjectCreated:*",
            Filter=Filter(
                S3Key=S3Key(
                    Rules=[S3Rules(
                        Name="suffix",
                        Value="gz"
                    )]
                )
            ),
            Function=GetAtt(lambda_waf_blacklisting_function, "Arn")
        )]
    )
))

cloud_front_logs_bucket_policy = template.add_resource(BucketPolicy(
    "CloudFrontLogsBucketPolicy",
    Bucket=Ref(cloud_front_logs_bucket),
    PolicyDocument={
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Allow-CloudFront-Log-Access",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        Ref(cloud_front_log_processor_lambda_iam_role_param)
                    ]
                },
                "Action": [
                    "s3:*"
                ],
                "Resource": [
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(cloud_front_logs_bucket),
                                "/*"
                            ]
                        ]
                    }
                ]
            }
        ]
    }

))

###
#
# Cloud Front for Web Application Firewall
#
###

gateway_distribution = template.add_resource(Distribution(
    "CerberusDistribution",
    DistributionConfig=DistributionConfig(
        Aliases=[
            Ref(hostname_param)
        ],
        DefaultCacheBehavior=DefaultCacheBehavior(
            AllowedMethods=[
                "GET",
                "PUT",
                "POST",
                "DELETE",
                "PATCH",
                "HEAD",
                "OPTIONS"
            ],
            CachedMethods=[
                "GET",
                "HEAD",
                "OPTIONS"
            ],
            ForwardedValues=ForwardedValues(
                Headers=[
                    "*"
                ],
                QueryString=True
            ),
            MaxTTL=0,
            MinTTL=0,
            DefaultTTL=0,
            TargetOriginId="CerberusGatewayOrigin",
            ViewerProtocolPolicy="https-only"
        ),
        Enabled=True,
        Origins=[
            Origin(
                Id="CerberusGatewayOrigin",
                DomainName=Join(".", ["origin", Ref(hostname_param)]),
                CustomOriginConfig=CustomOrigin(
                    HTTPSPort="443",
                    OriginProtocolPolicy="https-only",
                    OriginSSLProtocols=[
                        "TLSv1.2"
                    ]
                )
            )
        ],
        PriceClass="PriceClass_100",
        ViewerCertificate=ViewerCertificate(
            IamCertificateId=Ref(ssl_certificate_id_param),
            MinimumProtocolVersion="TLSv1",
            SslSupportMethod="sni-only"
        ),
        WebACLId=Ref(web_acl),
        Logging=Logging(
            Bucket=GetAtt(cloud_front_logs_bucket, "DomainName")
        )
    )
))

###
#
# Record Set for public Cerberus Cloud Front CNAME
#
###

cf_gateway_record_set = template.add_resource(RecordSetType(
    "CerberusPublicRecordSet",
    HostedZoneId=Ref(hosted_zone_id_param),
    Name=Join(".", [Ref(hostname_param), ""]),
    TTL=30,
    Type="CNAME",
    ResourceRecords=[GetAtt(gateway_distribution, "DomainName")]
))

###
#
# Outputs
#
###

template.add_output(Output(
    "autoscalingGroupLogicalId",
    Value=Ref(gateway_autoscaling_group)
))

template.add_output(Output(
    "launchConfigurationLogicalId",
    Value=Ref(gateway_launch_config)
))

template.add_output(Output(
    "elbLogicalId",
    Value=Ref(gateway_load_balancer)
))

template.add_output(Output(
    "elbCanonicalHostedZoneNameId",
    Value=GetAtt(gateway_load_balancer, "CanonicalHostedZoneNameID")
))

template.add_output(Output(
    "elbDnsName",
    Value=GetAtt(gateway_load_balancer, "DNSName")
))

template.add_output(Output(
    "elbSourceSecurityGroupName",
    Value=GetAtt(gateway_load_balancer, "SourceSecurityGroup.GroupName")
))

template.add_output(Output(
    "elbSourceSecurityGroupOwnerAlias",
    Value=GetAtt(gateway_load_balancer, "SourceSecurityGroup.OwnerAlias")
))

template.add_output(Output(
    "cloudFrontDistributionId",
    Value=Ref(gateway_distribution),
))

template.add_output(Output(
    "cloudFrontDistributionDomainName",
    Value=GetAtt(gateway_distribution, "DomainName")
))

template.add_output(Output(
    "cloudFrontAccessLogBucket",
    Value=Ref(cloud_front_logs_bucket)
))

template.add_output(Output(
    "whiteListIPSetID",
    Value=Ref(waf_white_list_set)
))

template.add_output(Output(
    "manualBlockIPSetID",
    Value=Ref(waf_manual_block_set)
))

template.add_output(Output(
    "autoBlockIPSetID",
    Value=Ref(waf_auto_block_set)
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
