/*
 * Copyright 2009 GBIF.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.provider.webapp.action.admin;

import static org.apache.commons.lang.StringUtils.trimToNull;

import org.gbif.provider.model.ResourceMetadata;
import org.gbif.provider.service.RegistryManager;
import org.gbif.provider.util.AppConfig;
import org.gbif.provider.webapp.action.BasePostAction;
import org.gbif.registry.api.client.GbrdsOrganisation;
import org.gbif.registry.api.client.Gbrds.OrgCredentials;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO: Documentation.
 * 
 */
public class ConfigOrgAction extends BasePostAction {
  @Autowired
  private RegistryManager registryManager;
  private String organisationKey;

  public AppConfig getConfig() {
    return this.cfg;
  }

  public String getRegistryNodeUrl() {
    return AppConfig.getRegistryNodeUrl();
  }

  public String getRegistryOrgUrl() {
    return AppConfig.getRegistryOrgUrl();
  }

  @Override
  public String read() {
    check();
    return SUCCESS;
  }

  public String register() {
    if (cfg.isOrgRegistered()) {
      saveMessage(getText("register.org.already"));
      return SUCCESS;
    }
    ResourceMetadata rm = cfg.getOrg();
    GbrdsOrganisation go = registryManager.buildGbrdsOrganisation(rm).build();
    OrgCredentials creds = registryManager.createGbrdsOrganisation(go).getResult();
    if (creds != null) {
      saveMessage(getText("register.org.success"));
      cfg.getOrg().setUddiID(creds.getKey());
      cfg.setOrgPassword(creds.getPassword());
      cfg.save();
    } else {
      saveMessage(getText("register.org.problem"));
    }
    return SUCCESS;
  }

  @Override
  public String save() {
    if (!check()) {
      return SUCCESS;
    }
    if (cfg.isOrgRegistered()) {
      ResourceMetadata rm = cfg.getOrg();
      GbrdsOrganisation go = registryManager.buildGbrdsOrganisation(rm).build();
      OrgCredentials creds = getCreds();
      if (registryManager.updateGbrdsOrganisation(go, creds).getResult()) {
        saveMessage(getText("registry.updated"));
        cfg.save();
      } else {
        saveMessage(getText("registry.problem"));
      }
    }
    return SUCCESS;
  }

  public void setConfig(AppConfig cfg) {
    this.cfg = cfg;
  }

  public void setOrganisationKey(String organisationKey) {
    this.organisationKey = !check() ? null : trimToNull(organisationKey);
  }

  private boolean check() {
    boolean result = true;

    // Checks if a GBIF Resource representing this IPT instance has already been
    // created in the GBRDS. If it has, then the GBIF Organisation associated
    // with that GBIF Resource cannot be changed and the user is notified.
    if (cfg.isIptRegistered()) {
      saveMessage(getText("register.org.ipt.already"));
    }

    // Checks if the GBIF Organisation associated with this IPT instance has
    // already been created in the GBRDS. If not, the user is notified.
    if (!cfg.isOrgRegistered()) {
      saveMessage(getText("config.check.orgRegistered"));
      result = false;
    }

    // Checks organisation password value:
    if (trimToNull(cfg.getIptOrgPassword()) == null) {
      saveMessage(getText("config.check.orgPassword"));
      result = false;
    }

    // Checks organisation credentials:
    OrgCredentials creds = getCreds();
    if (creds == null
        || !registryManager.validateCredentials(creds).getResult()) {
      saveMessage(getText("config.check.orgLogin"));
      result = false;
    }

    return result;
  }

  private OrgCredentials getCreds() {
    try {
      return OrgCredentials.with(cfg.getOrg().getUddiID(), cfg.getOrgPassword());
    } catch (Exception e) {
      return null;
    }
  }
}