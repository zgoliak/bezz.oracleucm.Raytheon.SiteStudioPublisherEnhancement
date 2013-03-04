package com.bezzotech.oracleucm.raytheon.SiteStudioPublisherEnhancment;

import intradoc.common.ExecutionContext;
import intradoc.common.Report;
import intradoc.common.ServiceException;
import intradoc.common.StringUtils;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.DataResultSet;
import intradoc.data.ResultSet;
import intradoc.data.Workspace;
import intradoc.server.Service;
import intradoc.shared.FilterImplementor;
import intradoc.shared.SharedObjects;

import java.util.Enumeration;
import java.util.Vector;

import sitestudio.SSHierarchyServiceHandler;

public class SiteStudioPublisherEnhancmentFilters implements FilterImplementor {

//	protected SSPEUtils utils;
	protected String pubStatField = SharedObjects.getEnvironmentValue( "PublicationStatusMetadataField" );

	public int doFilter( Workspace ws, DataBinder db, ExecutionContext ec )
			throws DataException, ServiceException {
		Report.trace( "bezzotechraytheon", "Entering SiteStudioPublisherEnhancmentFilters.doFilter", null );
		if( ec == null ) {
			System.out.println( "Plugin filter called without a context." );
			return FilterImplementor.CONTINUE;
		}
		Object obj = ec.getCachedObject( "filterParameter" );
		if( obj == null || !( obj instanceof String ) ) {
			System.out.println( "Plugin filter called without filter parameter." );
			return FilterImplementor.CONTINUE;
		}
		String s = ( String )obj;
//		utils = new SSPEUtils();

		//CHECKIN_NEW and CHECKIN_SEL
		if( s.equals( "SSPE_validateStandard" ) ) {
			if( db.getLocal( "IdcService" ).startsWith( "CHECKIN_" ) ) {
				Report.trace( "bezzotechraytheon", "Performing SSPE_validateStandard filter", null );
				String objType = db.getLocal( "xWebsiteObjectType" );
				if( objType == null ) return CONTINUE;
				else if( objType.equals( "Data File" ) || objType.equals( "Native Document" ) )
					method2( ws, db );
				Report.trace( "bezzotechraytheon", "Updated binder, set Publication Status to " +
						db.getLocal( pubStatField ) + " and Security Group to " +
						db.getLocal( "dSecurityGroup" ) + " based on depends on " +
						db.getLocal( "xDependsOnWebsiteSection" ), null );
			}
		} else
		// SS_ADD_NODE
		if( s.equals( "SSPE_SiteStudioAddNode" ) ) {
			Report.trace( "bezzotechraytheon", "Performing SSPE_SiteStudioAddNode filter", null );
			int i = 0;
			while((s = db.getLocal("property" + i)) != null) {
				if( s.equals( "PublicationStatus" ) ) break;
				i++;
			}
			String siteID = db.getLocal( "siteId" ); 
			String nodeID = db.getLocal( "newNodeId" ); 
			String nodeProperty = getValidParentPublicationStatus( ws, siteID, nodeID );
			db.putLocal( "property" + i, "PublicationStatus" );
			db.putLocal( "value" + i, calculatePublicationStatus( nodeProperty ) );
			Report.trace( "bezzotechraytheon", "Updating binder, property at index " + i + " (" +
					db.getLocal( "property" + i ) + ") will be set to " + db.getLocal( "value" + i ), null);
		} else
		// SS_SET_NODE_PROPERTY && SS_SWITCH_REGION_ASSOCIATION
		if( s.equals( "SSPE_SiteStudioPreSetNodeProperty" ) ) {
			Report.trace( "bezzotechraytheon", "Performing SSPE_SiteStudioPreSetNodeProperty filter", null );
			method3( db, ws );
		}
		return CONTINUE;
	}

	protected void method2( Workspace ws, DataBinder db ) throws DataException, ServiceException {
		String dependsOn = db.getLocal( "xDependsOnWebsiteSection" ); // Depends on Metadata
		boolean global = StringUtils.convertToBool( db.getLocal( "xGlobalWebsiteObject" ), false );

		// If content is marked as a Global asset set to release and be done
		if( global ) {
			db.putLocal( pubStatField, "release" );
			db.putLocal( "dSecurityGroup", "public" );
			return;
		}

		String siteID = db.getLocal( "xWebsites" ); // SiteStudio Website
		String nodeProperty = null;
		String targetNode = null;

		// Check the content for metadata dependancies and determine if a valid status is available
		Report.trace( "bezzotechraytheon", "Checking depends on: " + dependsOn, null );
		if( dependsOn != null && !dependsOn.equals( "" ) ) {
			dependsOn = dependsOn.replace( siteID + ":", "" );
			nodeProperty = getNodeProperty( ws, siteID, dependsOn, "PublicationStatus" );
			if( isValidPublicationStatus( nodeProperty ) ) {
				db.putLocal( pubStatField, nodeProperty );
				db.putLocal( "dSecurityGroup", calculateSecurityGroup( nodeProperty ) );
				return;
			}
			targetNode = dependsOn;
		}

		String nodeID = db.getLocal( "xWebsiteSection" ); // SiteStudio Website Section
		Report.trace( "bezzotechraytheon", "Checking website section: " + nodeID, null );
		if( targetNode == null && ( nodeID != null && !nodeID.equals( "" ) ) ) {
			db.putLocal( "xDependsOnWebsiteNode", nodeID );
			nodeID = nodeID.replace( siteID + ":", "" );
			nodeProperty = getNodeProperty( ws, siteID, nodeID, "PublicationStatus" );
			if( isValidPublicationStatus( nodeProperty ) ) {
				db.putLocal( pubStatField, nodeProperty );
				db.putLocal( "dSecurityGroup", calculateSecurityGroup( nodeProperty ) );
				return;
			}
			targetNode = nodeID;
		}

		Report.trace( "bezzotechraytheon", "Checking target node: " + targetNode, null );
		if( targetNode != null ) {
			nodeProperty = getValidParentPublicationStatus( ws, siteID, targetNode );
			db.putLocal( pubStatField, nodeProperty );
			db.putLocal( "dSecurityGroup", calculateSecurityGroup( nodeProperty ) );
			return;
		}
	}

	protected void method3( DataBinder db, Workspace ws ) throws DataException, ServiceException {
		if( db.getLocal( "SiteStudio_property" ).equals( "PublicationStatus" ) ) {
			Report.trace( "bezzotechraytheon", "User updated Publication Status, performing updates", null );
			String pubStat = db.getLocal( "SiteStudio_value" );
			String siteID = db.getLocal( "SiteStudio_siteId" );
			String nodeID = db.getLocal( "SiteStudio_nodeId" );
			if( pubStat.equals( "inherit" ) ) {
				pubStat = getValidParentPublicationStatus( ws, siteID, nodeID );
			}
			Report.trace( "bezzotechraytheon", "Found publication status: " + pubStat, null );
			DataBinder params = new DataBinder();
			params.putLocal( "dependsOn", siteID + ":" + nodeID );
			ResultSet rset = ws.createResultSet( "QdependsOnWebsiteNode", params );
			DataResultSet drset = new DataResultSet();
			drset.copy( rset );
			Report.debug( "bezzotechraytheon", drset.isEmpty() ? "No dependent content found" :
					"Found " + drset.getNumRows() + " dependent content items", null );
			if( !drset.isEmpty() ) {
				for( int i = 0; i < drset.getNumRows(); i++ ) {
					drset.setCurrentRow( i );
					DataBinder docinfoParams = new DataBinder();
					docinfoParams.putLocal( "dID", drset.getStringValueByName( "dID" ) );
					ResultSet docinfoRSet = ws.createResultSet( "QdocInfo", docinfoParams );
					DataResultSet docinfoDRSet = new DataResultSet();
					docinfoDRSet.copy( docinfoRSet );
					Report.debug( "bezzotechraytheon", docinfoDRSet.isEmpty() ? "Doc info not found" :
							"Found doc info", null );
					if( !docinfoDRSet.isEmpty() ) {
						if( !StringUtils.convertToBool(
								docinfoDRSet.getStringValueByName( "xGlobalWebsiteObject" ), false ) ) {
							docinfoDRSet.setCurrentValue( docinfoDRSet.getFieldInfoIndex( pubStatField ), pubStat );
							docinfoDRSet.setCurrentValue(
									docinfoDRSet.getFieldInfoIndex( "dSecurityGroup" ), calculateSecurityGroup( pubStat ) );
							SSHierarchyServiceHandler ssHandler = new SSHierarchyServiceHandler();
							DataBinder updateBinder = new DataBinder();
							updateBinder.mergeResultSetRowIntoLocalData( docinfoDRSet );
							updateBinder.putLocal( "IdcService", "UPDATE_DOCINFO" );
							Report.debug( "bezzotechraytheon", "Update binder " + updateBinder.toString(), null );
							ssHandler.executeServiceAsSysadmin( updateBinder, ws );
						}
					}
				}
			}
		}
	}

	protected boolean isValidPublicationStatus( String np ) {
		if( np == null ) return false;
		if( np.equals( "" ) ) return false;
		if( np.equals( "inherit" ) ) return false;
		return true;
	}

	protected String calculateSecurityGroup( String np ) {
		String secGrp = SharedObjects.getEnvironmentValue( "HeldSecurityGroup" );
		Report.debug( "bezzotechraytheon", "Test: " + np.equalsIgnoreCase( "release" ), null );
		if( np.equalsIgnoreCase( "release" ) )
			secGrp = SharedObjects.getEnvironmentValue( "PublishedSecurityGroup" );
		Report.trace( "bezzotechraytheon", "Found security group: " + secGrp, null );
		return secGrp;
	}

	protected String calculatePublicationStatus( String np ) {
		String pubStat = "inherit";
		Report.debug( "bezzotechraytheon", "Test: " + np.equalsIgnoreCase( "release" ), null );
		if( np.equalsIgnoreCase( "release" ) ) pubStat = "hold";
		Report.trace( "bezzotechraytheon", "Found publication status: " + pubStat, null );
		return pubStat;
	}

	public String getValidParentPublicationStatus( Workspace ws, String siteID, String nodeID )
			throws DataException, ServiceException {
		Report.trace( "bezzotechraytheon", "Entering getValidParentPublicationStatus", null );
		String nodeProperty = "inherit";
		String parent = null;
		String targetID = nodeID;
		while( nodeProperty.equals( "inherit" ) ) {
			parent = getParent( siteID, targetID, "parent" );
			nodeProperty = getNodeProperty( ws, siteID, parent, "PublicationStatus" );
			targetID = parent;
		}
		return nodeProperty;
	}

	protected String getNodeProperty( Workspace ws, String siteID, String nodeID, String prop )
			throws DataException, ServiceException {
		SSHierarchyServiceHandler ssHandler = new SSHierarchyServiceHandler();
		DataBinder db = new DataBinder();
		db.putLocal( "siteId", siteID );
		db.putLocal( "nodeId", nodeID );
		db.putLocal( "property", prop );
		db.putLocal( "IdcService", "SS_GET_NODE_PROPERTY" );
		ssHandler.executeServiceAsSysadmin( db, ws );
//		String value = ssHandler.getNodeProperty( siteID, nodeID, prop, false )
		String value = db.getLocal( "value" );
		Report.trace( "bezzotechraytheon", "Found node property (" + prop + ") value: " + value, null );
		return value;
	}

	protected String getParent( String siteID, String nodeID, String rel )
			throws DataException, ServiceException {
		SSHierarchyServiceHandler ssHandler = new SSHierarchyServiceHandler();
		String relativeId = ssHandler.getRelativeNodeIdInternal( siteID, nodeID, rel );
		Report.trace( "bezzotechraytheon", "Found relative (" + rel + ") id: " + relativeId, null );
		return relativeId;
	}
}