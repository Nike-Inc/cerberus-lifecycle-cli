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
from troposphere import Ref, Template, Parameter, Output, GetAtt
from troposphere.autoscaling import AutoScalingGroup, LaunchConfiguration
from troposphere.elasticloadbalancing import LoadBalancer, ConnectionDrainingPolicy, ConnectionSettings, HealthCheck, \
    Listener, Policy
from troposphere.route53 import RecordSetType
from troposphere.policies import AutoScalingRollingUpdate, UpdatePolicy

from smaas.support.tags import CerberusTags

CF_FILE = '../../src/main/resources/cloudformation/vault-cluster.json'

# Create Template
template = Template()
template.description = "Launches the Vault stack in the Cerberus VPC"
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

vpc_id_param = template.add_parameter(Parameter(
    "vpcId",
    Description="The ID of the VPC",
    Type="AWS::EC2::VPC::Id"
))

instance_profile_name_param = template.add_parameter(Parameter(
    "instanceProfileName",
    Description="The name for the Vault Instance Profile",
    Type="String"
))

ami_id_param = template.add_parameter(Parameter(
    "amiId",
    Description="The AMI ID for the Vault Instances",
    Type="String"
))

instance_size_param = template.add_parameter(Parameter(
    "instanceSize",
    Description="The instance size for the Vault instances",
    Type="String"
))

vault_server_elb_sg_id_param = template.add_parameter(Parameter(
    "vaultServerElbSgId",
    Description="Vault Server ELB Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

tools_ingress_sg_id_param = template.add_parameter(Parameter(
    "toolsIngressSgId",
    Description="Tools Ingress Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

vault_client_sg_id_param = template.add_parameter(Parameter(
    "vaultClientSgId",
    Description="Vault Security Group ID for clients",
    Type="AWS::EC2::SecurityGroup::Id"
))

vault_server_sg_id_param = template.add_parameter(Parameter(
    "vaultServerSgId",
    Description="Vault Security Group ID for servers",
    Type="AWS::EC2::SecurityGroup::Id"
))

consul_client_sg_id_param = template.add_parameter(Parameter(
    "consulClientSgId",
    Description="Consul Client Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

consul_server_sg_id_param = template.add_parameter(Parameter(
    "consulServerSgId",
    Description="Consul Server Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
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

vault_user_data_param = template.add_parameter(Parameter(
    "userData",
    Description="Vault User Data",
    Type="String"
))

hosted_zone_id_param = template.add_parameter(Parameter(
    "hostedZoneId",
    Description="The hosted zone id to associate the CNAME record with",
    Type="String"
))

cname_param = template.add_parameter(Parameter(
    "cname",
    Description="The CNAME to be created",
    Type="String"
))

desired_instances_param = template.add_parameter(Parameter(
    "desiredInstances",
    Description="Desired Number of Auto Scaling Instances",
    Type="Number",
    Default="3"
))

maximum_instances_param = template.add_parameter(Parameter(
    "maximumInstances",
    Description="Maximum Number of Auto Scaling Instances (must be larger than min)",
    Type="Number",
    Default="4"
))

minimum_instances_param = template.add_parameter(Parameter(
    "minimumInstances",
    Description="Minimum Number of Auto Scaling Instances",
    Type="Number",
    Default="3"
))


pause_time_param = template.add_parameter(Parameter(
    "pauseTime",
    Description="Pause time for AutoScalingRollingUpdate e.g PT15M",
    Type="String",
    Default="PT15M"
))

wait_on_resource_signals_param = template.add_parameter(Parameter(
    "waitOnResourceSignals",
    Description="Enabling WaitOnResourceSignals allows CloudFormation to wait until you have received a success signal before performing the next scaling action.",
    Type="String",
    Default="True"
))


###
#
# Elastic Load Balancer
#
###

vault_load_balancer = template.add_resource(LoadBalancer(
    "VaultElasticLoadBalancer",
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
            Target="HTTPS:8200/v1/sys/health",
            Timeout=2,
            UnhealthyThreshold=2
    ),
    Listeners=[
        Listener(
            InstancePort=8200,
            InstanceProtocol="HTTPS",
            LoadBalancerPort=443,
            PolicyNames=[
                "VaultTLSNegotiationPolicy"
            ],
            Protocol="HTTPS",
            SSLCertificateId=Ref(ssl_certificate_arn_param)
        )
    ],
    Policies=[
        Policy(
                PolicyName="VaultTLSNegotiationPolicy",
                PolicyType="SSLNegotiationPolicyType",
                Attributes=[
                    {
                        "Name": "Reference-Security-Policy",
                        "Value": "ELBSecurityPolicy-2015-05"
                    }
                ]
        ),
        Policy(
                PolicyName="VaultPublicKeyPolicy",
                PolicyType="PublicKeyPolicyType",
                Attributes=[
                    {
                        "Name": "PublicKey",
                        "Value": Ref(cert_public_key_param)
                    }
                ]
        ),
        Policy(
                PolicyName="VaultBackendServerAuthenticationPolicy",
                PolicyType="BackendServerAuthenticationPolicyType",
                Attributes=[
                    {
                        "Name": "PublicKeyPolicyName",
                        "Value": "VaultPublicKeyPolicy"
                    }
                ],
                InstancePorts=[
                    "8200"
                ]
        )
    ],
    Scheme="internal",
    SecurityGroups=[
        Ref(vault_server_elb_sg_id_param)
    ],
    Subnets=subnet_id_refs,
    Tags=cerberus_tags.get_tags_as_list()
))

###
#
# Launch Configuration and Auto Scaling Group for Vault
#
###

vault_launch_config = template.add_resource(LaunchConfiguration(
    "VaultLaunchConfiguration",
    AssociatePublicIpAddress=True,
    IamInstanceProfile=Ref(instance_profile_name_param),
    ImageId=Ref(ami_id_param),
    InstanceMonitoring=True,
    InstanceType=Ref(instance_size_param),
    KeyName=Ref(key_pair_name_param),
    SecurityGroups=[
        Ref(tools_ingress_sg_id_param),
        Ref(vault_client_sg_id_param),
        Ref(vault_server_sg_id_param),
        Ref(consul_client_sg_id_param),
        Ref(consul_server_sg_id_param)
    ],
    UserData=Ref(vault_user_data_param)
))

vault_autoscaling_group = template.add_resource(AutoScalingGroup(
    "VaultAutoScalingGroup",
    DesiredCapacity=Ref(desired_instances_param),
    HealthCheckGracePeriod=60,
    HealthCheckType="EC2",
    LaunchConfigurationName=Ref(vault_launch_config),
    LoadBalancerNames=[
        Ref(vault_load_balancer)
    ],
    MaxSize=Ref(maximum_instances_param),
    MinSize=Ref(minimum_instances_param),
    UpdatePolicy=UpdatePolicy(
            AutoScalingRollingUpdate=AutoScalingRollingUpdate(
                    MaxBatchSize=1,
                    MinInstancesInService=Ref(minimum_instances_param),
                    PauseTime=Ref(pause_time_param),
                    WaitOnResourceSignals=Ref(wait_on_resource_signals_param)
            )
    ),
    VPCZoneIdentifier=subnet_id_refs,
    Tags=cerberus_tags.get_autoscaling_tags_as_list()
))

###
#
# Record Set for Vault CNAME
#
###

vault_record_set = template.add_resource(RecordSetType(
    "VaultRecordSet",
    HostedZoneId=Ref(hosted_zone_id_param),
    Name=Ref(cname_param),
    TTL=30,
    Type="CNAME",
    ResourceRecords=[GetAtt(vault_load_balancer, "DNSName")]
))

###
#
# Outputs
#
###

template.add_output(Output(
    "autoscalingGroupLogicalId",
    Value=Ref(vault_autoscaling_group)
))

template.add_output(Output(
    "launchConfigurationLogicalId",
    Value=Ref(vault_launch_config)
))

template.add_output(Output(
    "elbLogicalId",
    Value=Ref(vault_load_balancer)
))

template.add_output(Output(
    "elbCanonicalHostedZoneNameId",
    Value=GetAtt(vault_load_balancer, "CanonicalHostedZoneNameID")
))

template.add_output(Output(
    "elbDnsName",
    Value=GetAtt(vault_load_balancer, "DNSName")
))

template.add_output(Output(
    "elbSourceSecurityGroupName",
    Value=GetAtt(vault_load_balancer, "SourceSecurityGroup.GroupName")
))

template.add_output(Output(
    "elbSourceSecurityGroupOwnerAlias",
    Value=GetAtt(vault_load_balancer, "SourceSecurityGroup.OwnerAlias")
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
