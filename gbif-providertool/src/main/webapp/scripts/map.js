var map, polygonControl, polyOptions, boundingBox;

function loadMap() {
	map = new OpenLayers.Map('map');
          
	var gbif = new OpenLayers.Layer.WMS( "background",
               "http://geoserver.gbif.org/wms?", {layers: "gbif:countries", version: "1.0.0", format: "image/png", bgcolor:"0x7391AD"} );
    
    //add the polygon layer
    var polygonLayer = new OpenLayers.Layer.Vector("Polygon Layer");

    if ($('#bbox_left').value!="" && $('#bbox_bottom').value!="" && $('#bbox_right').value !=null && $('#bbox_top').value!=null){
	    var bounds = new OpenLayers.Bounds($('#bbox_left').value, $('#bbox_bottom').value, $('#bbox_right').value, $('#bbox_top').value);
	    boundingBox = new OpenLayers.Feature.Vector(bounds.toGeometry());
	    polygonLayer.addFeatures(boundingBox);        
    }
    
    map.addLayers([gbif, polygonLayer]);
    map.addControl( new OpenLayers.Control.MousePosition() );
    
    map.zoomToMaxExtent();
    //map.setCenter(new OpenLayers.LonLat(0, 0), 1);
    map.zoomToMaxExtent();
    
    polyOptions = {sides: 4};
    polygonControl = new OpenLayers.Control.DrawFeature(
                                    polygonLayer,
                                    OpenLayers.Handler.RegularPolygon,
                                    {handlerOptions: polyOptions});

	polygonControl.featureAdded = function mapEvent(geometry){
		if(boundingBox!=null){
			polygonLayer.removeFeatures([boundingBox]);
		}
		//removes features
		boundingBox = geometry;
		$('#bbox_top').value = geometry.geometry.getBounds().top;
		$('#bbox_bottom').value = geometry.geometry.getBounds().bottom;
		$('#bbox_left').value = geometry.geometry.getBounds().left;
		$('#bbox_right').value = geometry.geometry.getBounds().right;
	};

    polygonControl.handler.setOptions({snapAngle: parseFloat(0)});
    polygonControl.handler.setOptions({irregular: true});
    map.addControl(polygonControl);
    polygonControl.activate();
}

function clearBounds(){
	$('#bbox_top').value = "";
	$('#bbox_bottom').value = "";
	$('#bbox_left').value = "";
	$('#bbox_right').value = "";
	polygonLayer.destroyFeatures();
}
