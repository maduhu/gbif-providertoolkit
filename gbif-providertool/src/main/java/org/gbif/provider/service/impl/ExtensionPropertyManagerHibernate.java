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
package org.gbif.provider.service.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.gbif.provider.model.Extension;
import org.gbif.provider.model.ExtensionProperty;
import org.gbif.provider.service.ExtensionPropertyManager;
import org.gbif.provider.tapir.ParseException;
import org.gbif.provider.tapir.filter.BooleanOperator;
import org.gbif.provider.tapir.filter.ComparisonOperator;
import org.gbif.provider.tapir.filter.Filter;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Query;

/**
 * TODO: Documentation.
 * 
 */
public class ExtensionPropertyManagerHibernate extends
    GenericManagerHibernate<ExtensionProperty> implements
    ExtensionPropertyManager {

  public ExtensionPropertyManagerHibernate() {
    super(ExtensionProperty.class);
  }

  public ExtensionProperty getCorePropertyByName(String name) {
    return (ExtensionProperty) getSession().createQuery(
        "select p FROM ExtensionProperty p join p.extension e WHERE p.name=:name and e.core=true) ").setParameter(
        "name", name).uniqueResult();
  }

  public ExtensionProperty getCorePropertyByQualName(String qName) {
    // use ExtensionProperty to parse qualname intp pieces for querying
    ExtensionProperty prop = new ExtensionProperty(qName);
    return (ExtensionProperty) getSession().createQuery(
        "select p FROM ExtensionProperty p join p.extension e WHERE p.name=:name and (p.namespace=:namespace or p.namespace=:namespace2) and e.core=true) ").setParameter(
        "name", prop.getName()).setParameter("namespace", prop.getNamespace()).setParameter(
        "namespace2", prop.getNamespace() + "/").uniqueResult();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.gbif.provider.service.ExtensionPropertyManager#getProperty(org.gbif
   * .provider.model.Extension, java.lang.String)
   */
  public ExtensionProperty getProperty(Extension extension, String name) {
    checkNotNull(extension, "Extension is null");
    checkNotNull(name, "Extension property name is null");
    checkArgument(name.length() > 0, "Extension property name is empty");
    ExtensionProperty prop = new ExtensionProperty(name);
    String query = "SELECT p " + "FROM ExtensionProperty p "
        + "JOIN p.extension e " + "WHERE p.extension.id=:eid "
        + "AND p.name=:name "
        + "AND (p.namespace=:namespace or p.namespace=:namespace2)) ";
    if (extension.isCore()) {
      query += "AND e.core = true";
    }
    Query q = getSession().createQuery(query).setParameter("eid",
        extension.getId()).setParameter("name", prop.getName()).setParameter(
        "namespace", prop.getNamespace()).setParameter("namespace2",
        prop.getNamespace() + "/");
    return (ExtensionProperty) q.uniqueResult();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.gbif.provider.service.ExtensionPropertyManager#getPropertyByQualName
   * (java.lang.String)
   */
  public ExtensionProperty getPropertyByQualName(String qName) {
    ExtensionProperty prop = new ExtensionProperty(qName);
    return (ExtensionProperty) getSession().createQuery(
        "select p FROM ExtensionProperty p join p.extension e WHERE p.name=:name and (p.namespace=:namespace or p.namespace=:namespace2)) ").setParameter(
        "name", prop.getName()).setParameter("namespace", prop.getNamespace()).setParameter(
        "namespace2", prop.getNamespace() + "/").uniqueResult();
  }

  public Set<ExtensionProperty> lookupFilterCoreProperties(Filter filter)
      throws ParseException {
    Set<ExtensionProperty> props = new HashSet<ExtensionProperty>();
    for (BooleanOperator op : filter) {
      if (ComparisonOperator.class.isAssignableFrom(op.getClass())) {
        ComparisonOperator cop = (ComparisonOperator) op;
        ExtensionProperty prop = cop.getProperty();
        // try to lookup by qualname
        ExtensionProperty persistentProp = this.getCorePropertyByQualName(prop.getQualName());
        if (persistentProp == null) {
          // nothing found? then try via alias name
          persistentProp = this.getCorePropertyByName(prop.getName());
        }
        if (persistentProp == null) {
          // still nothing found? cant deal with this filter then. throw
          // exception
          throw new ParseException("Filter contains the unknown concept "
              + prop.getQualName());
        }
        cop.setProperty(persistentProp);
        props.add(persistentProp);
      }
    }
    return props;
  }
}
