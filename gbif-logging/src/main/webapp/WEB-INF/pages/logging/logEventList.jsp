<%@ include file="/common/taglibs.jsp"%>

<head>
    <title><s:text name="logEventList.title"/></title>
    <meta name="heading" content="<s:text name='logEventList.heading'/>"/>
    <meta name="menu" content="LogEventMenu"/>
	<s:head theme="ajax" debug="true"/>
</head>


<style>
	table.log {
		white-space: nowrap;
	}
	table.log td.timestamp {
		color: #999999;
	}
	table.log td {
		padding-left: 5px;
		padding-right: 5px;
	}
	table.log tr.trace {
		color: #999999;
	}
	table.log tr.trace_over {
		color: #999999;
		background: #ffffe6;
	}
	table.log tr.debug {
		color: #999999;
	}
	table.log tr.debug_over {
		color: #999999;
		background: #ffffe6;
	}
	table.log tr.info {
		color: #00cc00;
	}
	table.log tr.info_over {
		color: #00cc00;
		background: #ffffe6;
	}
	table.log tr.warn {
		color: #990000;
	}
	table.log tr.warn_over {
		color: #660000;
		background: #ffffe6;
	}
	table.log tr.error {
		color: #ff0000;
	}
	table.log tr.error_over {
		color: #ff0000;
		background: #ffffe6;
	}
	table.log tr.fatal {
		color: #ff0000;
	}
	table.log tr.fatal_over {
		color: #ff0000;
		background: #ffffe6;
	}
	table.log div.hide {
		display:none;
	}
</style>
<p>
	<a href="javascript:setLevel(1)">verbose</a> &nbsp;&nbsp;&nbsp; 
	<a href="javascript:setLevel(3)">normal</a> &nbsp;&nbsp;&nbsp; 
	<a href="javascript:setLevel(4)">minimal</a>
</p> 
<table class="log">
	<tbody id="log">
	</tbody>
</table>

<s:url id="smdUrl" namespace="/nodecorate" action="LogEventList"/>

<script type="text/javascript">
    //load dojo RPC
    // make sure struts2 ajax theme is enabled
    dojo.require("dojo.rpc.*");
    
    //create service object(proxy) using SMD (generated by the json result)
    var service = new dojo.rpc.JsonService("${smdUrl}");
	
	var max=0;
	var level=2;
	<c:choose>
  		<c:when test="${not empty param.sourceId}">
			var sourceId   = ${param.sourceId};	
			var sourceType = ${param.sourceType};	
			var groupId = -1;	
			var userId  = -1;	
  		</c:when>
  		<c:when test="${not empty param.groupId}">
			var sourceId   = -1;	
			var sourceType = -1;	
			var groupId =  ${param.groupId};	
			var userId  = -1;	
 		 </c:when>
  		<c:when test="${not empty param.userId}">
			var sourceId   = -1;	
			var sourceType = -1;	
			var groupId = -1;	
			var userId  = ${param.userId};	
 		 </c:when>
 		 <c:otherwise>
			var sourceId   = -1;	
			var sourceType = -1;	
			var groupId = -1;	
			var userId  = -1;	
 		 </c:otherwise>
 	</c:choose>
	
	var timer;

   //function called when remote method returns
	var callback = function(bean) {
		var tbody = document.getElementById("log");
		var i=0;
        for (i=0; i<bean.length; i++) {
    		var tr = document.createElement("TR");

			if (bean[i].level == 1) {
				tr.className="trace";
			} else if (bean[i].level == 2) {
				tr.className="debug";
			} else if (bean[i].level == 3) {
				tr.className="info";
			} else if (bean[i].level == 4) {
				tr.className="warn";
			} else if (bean[i].level == 5) {
				tr.className="error";
			} else if (bean[i].level == 6) {
				tr.className="fatal";
			} 
				
			var td1 = document.createElement("TD");
			td1.className = "timestamp";
			var td1Text = document.createTextNode(bean[i].timestamp);
			td1.appendChild(td1Text);
			tr.appendChild(td1);
			
			//var td2 = document.createElement("TD");
			//td2.innerHTML = "<a href=javascript:alert('1')>*</a>";
			//tr.appendChild(td2);

			var td3 = document.createElement("TD");
			td3.innerHTML = bean[i].message;
			
			/*
			if (bean[i].exceptionDetail) {
				td3.innerHTML += "<div class='hide'>";
				td3.innerHTML += bean[i].exceptionDetail.message;
				td3.innerHTML +="</div>";
			}
			*/
			
			
			if (bean[i].exceptionDetail) {
				var detail = "<div class='hide'>";
				detail += bean[i].exceptionDetail.message;
				var j=0;
				for (j=0; j < bean[i].exceptionDetail.causeDetails.length; j++) {
					detail += "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
					detail += bean[i].exceptionDetail.causeDetails[j].className;
					detail += "." + bean[i].exceptionDetail.causeDetails[j].methodName;
					detail += "(" + bean[i].exceptionDetail.causeDetails[j].fileName;
					detail += ":" + bean[i].exceptionDetail.causeDetails[j].lineNumber + ")";
				}
				
				detail += "</div>";
				td3.innerHTML += detail;
			}
			tr.appendChild(td3);
			
    		if (tbody.firstChild) {
				var firstTR = tbody.firstChild;
				tbody.insertBefore(tr, firstTR);
    		
        	} else {
				tbody.appendChild(tr);
    		}
			
			if (bean[i].exceptionDetail) {
       			tr.onmouseover = function() { previousClass=this.className;this.className+='_over' };
	       	 	tr.onmouseout = function() { this.className=previousClass };
    	   	 	tr.onclick = function() { 
    				var divs = this.getElementsByTagName("div");
    				for (i=0; i < divs.length; i++) {
    					if (divs[i].className=="hide") divs[i].className="";
    					else divs[i].className="hide";
    				}
       	 		};
       	 	}
       	 	
       	 	

    		
    		max = bean[i].id;
    	}
    	timer = setTimeout("getLog(" + max + ")", 3000);
    };
    
	function setLevel(newLevel) {
			document.getElementById("log").innerHTML = "";
			level=newLevel;
			clearTimeout(timer);
			getLog(0);
	}
	
	function getLog(id) {
	    // execute remote method
		if (sourceId>-1) {
			var defered = service.getLatestForSource(sourceId,sourceType,id,level);
		    //attach callback to defered object
	    	defered.addCallback(callback);
		} else 	if (groupId>-1) {
	    	var defered = service.getLatestForGroup(groupId,id,level);
		    //attach callback to defered object
	    	defered.addCallback(callback);
		} else 	if (userId>-1) {
	    	var defered = service.getLatestForUser(userId,id,level);
		    //attach callback to defered object
	    	defered.addCallback(callback);
		} else {
	    	var defered = service.getAllLatest(id,level);
		    //attach callback to defered object
	    	defered.addCallback(callback);
		}
    }
    
    
    setLevel(3);
</script>
