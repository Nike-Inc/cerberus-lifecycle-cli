package com.nike.cerberus.command.validator;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Sets;
import com.nike.cerberus.domain.environment.StackName;

import java.util.Set;

/**
 * Validates that the stack name specified actually requires a certificate to be uploaded.
 */
public class UploadCertFilesStackNameValidator implements IValueValidator<StackName> {

    private final Set<StackName> stackNamesWithCerts = Sets.newHashSet(
            StackName.VAULT, StackName.CONSUL, StackName.CMS, StackName.GATEWAY);

    @Override
    public void validate(final String name, final StackName stackName) throws ParameterException {
        if (!stackNamesWithCerts.contains(stackName)) {
            throw new ParameterException("Stack specified doesn't require a certificate to be uploaded.");
        }
    }
}
