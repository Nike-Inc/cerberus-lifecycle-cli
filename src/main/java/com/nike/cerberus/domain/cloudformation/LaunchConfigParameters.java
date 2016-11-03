package com.nike.cerberus.domain.cloudformation;

/**
 * Interface implemented by stack parameter POJOs that have launch configuration parameters.
 */
public interface LaunchConfigParameters extends TagParameters, SslConfigParameters {

    LaunchConfigParametersDelegate getLaunchConfigParameters();
}
