from troposphere import Parameter, Ref, Tags, Template
from troposphere.ec2 import Tag
from troposphere.autoscaling import Tag as AutoScalingTag


class CerberusTags:
    tag_map = {}

    def __init__(self):
        self.tag_map["tagName"] = Parameter(
            "tagName",
            Description="Name assigned to the stack. Format: {appGroup}-{appName}",
            Type="String"
        )

        self.tag_map["tagEmail"] = Parameter(
            "tagEmail",
            Description="E-mail address for group or person responsible for the stack.",
            Type="String"
        )

        self.tag_map["tagClassification"] = Parameter(
            "tagClassification",
            Description="Denotes which category of Data Classification the instance is grouped under.",
            Type="String",
            Default="Gold"
        )

        self.tag_map["tagCostcenter"] = Parameter(
            "tagCostcenter",
            Description="Represents the Cost Center associated with the team/project.",
            Type="String"
        )

    def add_tag_parameters(self, template: Template):
        for param in self.tag_map.values():
            template.add_parameter(param)

    def get_tags_as_list(self):
        return [
            Tag("Name", Ref(self.tag_map["tagName"])),
            Tag("email", Ref(self.tag_map["tagEmail"])),
            Tag("classification", Ref(self.tag_map["tagClassification"])),
            Tag("costcenter", Ref(self.tag_map["tagCostcenter"]))
        ]

    def get_gateway_tags_as_list(self):
        return [
            Tag("Name", "cloudfront"),
            Tag("AutoUpdate", "true"),
            Tag("email", Ref(self.tag_map["tagEmail"])),
            Tag("classification", Ref(self.tag_map["tagClassification"])),
            Tag("costcenter", Ref(self.tag_map["tagCostcenter"]))
        ]

    def get_autoscaling_tags_as_list(self):
        return [
            AutoScalingTag("Name", Ref(self.tag_map["tagName"]), True),
            AutoScalingTag("email", Ref(self.tag_map["tagEmail"]), True),
            AutoScalingTag("classification", Ref(self.tag_map["tagClassification"]), True),
            AutoScalingTag("costcenter", Ref(self.tag_map["tagCostcenter"]), True)
        ]

    def get_tags(self):
        return Tags(
            Name=Ref(self.tag_map["tagName"]),
            email=Ref(self.tag_map["tagEmail"]),
            classification=Ref(self.tag_map["tagClassification"]),
            costcenter=Ref(self.tag_map["tagCostcenter"])
        )
