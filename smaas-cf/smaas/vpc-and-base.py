###
# Copyright (c) 2016 Nike Inc.
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
from troposphere import Ref, Template, Parameter, Output, GetAtt, Join, AWS_ACCOUNT_ID, Equals, If
from troposphere.ec2 import VPC
from troposphere.ec2 import DHCPOptions
from troposphere.ec2 import VPCDHCPOptionsAssociation
from troposphere.ec2 import RouteTable
from troposphere.ec2 import InternetGateway
from troposphere.ec2 import Route
from troposphere.ec2 import VPCGatewayAttachment
from troposphere.ec2 import Subnet
from troposphere.ec2 import SubnetRouteTableAssociation
from troposphere.ec2 import SecurityGroup
from troposphere.ec2 import SecurityGroupIngress
from troposphere.iam import Role, InstanceProfile, PolicyType, Policy
from troposphere.kms import Key
from troposphere.rds import DBInstance, DBSubnetGroup, DBParameterGroup
from troposphere.route53 import HostedZone, HostedZoneVPCs, HostedZoneConfiguration
from troposphere.s3 import Bucket, Private, BucketPolicy, PublicRead, WebsiteConfiguration, VersioningConfiguration

from smaas.support.constants import aws_region_ref
from smaas.support.tags import CerberusTags
from smaas.support.network import CerberusNetwork

CF_FILE = '../../src/main/resources/cloudformation/vpc-and-base.json'

# Create Template
template = Template()
template.description = "Creates the Cerberus VPC and foundational resources"
template.version = '2010-09-09'

cerberus_tags = CerberusTags()
cerberus_tags.add_tag_parameters(template)

cerberus_network = CerberusNetwork()
cerberus_network.add_parameters(template)

###
#
# Parameters
#
###

# Required account admin ARN for KMS key administration
account_admin_arn_param = template.add_parameter(Parameter(
    "accountAdminArn",
    Description="The ARN for a IAM user, group or role that can create this stack.",
    Type="String"
))

# Availability Zones: We use a more generic 1, 2, 3 to represent the actual zones for abstraction
az1_param = template.add_parameter(Parameter(
    "az1",
    Description="An availability zone identifier for zone '1'",
    Type="String",
    AllowedPattern="[a-z]{2}-[a-z]+-\d\w"
))

az2_param = template.add_parameter(Parameter(
    "az2",
    Description="An availability zone identifier for zone '2'",
    Type="String",
    AllowedPattern="[a-z]{2}-[a-z]+-\d\w"
))

az3_param = template.add_parameter(Parameter(
    "az3",
    Description="An availability zone identifier for zone '3'",
    Type="String",
    AllowedPattern="[a-z]{2}-[a-z]+-\d\w"
))

# RDS instance parameters for the management DB
cms_db_allocated_storage_param = template.add_parameter(Parameter(
    "cmsDbAllocatedStorage",
    Description="The allocated storage for the RDS instance.",
    Type="String",
    Default="100"
))

cms_db_instance_size_param = template.add_parameter(Parameter(
    "cmsDbInstanceSize",
    Description="MySQL DB instance class",
    Type="String",
    Default="db.r3.large"
))

cms_db_name_param = template.add_parameter(Parameter(
    "cmsDbName",
    Description="The name of the database initially create on the RDS instance",
    Type="String"
))

cms_db_master_username_param = template.add_parameter(Parameter(
    "cmsDbMasterUsername",
    Description="Master username for the cms RDS instance",
    Type="String"
))

cms_db_master_password_param = template.add_parameter(Parameter(
    "cmsDbMasterPassword",
    Description="Master password for the cms RDS instance",
    Type="String"
))

cms_db_port_param = template.add_parameter(Parameter(
    "cmsDbPort",
    Description="Port for the cms DB instance",
    Type="Number",
    Default="3306"
))

# Route 53 hosted zone and record sets for VPC
vpc_hosted_zone_name_param = template.add_parameter(Parameter(
    "vpcHostedZoneName",
    Description="The hosted zone name used in the Cerberus VPC",
    Type="String"
))

az_by_identifier_map = {
    1: az1_param,
    2: az2_param,
    3: az3_param
}

###
#
# VPC and Supporting Networking
#
###

# Create the VPC
cerberus_vpc = template.add_resource(VPC(
    "CerberusVpc",
    CidrBlock=Ref(cerberus_network.vpc_cidr_block_param),
    EnableDnsSupport=True,
    EnableDnsHostnames=True,
    Tags=cerberus_tags.get_tags_as_list()
))

# DHCP Options
# http://docs.aws.amazon.com/AmazonVPC/latest/UserGuide/VPC_DHCP_Options.html
# If you're using AmazonProvidedDNS in us-east-1, specify ec2.internal.
# If you're using AmazonProvidedDNS in another region, specify region.compute.internal
# (for example, ap-northeast-1.compute.internal). Otherwise, specify a domain name
# (for example, MyCompany.com). This value is used to complete unqualified DNS hostnames.
template.add_condition("RegionEqualsEastOne", Equals(aws_region_ref, "us-east-1"))
cerberus_dhcp_options = template.add_resource(DHCPOptions(
    "CerberusDhcpOptions",
    DomainName=If("RegionEqualsEastOne", "ec2.internal", Join(".", [aws_region_ref, "compute.internal"])),
    DomainNameServers=["AmazonProvidedDNS"],
    Tags=cerberus_tags.get_tags_as_list()
))

cerberus_dhcp_options_association = template.add_resource(VPCDHCPOptionsAssociation(
    "CerberusVpcDhcpOptionsAssociation",
    DhcpOptionsId=Ref(cerberus_dhcp_options),
    VpcId=Ref(cerberus_vpc)
))

# Routing, Subnets and Gateways
cerberus_route_table = template.add_resource(RouteTable(
    "CerberusRouteTable",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

cerberus_internet_gateway = template.add_resource(InternetGateway(
    "CerberusInternetGateway",
    Tags=cerberus_tags.get_tags_as_list()
))

cerberus_route_internet_gateway = template.add_resource(Route(
    "CerberusRouteInternetGateway",
    DestinationCidrBlock=Ref(cerberus_network.gateway_cidr_block_param),
    GatewayId=Ref(cerberus_internet_gateway),
    RouteTableId=Ref(cerberus_route_table)
))

cerberus_vpc_gateway_attachment = template.add_resource(VPCGatewayAttachment(
    "CerberusVpcGatewayAttachment",
    InternetGatewayId=Ref(cerberus_internet_gateway),
    VpcId=Ref(cerberus_vpc)
))

# Associate the three subnets to the route table
vpc_subnet_resources_by_zone_identifier = {}
for zone_identifier, subnet_cidr_block in cerberus_network.subnet_cidr_block_param_by_az_map.items():
    subnet = template.add_resource(Subnet(
        "CerberusSubnetForAz{0}".format(zone_identifier),
        AvailabilityZone=Ref(az_by_identifier_map[zone_identifier]),
        CidrBlock=Ref(subnet_cidr_block),
        MapPublicIpOnLaunch=True,
        VpcId=Ref(cerberus_vpc),
        Tags=cerberus_tags.get_tags_as_list()
    ))

    vpc_subnet_resources_by_zone_identifier[zone_identifier] = subnet

    template.add_resource(SubnetRouteTableAssociation(
        "CerberusSubnetRouteTableAssociationForAz{0}".format(zone_identifier),
        RouteTableId=Ref(cerberus_route_table),
        SubnetId=Ref(subnet)
    ))

###
#
# Security Groups
#
###

tools_ingress_sg = template.add_resource(SecurityGroup(
    "ToolsIngressSg",
    GroupDescription="Administration ingress from tools NAT boxes",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

gateway_elb_sg = template.add_resource(SecurityGroup(
    "GatewayElbSg",
    GroupDescription="Gateway ELB Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_gateway_tags_as_list()
))

gateway_server_sg = template.add_resource(SecurityGroup(
    "GatewayServerSg",
    GroupDescription="Gateway Server Instance Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

cms_elb_sg = template.add_resource(SecurityGroup(
    "CmsElbSg",
    GroupDescription="Management Server ELB Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

cms_sg = template.add_resource(SecurityGroup(
    "CmsSg",
    GroupDescription="Management Server Instance Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

cms_db_sg = template.add_resource(SecurityGroup(
    "CmsDbSg",
    GroupDescription="Management Server Database Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

vault_server_elb_sg = template.add_resource(SecurityGroup(
    "VaultServerElbSg",
    GroupDescription="Vault ServerELB Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

vault_client_sg = template.add_resource(SecurityGroup(
    "VaultClientSg",
    GroupDescription="Vault Client Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

vault_server_sg = template.add_resource(SecurityGroup(
    "VaultServerSg",
    GroupDescription="Vault Server Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

consul_client_sg = template.add_resource(SecurityGroup(
    "ConsulClientSg",
    GroupDescription="Consul Client Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

consul_server_sg = template.add_resource(SecurityGroup(
    "ConsulServerSg",
    GroupDescription="Consul Server Security Group",
    VpcId=Ref(cerberus_vpc),
    Tags=cerberus_tags.get_tags_as_list()
))

###
#
# Security Group Ingress Rules
#
###

# Allow HTTPS through from the internet to the ELB
template.add_resource(SecurityGroupIngress(
    "GatewayElbIngressFromInternetSg443",
    GroupId=Ref(gateway_elb_sg),
    CidrIp=Ref(cerberus_network.gateway_cidr_block_param),
    FromPort=443,
    IpProtocol="tcp",
    ToPort=443
))

# Allow the internet facing gateway ELB to talk to the gateway instances over HTTPS (443)
template.add_resource(SecurityGroupIngress(
    "GatewayServerIngressFromGatewayElb443",
    GroupId=Ref(gateway_server_sg),
    SourceSecurityGroupId=Ref(gateway_elb_sg),
    FromPort=443,
    IpProtocol="tcp",
    ToPort=443
))

# Allow gateway server instances to talk to the management server ELB over HTTPS (443)
template.add_resource(SecurityGroupIngress(
    "CmsElbFromGatewayServer443",
    GroupId=Ref(cms_elb_sg),
    SourceSecurityGroupId=Ref(gateway_server_sg),
    FromPort=443,
    IpProtocol="tcp",
    ToPort=443
))

# Allow gateway server instances to talk to the vault ELB over HTTPS (443)
template.add_resource(SecurityGroupIngress(
    "VaultServerElbFromGatewayServer443",
    GroupId=Ref(vault_server_elb_sg),
    SourceSecurityGroupId=Ref(gateway_server_sg),
    FromPort=443,
    IpProtocol="tcp",
    ToPort=443
))

# Allow the management server ELB to talk to the management server instances over HTTPS (8443)
template.add_resource(SecurityGroupIngress(
    "CmsIngressFromCmsElb8443",
    GroupId=Ref(cms_sg),
    SourceSecurityGroupId=Ref(cms_elb_sg),
    FromPort=8443,
    IpProtocol="tcp",
    ToPort=8443
))

# Allow the management server ELB to talk to the management server instances over HTTP (8080)
template.add_resource(SecurityGroupIngress(
    "CmsIngressFromCmsElb8080",
    GroupId=Ref(cms_sg),
    SourceSecurityGroupId=Ref(cms_elb_sg),
    FromPort=8080,
    IpProtocol="tcp",
    ToPort=8080
))

# Allow the management server ELB to talk to the management server instances over HTTP (8077)
template.add_resource(SecurityGroupIngress(
    "CmsIngressFromCmsElb8077",
    GroupId=Ref(cms_sg),
    SourceSecurityGroupId=Ref(cms_elb_sg),
    FromPort=8077,
    IpProtocol="tcp",
    ToPort=8077
))

# Allow the management server instances to talk to the Vault ELB over HTTPS (443)
template.add_resource(SecurityGroupIngress(
    "VaultElbIngressFromCms443",
    GroupId=Ref(vault_server_elb_sg),
    SourceSecurityGroupId=Ref(cms_sg),
    FromPort=443,
    IpProtocol="tcp",
    ToPort=443
))

# Allow the management server instances to access the management db instance
template.add_resource(SecurityGroupIngress(
    "CmsDbFromCms",
    GroupId=Ref(cms_db_sg),
    SourceSecurityGroupId=Ref(cms_sg),
    FromPort=Ref(cms_db_port_param),
    IpProtocol="tcp",
    ToPort=Ref(cms_db_port_param)
))

# Allow the Vault server ELB to talk to Vault server instances over HTTPS (8200)
template.add_resource(SecurityGroupIngress(
    "VaultServerIngressFromVaultElb8200",
    GroupId=Ref(vault_server_sg),
    SourceSecurityGroupId=Ref(vault_server_elb_sg),
    FromPort=8200,
    IpProtocol="tcp",
    ToPort=8200
))

# Allow the Vault server ELB to talk to Vault server instances over HTTPS (8201)
# Vault does internal communication by default on the port above the normal listening port, in this case 8201
template.add_resource(SecurityGroupIngress(
    "VaultServerIngressFromVaultElb8201",
    GroupId=Ref(vault_server_sg),
    SourceSecurityGroupId=Ref(vault_server_elb_sg),
    FromPort=8201,
    IpProtocol="tcp",
    ToPort=8201
))

# Allow Vault server instances to talk to other Vault server instances on 8200
template.add_resource(SecurityGroupIngress(
    "VaultServerIngress8200",
    GroupId=Ref(vault_server_sg),
    SourceSecurityGroupId=Ref(vault_client_sg),
    FromPort=8200,
    IpProtocol="tcp",
    ToPort=8200
))

# Allow Consul agents to talk on 8300, 8301 and 8302
template.add_resource(SecurityGroupIngress(
    "ConsulServerIngress8300",
    GroupId=Ref(consul_server_sg),
    SourceSecurityGroupId=Ref(consul_client_sg),
    FromPort=8300,
    IpProtocol="tcp",
    ToPort=8300
))

for port in [8301, 8302]:
    template.add_resource(SecurityGroupIngress(
        "ConsulServerIngressTcp{0}".format(port),
        GroupId=Ref(consul_server_sg),
        SourceSecurityGroupId=Ref(consul_client_sg),
        FromPort=port,
        IpProtocol="tcp",
        ToPort=port
    ))

    template.add_resource(SecurityGroupIngress(
        "ConsulServerIngressUdp{0}".format(port),
        GroupId=Ref(consul_server_sg),
        SourceSecurityGroupId=Ref(consul_client_sg),
        FromPort=port,
        IpProtocol="udp",
        ToPort=port
    ))

###
#
# IAM roles
#
###

cloud_front_log_processor_lambda_iam_role = template.add_resource(Role(
    "WAFLambdaRole",
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
            PolicyName="WAFAccess",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "waf:*",
                    ],
                    "Resource": "*"
                }],
            }
        ),
        Policy(
            PolicyName="S3BucketList",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "s3:ListAllMyBuckets",
                    ],
                    "Resource": "*"
                }],
            }
        ),
        Policy(
            PolicyName="LogsAccess",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "logs:*",
                    ],
                    "Resource": "*"
                }],
            }
        ),
        Policy(
            PolicyName="LambdaAccess",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "lambda:*",
                    ],
                    "Resource": "*"
                }],
            }
        ),
        Policy(
            PolicyName="CloudFormationAccess",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "cloudformation:DescribeStacks",
                        "cloudformation:ListStacks",
                    ],
                    "Resource": "*"
                }],
            }
        ),
        Policy(
            PolicyName="CloudWatchAccess",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "cloudwatch:PutMetricData",
                    ],
                    "Resource": "*"
                }],
            }
        )
    ],
    Path="/"
))

gateway_iam_role = template.add_resource(Role(
    "GatewayIamRole",
    AssumeRolePolicyDocument={
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {
                "Service": ["ec2.amazonaws.com"]
            },
            "Action": ["sts:AssumeRole"]
        }]
    },
    Policies=[
        Policy(
                PolicyName="gatewayPolicy",
                PolicyDocument={
                    "Statement": [{
                        "Effect": "Allow",
                        "Action": [
                            "EC2:Describe*",
                            "cloudformation:SignalResource"
                        ],
                        "Resource": "*"
                    }],
                }
        )
    ],
    Path="/"
))

cms_iam_role = template.add_resource(Role(
    "CmsIamRole",
    AssumeRolePolicyDocument={
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {
                "Service": ["ec2.amazonaws.com"]
            },
            "Action": ["sts:AssumeRole"]
        }]
    },
    Policies=[
        Policy(
                PolicyName="cmsPolicy",
                PolicyDocument={
                    "Statement": [{
                        "Effect": "Allow",
                        "Action": [
                            "EC2:Describe*",
                            "cloudformation:SignalResource"
                        ],
                        "Resource": "*"
                    }],
                }
        )
    ],
    Path="/"
))

vault_iam_role = template.add_resource(Role(
    "VaultIamRole",
    AssumeRolePolicyDocument={
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {
                "Service": ["ec2.amazonaws.com"]
            },
            "Action": ["sts:AssumeRole"]
        }]
    },
    Policies=[
        Policy(
                PolicyName="consulPolicy",
                PolicyDocument={
                    "Statement": [{
                        "Effect": "Allow",
                        "Action": [
                            "EC2:Describe*",
                            "cloudformation:SignalResource"
                        ],
                        "Resource": "*"
                    }],
                }
        )
    ],
    Path="/"
))

consul_iam_role = template.add_resource(Role(
    "ConsulIamRole",
    AssumeRolePolicyDocument={
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {
                "Service": ["ec2.amazonaws.com"]
            },
            "Action": ["sts:AssumeRole"]
        }]
    },
    Policies=[
        Policy(
            PolicyName="consulPolicy",
            PolicyDocument={
                "Statement": [{
                    "Effect": "Allow",
                    "Action": [
                        "EC2:Describe*",
                        "cloudformation:DescribeStacks",
                        "cloudformation:SignalResource"
                    ],
                    "Resource": "*"
                }],
            }
        )
    ],
    Path="/"
))

gateway_instance_profile = template.add_resource(InstanceProfile(
    "GatewayInstanceProfile",
    Path="/",
    Roles=[
        Ref(gateway_iam_role)
    ]
))

cms_instance_profile = template.add_resource(InstanceProfile(
    "CmsInstanceProfile",
    Path="/",
    Roles=[
        Ref(cms_iam_role)
    ]
))

vault_instance_profile = template.add_resource(InstanceProfile(
    "VaultInstanceProfile",
    Path="/",
    Roles=[
        Ref(vault_iam_role)
    ]
))

consul_instance_profile = template.add_resource(InstanceProfile(
    "ConsulInstanceProfile",
    Path="/",
    Roles=[
        Ref(consul_iam_role)
    ]
))

###
#
# S3 Bucket for the dashboard client-side application
#
###

dashboard_bucket = template.add_resource(Bucket(
    "DashboardBucket",
    AccessControl=PublicRead,
    WebsiteConfiguration=WebsiteConfiguration(
        IndexDocument="index.html",
        ErrorDocument="error.html"
    ),
    Tags=cerberus_tags.get_tags()
))

dashboard_bucket_access_policy = template.add_resource(BucketPolicy(
    "DashboardBucketAccessPolicy",
    Bucket=Ref(dashboard_bucket),
    PolicyDocument={
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Allow-Bucket-Access",
                "Effect": "Allow",
                "Principal": "*",
                "Action": [
                    "s3:GetObject"
                ],
                "Resource": [
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(dashboard_bucket),
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
# S3 Bucket for runtime configuration files for Vault and Consul
#
###

config_bucket = template.add_resource(Bucket(
    "CerberusConfigBucket",
    AccessControl=Private,
    VersioningConfiguration=VersioningConfiguration(
        Status="Enabled"
    ),
    Tags=cerberus_tags.get_tags()
))

config_bucket_access_policy = template.add_resource(BucketPolicy(
    "CerberusConfigBucketAccessPolicy",
    Bucket=Ref(config_bucket),
    PolicyDocument={
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Allow-ListBucket-Access",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(gateway_iam_role, "Arn"),
                        GetAtt(cms_iam_role, "Arn"),
                        GetAtt(vault_iam_role, "Arn"),
                        GetAtt(consul_iam_role, "Arn"),
                        GetAtt(cloud_front_log_processor_lambda_iam_role, "Arn")
                    ]
                },
                "Action": [
                    "s3:ListBucket"
                ],
                "Resource": [
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket)
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/*"
                            ]
                        ]
                    }
                ]
            },
            {
                "Sid": "Allow-Bucket-Access-For-Gateway",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(gateway_iam_role, "Arn")
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
                                Ref(config_bucket),
                                "/data/gateway/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/gateway"
                            ]
                        ]
                    }
                ]
            },
            {
                "Sid": "Allow-Bucket-Access-For-CMS",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(cms_iam_role, "Arn")
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
                                Ref(config_bucket),
                                "/data/cms/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/cms"
                            ]
                        ]
                    }
                ]
            },
            {
                "Sid": "Allow-Bucket-Access-For-Log-Processing-Lambda",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(cloud_front_log_processor_lambda_iam_role, "Arn")
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
                                Ref(config_bucket),
                                "/data/cloud-front-log-processor/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/cloud-front-log-processor"
                            ]
                        ]
                    }
                ]
            },
            {
                "Sid": "Allow-Bucket-Access-For-Vault",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(vault_iam_role, "Arn")
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
                                Ref(config_bucket),
                                "/data/vault/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/vault"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/consul/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/consul"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/config/secrets.json"
                            ]
                        ]
                    }
                ]
            },
            {
                "Sid": "Allow-Bucket-Access-For-Consul",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(consul_iam_role, "Arn")
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
                                Ref(config_bucket),
                                "/data/consul/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/data/consul"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/consul/*"
                            ]
                        ]
                    },
                    {
                        "Fn::Join": [
                            "",
                            [
                                "arn:aws:s3:::",
                                Ref(config_bucket),
                                "/consul"
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
# KMS for config file decryption by vault and consul instances
#
###

config_file_key = template.add_resource(Key(
    "ConfigFileKey",
    Description="Cerberus encryption key for storing config files in an encrypted state.",
    Enabled=True,
    KeyPolicy={
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "Allow-Root-User",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        {
                            "Fn::Join": [
                                "",
                                [
                                    "arn:aws:iam::",
                                    Ref(AWS_ACCOUNT_ID),
                                    ":root"
                                ]
                            ]
                        }
                    ]
                },
                "Action": [
                    "kms:*"
                ],
                "Resource": "*"
            },
            {
                "Sid": "Allow-Decrypt-From-Instances",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        GetAtt(gateway_iam_role, "Arn"),
                        GetAtt(cms_iam_role, "Arn"),
                        GetAtt(vault_iam_role, "Arn"),
                        GetAtt(consul_iam_role, "Arn")
                    ]
                },
                "Action": [
                    "kms:Decrypt"
                ],
                "Resource": "*"
            },
            {
                "Sid": "Allow-Account-Admin",
                "Effect": "Allow",
                "Principal": {
                    "AWS": [
                        Ref(account_admin_arn_param)
                    ]
                },
                "Action": [
                    "kms:*"
                ],
                "Resource": "*"
            }
        ]
    }
))

###
#
# Policy for the CMS role being able to manage KMS keys
#
###

cms_kms_policy = template.add_resource(PolicyType(
    "CmsKmsPolicy",
    PolicyName="CmsKmsPolicy",
    Roles=[
        Ref(cms_iam_role)
    ],
    PolicyDocument={
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": [
                    "kms:CreateAlias",
                    "kms:CreateKey",
                    "kms:DeleteAlias",
                    "kms:DescribeKey",
                    "kms:DisableKey",
                    "kms:DisableKeyRotation",
                    "kms:EnableKey",
                    "kms:EnableKeyRotation",
                    "kms:GetKeyPolicy",
                    "kms:GetKeyRotationStatus",
                    "kms:ListAliases",
                    "kms:ListKeyPolicies",
                    "kms:ListKeys",
                    "kms:PutKeyPolicy",
                    "kms:UpdateAlias",
                    "kms:UpdateKeyDescription"
                ],
                "Resource": [
                    "*"
                ]
            }
        ]
    }
))

###
#
# RDS for the management service
#
###

cms_db_subnet_group = template.add_resource(DBSubnetGroup(
    "CmsDatabaseSubnetGroup",
    DBSubnetGroupDescription="DB Subnet Group for management DB",
    SubnetIds=[
        Ref(vpc_subnet_resources_by_zone_identifier[1]),
        Ref(vpc_subnet_resources_by_zone_identifier[2]),
        Ref(vpc_subnet_resources_by_zone_identifier[3])
    ]
))

cms_db_param_group = template.add_resource(DBParameterGroup(
    "CmsDatabaseParamGroup",
    Description="Default parameters for the cms DB",
    Family="mysql5.6",
    Parameters={
        "slow_query_log": 1,
        "log_output": "TABLE"
    }
))

cms_db_instance = template.add_resource(DBInstance(
    "CmsDatabase",
    AllocatedStorage=Ref(cms_db_allocated_storage_param),
    AllowMajorVersionUpgrade=True,
    AutoMinorVersionUpgrade=True,
    BackupRetentionPeriod=14,
    DBInstanceClass=Ref(cms_db_instance_size_param),
    DBName=Ref(cms_db_name_param),
    DBParameterGroupName=Ref(cms_db_param_group),
    DBSubnetGroupName=Ref(cms_db_subnet_group),
    Engine="MySQL",
    MasterUsername=Ref(cms_db_master_username_param),
    MasterUserPassword=Ref(cms_db_master_password_param),
    VPCSecurityGroups=[
        Ref(tools_ingress_sg),
        Ref(cms_db_sg)
    ],
    MultiAZ=True,
    PreferredBackupWindow="13:14-13:44",
    PreferredMaintenanceWindow="tue:06:48-tue:07:18",
    PubliclyAccessible=False,
    StorageEncrypted=True,
    StorageType="gp2",
    Port=Ref(cms_db_port_param),
    Tags=cerberus_tags.get_tags_as_list()
))

###
#
# VPC Hosted Zone and Record Sets
#
###

vpc_hosted_zone = template.add_resource(HostedZone(
    "CerberusInternalHostedZone",
    Name=Join(".", [aws_region_ref, Ref(vpc_hosted_zone_name_param)]),
    VPCs=[
        HostedZoneVPCs(
            VPCId=Ref(cerberus_vpc),
            VPCRegion=aws_region_ref
        )
    ],
    HostedZoneTags=cerberus_tags.get_tags(),
    HostedZoneConfig=HostedZoneConfiguration(
        Comment="The hosted zone for the Cerberus VPC"
    )
))

###
#
# Outputs
#
###

template.add_output(Output(
    "vpcId",
    Value=Ref(cerberus_vpc)
))

template.add_output(Output(
    "gatewayIamRoleArn",
    Value=GetAtt(gateway_iam_role, "Arn")
))

template.add_output(Output(
    "cmsIamRoleArn",
    Value=GetAtt(cms_iam_role, "Arn")
))

template.add_output(Output(
    "consulIamRoleArn",
    Value=GetAtt(consul_iam_role, "Arn")
))

template.add_output(Output(
    "vaultIamRoleArn",
    Value=GetAtt(vault_iam_role, "Arn")
))

template.add_output(Output(
    "cloudFrontLogProcessorLambdaIamRoleArn",
    Value=GetAtt(cloud_front_log_processor_lambda_iam_role, "Arn")
))

template.add_output(Output(
    "gatewayInstanceProfileName",
    Value=Ref(gateway_instance_profile)
))

template.add_output(Output(
    "cmsInstanceProfileName",
    Value=Ref(cms_instance_profile)
))

template.add_output(Output(
    "consulInstanceProfileName",
    Value=Ref(consul_instance_profile)
))

template.add_output(Output(
    "vaultInstanceProfileName",
    Value=Ref(vault_instance_profile)
))

template.add_output(Output(
    "toolsIngressSgId",
    Value=GetAtt(tools_ingress_sg, "GroupId")
))

template.add_output(Output(
    "gatewayElbSgId",
    Value=GetAtt(gateway_elb_sg, "GroupId")
))

template.add_output(Output(
    "gatewayServerSgId",
    Value=GetAtt(gateway_server_sg, "GroupId")
))

template.add_output(Output(
    "cmsElbSgId",
    Value=GetAtt(cms_elb_sg, "GroupId")
))

template.add_output(Output(
    "cmsSgId",
    Value=GetAtt(cms_sg, "GroupId")
))

template.add_output(Output(
    "cmsDbSgId",
    Value=GetAtt(cms_db_sg, "GroupId")
))

template.add_output(Output(
    "vaultServerElbSgId",
    Value=GetAtt(vault_server_elb_sg, "GroupId")
))

template.add_output(Output(
    "vaultClientSgId",
    Value=GetAtt(vault_client_sg, "GroupId")
))

template.add_output(Output(
    "vaultServerSgId",
    Value=GetAtt(vault_server_sg, "GroupId")
))

template.add_output(Output(
    "consulClientSgId",
    Value=GetAtt(consul_client_sg, "GroupId")
))

template.add_output(Output(
    "consulServerSgId",
    Value=GetAtt(consul_server_sg, "GroupId")
))

template.add_output(Output(
    "configFileKeyId",
    Value=Ref(config_file_key)
))

template.add_output(Output(
    "dashboardBucketName",
    Value=Ref(dashboard_bucket)
))

template.add_output(Output(
    "dashboardBucketWebsiteUrl",
    Value=GetAtt(dashboard_bucket, "WebsiteURL")
))

template.add_output(Output(
    "configBucketName",
    Value=Ref(config_bucket)
))

template.add_output(Output(
    "configBucketDomainName",
    Value=GetAtt(config_bucket, "DomainName")
))

template.add_output(Output(
    "cmsDbId",
    Value=Ref(cms_db_instance)
))

template.add_output(Output(
    "cmsDbAddress",
    Value=GetAtt(cms_db_instance, "Endpoint.Address")
))

template.add_output(Output(
    "cmsDbPort",
    Value=GetAtt(cms_db_instance, "Endpoint.Port")
))

template.add_output(Output(
    "cmsDbJdbcConnectionString",
    Description="JDBC connection string for cms database",
    Value=Join("", [
        "jdbc:mysql://",
        GetAtt(cms_db_instance, "Endpoint.Address"),
        ":",
        GetAtt(cms_db_instance, "Endpoint.Port"),
        "/",
        Ref(cms_db_name_param),
        "?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=false&serverTimezone=UTC"
    ])
))

template.add_output(Output(
    "vpcHostedZoneId",
    Description="The VPC hosted zone ID",
    Value=Ref(vpc_hosted_zone)
))

for zone_identifier, subnet in vpc_subnet_resources_by_zone_identifier.items():
    template.add_output(Output(
        "vpcSubnetIdForAz{0}".format(zone_identifier),
        Value=Ref(subnet)
    ))

template.add_output(Output(
    "cmsKmsPolicyId",
    Description="The policy id for CMS managing KMS keys",
    Value=Ref(cms_kms_policy)
))

cerberus_network.add_outputs(template)

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
