package com.nike.cerberus.operation.composite;

import com.google.common.collect.Lists;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.composite.CreateCmsResourcesForRegionCommand;
import com.nike.cerberus.command.core.*;
import com.nike.cerberus.command.rds.CreateDatabaseCommand;

import java.util.List;

public class CreateCmsResourcesForRegionOperation extends CompositeOperation<CreateCmsResourcesForRegionCommand> {

  @Override
  protected List<ChainableCommand> getCompositeCommandChain(CreateCmsResourcesForRegionCommand compositeCommand) {
    return Lists.newArrayList(
      new ChainableCommand(new CreateVpcCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new CreateSecurityGroupsCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new WhitelistCidrForVpcAccessCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new CreateDatabaseCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new CreateLoadBalancerCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new UpdateCmsConfigCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new CreateCmsClusterCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new CreateAlbLogAthenaDbAndTableCommand(), compositeCommand.getStackRegion()),
      new ChainableCommand(new CreateRoute53Command(), compositeCommand.getStackRegion())
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRunnable(CreateCmsResourcesForRegionCommand command) {
    // Always return true and depend on isRunnable of the chained commands to skip commands that cannot be re-ran
    // we want this command to be able to be run more than once to complete an environment for example say temporary
    // aws creds expire half way through environment creation
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean skipOnNotRunnable() {
    // Since we always return true for isRunnable on this command sub of the sub commands may have already been run
    // we want this command to be able to be run more than once to complete an environment for example say temporary
    // aws creds expire half way through environment creation
    return true;
  }
}
