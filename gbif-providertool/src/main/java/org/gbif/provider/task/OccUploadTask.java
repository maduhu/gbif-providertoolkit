package org.gbif.provider.task;

	import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.gbif.provider.model.BBox;
import org.gbif.provider.model.DarwinCore;
import org.gbif.provider.model.OccStatByRegionAndTaxon;
import org.gbif.provider.model.OccurrenceResource;
import org.gbif.provider.model.Region;
import org.gbif.provider.model.Taxon;
import org.gbif.provider.model.dto.ExtensionRecord;
import org.gbif.provider.model.voc.Rank;
import org.gbif.provider.model.voc.RegionType;
import org.gbif.provider.service.DarwinCoreManager;
import org.gbif.provider.service.OccResourceManager;
import org.gbif.provider.service.OccStatManager;
import org.gbif.provider.service.RegionManager;
import org.gbif.provider.service.TaxonManager;
import org.gbif.provider.service.impl.DarwinCoreManagerHibernate;
import org.gbif.provider.util.CacheMap;
import org.springframework.beans.factory.annotation.Autowired;

	/**
	 * Tha main task responsible for uploading raw data into the cache and doing simple preprocessing while iterating through the ImportSource.
	 * Any further secondary postprocessing is done with the help of a second post processing task that this task will automatically schedule once its done.
	 * @author markus
	 *
	 */
	public class OccUploadTask extends ImportTask<DarwinCore, OccurrenceResource> {
		public static final int TASK_TYPE_ID = 1;
		// resource stats
		private OccurrenceResource occResource;
		private int recWithCoordinates;
		private int recWithCountry;
		private int recWithAltitude;
		private int recWithDate;
		private BBox bbox;
		// not stored in resource
		private int numRegions;
		private int numTaxa;
		private Map<Region, Map<Taxon, OccStatByRegionAndTaxon>> occByRegionAndTaxon;
		private CacheMap<String, Taxon> taxonCache = new CacheMap<String, Taxon>(2500);
		private LinkedList<Taxon> newTaxa = new LinkedList<Taxon>();
		private static final List<Rank> higherRanks; 
		  static  
		  {  
		    List<Rank> ranks = new ArrayList<Rank>(Rank.DARWIN_CORE_HIGHER_RANKS);
		    Collections.reverse(ranks);
		    higherRanks = Collections.unmodifiableList(ranks);  
		  }  
		private CacheMap<String, Region> regionCache = new CacheMap<String, Region>(1000);
		private LinkedList<Region> newRegions = new LinkedList<Region>();
		private static final List<RegionType> higherGeography; 
		  static  
		  {  
		    List<RegionType> regionTypes = new ArrayList<RegionType>(RegionType.DARWIN_CORE_REGIONS);
		    regionTypes.add(RegionType.Locality);
		    Collections.reverse(regionTypes);
		    higherGeography = Collections.unmodifiableList(regionTypes);  
		  }  		  
		@Autowired
		private OccStatManager occStatManager;
		@Autowired
		private TaxonManager taxonManager;
		@Autowired
		private RegionManager regionManager;
		@Autowired
		private DarwinCoreManager darwinCoreManager;

		
		@Autowired
		private OccUploadTask(DarwinCoreManager dwcManager, OccResourceManager resourceManager) {
			super(dwcManager, resourceManager);
		}


		@Override
		protected void prepareHandler(OccurrenceResource resource) {
			recWithCoordinates=0;
			recWithCountry=0;
			recWithAltitude=0;
			recWithDate=0;

			bbox=new BBox();
			occByRegionAndTaxon = new HashMap<Region, Map<Taxon, OccStatByRegionAndTaxon>>();
			
			this.occResource = loadResource();
		}
		
		
		@Override
		protected void recordHandler(DarwinCore dwc){
			// extract taxon
			extractTaxon(dwc);
			// extract region
			extractRegion(dwc);
			
			
			// stats
			if(StringUtils.trimToNull(dwc.getCountry())!=null){
				recWithCountry++;
			}
			if(dwc.getDateCollected()!=null){
				recWithDate++;
			}
			if(dwc.getMinimumElevationInMetersAsInteger()!=null){
				recWithAltitude++;
			}
			
			// counts per taxon & region
			Region reg = dwc.getRegion();
			Taxon tax = dwc.getTaxon();
			OccStatByRegionAndTaxon stat;
			Map<Taxon, OccStatByRegionAndTaxon> taxMap;
			if (!occByRegionAndTaxon.containsKey(reg)){
				taxMap = new HashMap<Taxon, OccStatByRegionAndTaxon>();
				occByRegionAndTaxon.put(reg, taxMap);
			}else{
				taxMap = occByRegionAndTaxon.get(reg);				
			}
			if (!taxMap.containsKey(tax)){
				stat = new OccStatByRegionAndTaxon();
				stat.setResource(occResource);
				stat.setRegion(reg);
				stat.setTaxon(tax);
				stat.setNumOcc(1);
				taxMap.put(tax, stat);
			}else{
				stat = taxMap.get(tax);
				stat.incrementNumOcc();
			}
			
		}
		
		

		//
		// TAXA
		//
		private Taxon extractTaxon(DarwinCore dwc) {
			String path = dwc.getTaxonomyPath();
			Taxon taxon=findPersistentTaxon(path);
			if (taxon == null){
				// taxon doesnt exist yet. create it based on ScientificName
				taxon = Taxon.newInstance(occResource);
				taxon.setMpath(path);
				taxon.setScientificName(dwc.getScientificName());
				taxon.setNomenclaturalCode(dwc.getNomenclaturalCode());
				if (dwc.getInfraspecificRank()!=null){
					taxon.setRank(dwc.getInfraspecificRank());
				}

				// need to link the new taxon into the taxonomic hierarchy
				// try to find lowest persistent higher taxon 
				// create new higher taxa as we go up and havent found a persistent one yet
				Taxon parent = null;
				newTaxa.clear();
				boolean persistentParentFound = false;
				// first see if infraspecific epitheton exists. 
				// This means there also is a species which we will use as a higher taxon too
				if (dwc.getInfraspecificEpithet()!=null){
					parent = findPersistentTaxon(dwc.getTaxonomyPath(Rank.Species));
					if (parent==null){
						// cant find species. create new taxon and go further up the ranks
						parent = buildTaxon(dwc, Rank.Species);
						// we cant assign a parent yet, therefor put it on the new taxon stack 
						// and save it later once we reach a persistent taxon or the kingdom
						newTaxa.add(parent);
					}else{
						persistentParentFound=true;
					}
					taxon.setParent(parent);
				}
				if (!persistentParentFound){
					for (Rank rank : higherRanks){
						if (dwc.getHigherTaxonName(rank)==null){
							continue;
						}
						parent = findPersistentTaxon(dwc.getTaxonomyPath(rank));
						if (parent!=null){
							persistentParentFound=true;
						}else{
							parent = buildTaxon(dwc, rank);
							newTaxa.add(parent);
						}
						if (taxon.getParent()==null){
							taxon.setParent(parent);
						}
						if (persistentParentFound){
							break;
						}
					}
				}
				// save new taxa
				if (!persistentParentFound){
					// no persistent taxon found in entire hierarchy.
					// use highest taxon as a new taxonomy root
					parent = newTaxa.removeLast();
					saveTaxon(parent);
				}
				// save all other new taxa if there are any
				Taxon newTaxon;
				while(!newTaxa.isEmpty()){
					newTaxon = newTaxa.removeLast();
					newTaxon.setParent(parent);
					parent = saveTaxon(newTaxon);
				}
				
				// finally save the real taxon linked to the darwin core record
				saveTaxon(taxon);
			}
			
			dwc.setTaxon(taxon);
			return taxon;			
		}

		private Taxon findPersistentTaxon(String mpath){
			if (taxonCache.containsKey(mpath)){
				return taxonCache.get(mpath);
			}else{
				// cache is limited, so we need to check the db too to make sure it doesnt exist
				return taxonManager.getByMaterializedPath(getResourceId(), mpath);
			}
		}
		private Taxon buildTaxon(DarwinCore dwc, Rank rank){
			Taxon taxon = Taxon.newInstance(occResource);
			taxon.setMpath(dwc.getTaxonomyPath(rank));
			taxon.setScientificName(dwc.getHigherTaxonName(rank));
			taxon.setNomenclaturalCode(dwc.getNomenclaturalCode());
			taxon.setRank(rank.toString());
			taxon.setDwcRank(rank);
			return taxon;
		}
		private Taxon saveTaxon(Taxon taxon){
			taxonManager.save(taxon);
			taxonCache.put(taxon.getMpath(), taxon);
			numTaxa++;
			return taxon;
		}


		//
		// REGIONS
		//
		private Region extractRegion(DarwinCore dwc) {			
			Region region = null;
			boolean persistentParentFound=false;
			newRegions.clear();
			for (RegionType regionType : higherGeography){
				if (dwc.getHigherGeographyName(regionType)==null){
					continue;
				}
				region = findPersistentRegion(dwc.getGeographyPath(regionType));
				if (region!=null){
					persistentParentFound=true;
					break;
				}else{
					region = buildRegion(dwc, regionType);
					newRegions.add(region);
				}
			}

			// save new taxa
			if (!persistentParentFound){
				// no persistent region found in entire hierarchy.
				// use highest region as a new geography root region
				region = newRegions.removeLast();
				saveRegion(region);
			}
			// save all other new regions if there are any
			Region newRegion;
			while(!newRegions.isEmpty()){
				newRegion = newRegions.removeLast();
				newRegion.setParent(region);
				region = saveRegion(newRegion);
			}
				
			dwc.setRegion(region);
			return region;			
		}

		private Region findPersistentRegion(String mpath){
			if (regionCache.containsKey(mpath)){
				return regionCache.get(mpath);
			}else{
				// cache is limited, so we need to check the db too to make sure it doesnt exist
				return regionManager.getByMaterializedPath(getResourceId(), mpath);
			}
		}
		private Region buildRegion(DarwinCore dwc, RegionType regionType){
			Region region = Region.newInstance(occResource);
			region.setMpath(dwc.getGeographyPath(regionType));
			region.setLabel(dwc.getHigherGeographyName(regionType));
			region.setType(regionType);
			return region;
		}
		private Region saveRegion(Region region){
			regionManager.save(region);
			regionCache.put(region.getMpath(), region);
			numRegions++;
			return region;
		}
		
		
		
		@Override
		protected void extensionRecordHandler(ExtensionRecord extRec) {
			// see if darwin core record is affected, e.g. geo extension => coordinates
			if (extRec.getExtension().getId().equals(DarwinCoreManagerHibernate.GEO_EXTENSION_ID)){
				// this is the geo extension!
				DarwinCore dwc = darwinCoreManager.get(extRec.getCoreId());
				if (darwinCoreManager.updateWithGeoExtension(dwc, extRec)){
					// update bbox
					bbox.expandBox(dwc.getLocation());
					// potentially transform coordinates
					String geodatum=extRec.getPropertyValue(DarwinCoreManagerHibernate.GEODATUM_PROP);
					// FIXME: dont transform coordinates for now as I have no idea how to get the SpatialReferenceID from the datum alone... 
					darwinCoreManager.save(dwc);
					// increase stats counter
					if (dwc.getLocation().isValid()){
						//FIXME: when multiple extension records for the same dwcore record exist this counter will count all instead of just one!!!
						// might need to do a count via SQL after upload is done ...
						recWithCoordinates++;
						// update Taxon bbox stats
						if (dwc.getTaxon()!=null){
							dwc.getTaxon().expandBox(dwc.getLocation());			
						}
						if (dwc.getRegion()!=null){
							dwc.getRegion().expandBox(dwc.getLocation());			
						}
					}
				}
			}
		}

		@Override
		protected void closeHandler(OccurrenceResource resource){
			// occByRegionAndTaxon
			currentActivity = "Inserting count statistics by region and taxon";
			int occStatCount=0;
			for (Map<Taxon, OccStatByRegionAndTaxon> taxMap : occByRegionAndTaxon.values()){
				for (OccStatByRegionAndTaxon stat : taxMap.values()){
					try {
						occStatManager.save(stat);
						occStatCount++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			log.debug(occStatCount + " occurrence stats by region & taxon inserted");
			
			// create nested set indices
			currentActivity = "Creating taxonomy index";
			taxonManager.buildNestedSet(getResourceId());
			taxonManager.setResourceStats(resource);

			// update resource properties
			resource.setRecWithCoordinates(recWithCoordinates);
			resource.setRecWithCountry(recWithCountry);
			resource.setRecWithAltitude(recWithAltitude);
			resource.setRecWithDate(recWithDate);
			resource.setBbox(bbox);
		}


		@Override
		protected String statusHandler() {
			return String.format("%s taxa, %s regions created", numTaxa, numRegions);
		}


		public int taskTypeId() {
			return TASK_TYPE_ID;
		}

	}
