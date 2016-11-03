from io import open
from troposphere import Ref, Template, Parameter, Output
from troposphere.autoscaling import AutoScalingGroup, LaunchConfiguration
from troposphere.policies import AutoScalingRollingUpdate, UpdatePolicy

from smaas.support.tags import CerberusTags

CF_FILE = '../../src/main/resources/cloudformation/rds-backup-cluster.json'

# Create Template
template = Template()
template.description = "Launches the RDS backup stack in the Cerberus VPC"
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

ami_id_param = template.add_parameter(Parameter(
    "amiId",
    Description="The AMI ID for the RDS backup instances",
    Type="String"
))

instance_size_param = template.add_parameter(Parameter(
    "instanceSize",
    Description="The instance size for the RDS backup instances",
    Type="String"
))

tools_ingress_sg_id_param = template.add_parameter(Parameter(
    "toolsIngressSgId",
    Description="Tools Ingress Security Group ID",
    Type="AWS::EC2::SecurityGroup::Id"
))

cms_sg_id_param = template.add_parameter(Parameter(
    "cmsSgId",
    Description="CMS Security Group ID for servers",
    Type="AWS::EC2::SecurityGroup::Id"
))

instance_profile_name_param = template.add_parameter(Parameter(
    "instanceProfileName",
    Description="The name for the Vault Instance Profile",
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
    Type="String"
))

user_data_param = template.add_parameter(Parameter(
    "userData",
    Description="RDS backup cluster user data",
    Type="String"
))

###
#
# Launch Configuration and Auto Scaling Group
#
###

rds_backup_launch_config = template.add_resource(LaunchConfiguration(
    "RdsBackupLaunchConfiguration",
    AssociatePublicIpAddress=True,
    IamInstanceProfile=Ref(instance_profile_name_param),
    ImageId=Ref(ami_id_param),
    InstanceMonitoring=True,
    InstanceType=Ref(instance_size_param),
    KeyName=Ref(key_pair_name_param),
    SecurityGroups=[
        Ref(tools_ingress_sg_id_param),
        Ref(cms_sg_id_param)
    ],
    UserData=Ref(user_data_param)
))

rds_backup_autoscaling_group = template.add_resource(AutoScalingGroup(
    "RdsBackupAutoScalingGroup",
    DesiredCapacity=1,
    HealthCheckGracePeriod=60,
    HealthCheckType="EC2",
    LaunchConfigurationName=Ref(rds_backup_launch_config),
    MaxSize=1,
    MinSize=1,
    UpdatePolicy=UpdatePolicy(
        AutoScalingRollingUpdate=AutoScalingRollingUpdate(
            MaxBatchSize=1,
            MinInstancesInService=0,
            PauseTime="PT5M",
            WaitOnResourceSignals=False
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
    Value=Ref(rds_backup_autoscaling_group)
))

template.add_output(Output(
    "launchConfigurationLogicalId",
    Value=Ref(rds_backup_launch_config)
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
