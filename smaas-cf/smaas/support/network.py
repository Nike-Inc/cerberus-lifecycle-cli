from troposphere import Parameter, Template, Output

VPC_SUBNET_CIDRS = {
    1: "172.20.0.0/24",
    2: "172.20.4.0/24",
    3: "172.20.8.0/24"
}


class CerberusNetwork:
    ec2_domain_name_suffix_param = None
    gateway_cidr_block_param = None
    vpc_cidr_block_param = None
    subnet_cidr_block_param_by_az_map = {}
    subnet_cidr_block_for_az1_output = None
    subnet_cidr_block_for_az2_output = None
    subnet_cidr_block_for_az3_output = None

    def __init__(self):
        self.ec2_domain_name_suffix_param = Parameter(
            "ec2ComputeDomainNameSuffix",
            Description="The suffix the region will be appended to for the VPC's internal DNS",
            Type="String"
        )

        self.gateway_cidr_block_param = Parameter(
            "gatewayCidrBlock",
            Description="The internet gateway CIDR block for where traffic is allowed from",
            Type="String",
            MinLength="9",
            MaxLength="20",
            AllowedPattern="[0-9./]*",
            Default="0.0.0.0/0"
        )

        self.vpc_cidr_block_param = Parameter(
            "vpcCidrBlock",
            Description="The VPC's CIDR block for internal IPv4 addressing",
            Type="String",
            MinLength="9",
            MaxLength="20",
            AllowedPattern="[0-9./]*",
            Default="172.20.0.0/20"
        )

        self.subnet_cidr_block_param_by_az_map[1] = Parameter(
            "subnetCidrBlockForAz1",
            Description="Cidr block for subnet in AZ '1'",
            Type="String",
            MinLength="9",
            MaxLength="20",
            AllowedPattern="[0-9./]*",
            Default=VPC_SUBNET_CIDRS[1]
        )

        self.subnet_cidr_block_param_by_az_map[2] = Parameter(
            "subnetCidrBlockForAz2",
            Description="Cidr block for subnet in AZ '2'",
            Type="String",
            MinLength="9",
            MaxLength="20",
            AllowedPattern="[0-9./]*",
            Default=VPC_SUBNET_CIDRS[2]
        )

        self.subnet_cidr_block_param_by_az_map[3] = Parameter(
            "subnetCidrBlockForAz3",
            Description="Cidr block for subnet in AZ '3'",
            Type="String",
            MinLength="9",
            MaxLength="20",
            AllowedPattern="[0-9./]*",
            Default=VPC_SUBNET_CIDRS[3]
        )

        self.subnet_cidr_block_for_az1_output = Output(
            "subnetCidrBlockForAz1",
            Description="Cidr block for subnet in AZ '1'",
            Value=VPC_SUBNET_CIDRS[1]
        )

        self.subnet_cidr_block_for_az2_output = Output(
            "subnetCidrBlockForAz2",
            Description="Cidr block for subnet in AZ '2'",
            Value=VPC_SUBNET_CIDRS[2]
        )

        self.subnet_cidr_block_for_az3_output = Output(
            "subnetCidrBlockForAz3",
            Description="Cidr block for subnet in AZ '3'",
            Value=VPC_SUBNET_CIDRS[3]
        )

    def add_parameters(self, template: Template):
        template.add_parameter(self.ec2_domain_name_suffix_param)
        template.add_parameter(self.gateway_cidr_block_param)
        template.add_parameter(self.vpc_cidr_block_param)

        for subnet_cidr_block in self.subnet_cidr_block_param_by_az_map.values():
            template.add_parameter(subnet_cidr_block)


    def add_outputs(self, template: Template):
        template.add_output(self.subnet_cidr_block_for_az1_output)
        template.add_output(self.subnet_cidr_block_for_az2_output)
        template.add_output(self.subnet_cidr_block_for_az3_output)
