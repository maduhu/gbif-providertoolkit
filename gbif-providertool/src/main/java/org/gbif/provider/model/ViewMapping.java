/***************************************************************************
* Copyright (C) 2008 Global Biodiversity Information Facility Secretariat.
* All Rights Reserved.
*
* The contents of this file are subject to the Mozilla Public
* License Version 1.1 (the "License"); you may not use this file
* except in compliance with the License. You may obtain a copy of
* the License at http://www.mozilla.org/MPL/
*
* Software distributed under the License is distributed on an "AS
* IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
* implied. See the License for the specific language governing
* rights and limitations under the License.

***************************************************************************/

package org.gbif.provider.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Transient;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.appfuse.model.Address;
import org.appfuse.model.BaseObject;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.MapKey;

/**
 * A mapping between a resource and an extension (incl darwincore itself).
 * The ViewMapping defines the sql statement used to upload data for a certain extension,
 * therefore for every extension there exists a separate sql statement which should be uploaded one after the other.
 * @author markus
 *
 */
@Entity
public class ViewMapping extends BaseObject implements Comparable<ViewMapping> {
	private Long id;
	private DatasourceBasedResource resource;
	private Extension extension;
	private String sourceSql;
	private String sourceFileLocation;
	private ColumnMapping coreIdColumn = new ColumnMapping ();
	private Map<Long, PropertyMapping> propertyMappings = new HashMap<Long, PropertyMapping>();
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO) 
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@ManyToOne
	@JoinColumn(name="resource_id", nullable=true) 
	public DatasourceBasedResource getResource() {
		return resource;
	}
	public void setResource(DatasourceBasedResource resource) {
		this.resource = resource;
	}
	
	@ManyToOne
	public Extension getExtension() {
		return extension;
	}
	public void setExtension(Extension extension) {
		this.extension = extension;
	}
	
	public String getSourceSql() {
		return sourceSql;
	}
	public void setSourceSql(String sourceSql) {
		if (sourceSql != null){
			this.sourceFileLocation=null;
		}
		this.sourceSql = sourceSql;
	}
	
	public String getSourceFileLocation() {
		return sourceFileLocation;
	}
	public void setSourceFileLocation(String sourceFileLocation) {
		this.sourceFileLocation = sourceFileLocation;
	}
	@Transient
	public File getSourceFile() {
		File file = null;
		if (this.sourceFileLocation != null){
			file = new File(this.sourceFileLocation);
		}
		return file;
	}	
	public void setSourceFile(File file) {
		if (file != null){
			this.sourceSql=null;
		}
		this.sourceFileLocation = file.getAbsolutePath();
	}	
	/**
	 * Index of resultset column for the local or global identifier for a core-record. 
	 * Acts as the primary key for the core mapping or the foreign key for extension mappings
	 * @return
	 */
	@Embedded
	@AttributeOverrides( {
        @AttributeOverride(name="columnName", column = @Column(name="localid_col", length=64)),
	} )
	public ColumnMapping getCoreIdColumn() {
		return coreIdColumn;
	}
	public void setCoreIdColumn(ColumnMapping coreIdColumn) {
		this.coreIdColumn = coreIdColumn;
	}

	
	@OneToMany(mappedBy="viewMapping", cascade=CascadeType.ALL)
	@MapKey(columns = @Column(name = "property_id"))
	public Map<Long, PropertyMapping> getPropertyMappings() {
		return propertyMappings;
	}
	public void setPropertyMappings(Map<Long, PropertyMapping> propertyMappings) {
		this.propertyMappings = propertyMappings;
	}
	
	public void addPropertyMapping(PropertyMapping propertyMapping) {
		propertyMapping.setViewMapping(this);
		propertyMappings.put(propertyMapping.getProperty().getId(), propertyMapping);
	}
	
	@Transient
	public boolean hasMappedProperty(ExtensionProperty property) {
		if (propertyMappings.containsKey(property.getId())){
			return true;
		}
		return false;
	}	

	@Transient
	public PropertyMapping getMappedProperty(ExtensionProperty property) {
		return propertyMappings.get(property.getId());
	}	
	
	@Transient
	public List<ExtensionProperty> getMappedProperties() {
		List<ExtensionProperty> props = new ArrayList<ExtensionProperty>();
		for (PropertyMapping pm : propertyMappings.values()){
			props.add(pm.getProperty());
		}
		return props;
	}		
	/**
	 * Natural sort order is resource, then extension
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	public int compareTo(ViewMapping view) {
		if (resource != null){
			int resCmp = resource.compareTo(view.resource);
			int extComp = (extension == null ? extComp = -1 : extension.compareTo(view.extension)); 
			return (resCmp != 0 ? resCmp : extComp);			
		}else{
			return -1;
		}
	}
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
        int result = 17;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        result = 31 * result + (resource != null ? (resource.getId() != null ? resource.getId().hashCode() : 0) : 0);
        //result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (propertyMappings != null ? propertyMappings.hashCode() : 0);
        result = 31 * result + (sourceSql != null ? sourceSql.hashCode() : 0);
        result = 31 * result + (sourceFileLocation != null ? sourceFileLocation.hashCode() : 0);
        result = 31 * result + (coreIdColumn != null ? coreIdColumn.hashCode() : 0);
        return result;
	}
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new ToStringBuilder(this)
		.append("id", this.id)
		.append("sourceSql", this.sourceSql)
		.append("sourceFileLocation", this.sourceFileLocation)
		.append("coreIdColumn", this.coreIdColumn)
		.append("extension", this.extension)
		.toString();
	}
	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViewMapping)) {
            return false;
        }

        final ViewMapping vm = (ViewMapping) o;

        return this.hashCode() == vm.hashCode();
	}

	
}
