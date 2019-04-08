/*
 * Copyright (c) 2016 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.domain.environment;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Container for data needed by Cerberus.
 */
@Singleton
public class EnvironmentData {

    private String environmentName;

    private String adminIamRoleArn;

    private String cmsIamRoleArn;

    private String rootIamRoleArn;

    private String databasePassword;

    private LinkedList<CertificateInformation> certificateInfoList = new LinkedList<>();

    private Map<Regions, RegionData> regionData = new HashMap<>();;

    private boolean auditLoggingEnabled;

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getAdminIamRoleArn() {
        return adminIamRoleArn;
    }

    public void setAdminIamRoleArn(String adminIamRoleArn) {
        this.adminIamRoleArn = adminIamRoleArn;
    }

    public String getRootIamRoleArn() {
        return rootIamRoleArn;
    }

    public void setRootIamRoleArn(String rootIamRoleArn) {
        this.rootIamRoleArn = rootIamRoleArn;
    }

    public String getCmsIamRoleArn() {
        return cmsIamRoleArn;
    }

    public void setCmsIamRoleArn(String cmsIamRoleArn) {
        this.cmsIamRoleArn = cmsIamRoleArn;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public LinkedList<CertificateInformation> getCertificateData() {
        return certificateInfoList;
    }

    public boolean isAuditLoggingEnabled() {
        return auditLoggingEnabled;
    }

    public EnvironmentData setAuditLoggingEnabled(boolean auditLoggingEnabled) {
        this.auditLoggingEnabled = auditLoggingEnabled;
        return this;
    }

    public void addNewCertificateData(CertificateInformation certificateData) {
        getCertificateData().add(certificateData);
    }

    public void removeCertificateInformationByName(String identityManagementCertificateName) {
        List<CertificateInformation> certsInfoToBeDeleted = certificateInfoList.stream()
                .filter(certificateInformation ->
                        certificateInformation.getCertificateName().equals(identityManagementCertificateName))
                .collect(Collectors.toList());

        certificateInfoList.removeAll(certsInfoToBeDeleted);
    }

    public Map<Regions, RegionData> getRegionData() {
        return regionData;
    }

    public void addRegionData(Regions region, RegionData rData) {
        regionData.put(region, rData);
    }

    /**
     * @return List of all the regions that are configured for storing config.
     */
    @JsonIgnore
    public List<Regions> getConfigRegions() {
        List<Regions> configRegions = new LinkedList<>();
        regionData.forEach((region, rData) -> {
            if (rData.getConfigCmkArn().isPresent()
                    && rData.getConfigBucket().isPresent()) {
                configRegions.add(region);
            }
        });
        return configRegions;
    }

    @JsonIgnore
    public List<Regions> getCmsRegions() {
        List<Regions> regions = new LinkedList<>();
        regionData.forEach((region, rData) -> {
            if (rData.getManagementServiceCmkArn().isPresent()
                && rData.getConfigBucket().isPresent()) {
                regions.add(region);
            }
        });
        return regions;
    }

    @JsonIgnore
    public Regions getPrimaryRegion() {
        return regionData.entrySet().stream()
                .filter(entry -> entry.getValue().isPrimary()).findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to find primary region in region specific state"))
                .getKey();
    }

    public List<String> getManagementServiceCmkArns() {
        return regionData.entrySet().stream()
                .filter(entry -> entry.getValue().getManagementServiceCmkArn().isPresent())
                .map(entry -> entry.getValue().getManagementServiceCmkArn().get())
                .collect(Collectors.toList());
    }
}
