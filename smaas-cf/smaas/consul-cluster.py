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
from troposphere.autoscaling import AutoScalingGroup, LaunchConfiguration
from troposphere import Ref, Template, Parameter, Output, GetAtt
from troposphere.policies import AutoScalingRollingUpdate, UpdatePolicy

from smaas.support.tags import CerberusTags

CF_FILE = '../../src/main/resources/cloudformation/consul-cluster.json'

# Create Template
template = Template()
template.description = "Launches the Consul cluster in the Cerberus VPC"
template.version = '2010-09-09'

###
#
# Inputs
#
###

cerberus_tags = CerberusTags()
cerberus_tags.add_tag_parameters(template)

vpc_id_param = template.add_parameter(Parameter(
    "vpcId",
    Description="The ID of the VPC",
    Type="AWS::EC2::VPC::Id"
))

instance_profile_param = template.add_parameter(Parameter(
    "instanceProfileName",
    Description="The name for the Consul instance profile",
    Type="String"
))

ami_id_param = template.add_parameter(Parameter(
    "amiId",
    Description="The AMI ID for the Consul server instances",
    Type="String"
))

instance_size_param = template.add_parameter(Parameter(
    "instanceSize",
    Description="The instance size for the Consul server instances",
    Type="String"
))

tools_ingress_sg_id_param = template.add_parameter(Parameter(
    "toolsIngressSgId",
    Description="Tools Ingress Security Group ID",
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

key_pair_name_param = template.add_parameter(Parameter(
    "keyPairName",
    Description="The key pair to be associated with the EC2 instances",
    Type="String",
    Default="cpe-cerberus"
))

user_data_param = template.add_parameter(Parameter(
        "userData",
        Description="Consul User Data",
        Type="String"
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

subnet_id_refs = []
for zone_identifier in range(1, 4):
    vpc_subnet_id = template.add_parameter(Parameter(
            "vpcSubnetIdForAz{0}".format(zone_identifier),
            Description="VPC Subnet ID for Zone {0}".format(zone_identifier),
            Type="String"
    ))

    subnet_id_refs.append(Ref(vpc_subnet_id))


###
#
# EC2 Consul Server Instances
#
###

consul_launch_config = template.add_resource(LaunchConfiguration(
        "ConsulLaunchConfiguration",
        AssociatePublicIpAddress=True,
        IamInstanceProfile=Ref(instance_profile_param),
        ImageId=Ref(ami_id_param),
        InstanceMonitoring=True,
        InstanceType=Ref(instance_size_param),
        KeyName=Ref(key_pair_name_param),
        SecurityGroups=[
            Ref(tools_ingress_sg_id_param),
            Ref(consul_client_sg_id_param),
            Ref(consul_server_sg_id_param)
        ],
        UserData=Ref(user_data_param)
))

consul_autoscaling_group = template.add_resource(AutoScalingGroup(
        "ConsulAutoScalingGroup",
        DesiredCapacity=Ref(desired_instances_param),
        HealthCheckGracePeriod=60,
        HealthCheckType="EC2",
        LaunchConfigurationName=Ref(consul_launch_config),
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
# Outputs
#
###

template.add_output(Output(
        "autoscalingGroupLogicalId",
        Value=Ref(consul_autoscaling_group)
))

template.add_output(Output(
        "launchConfigurationLogicalId",
        Value=Ref(consul_launch_config)
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
