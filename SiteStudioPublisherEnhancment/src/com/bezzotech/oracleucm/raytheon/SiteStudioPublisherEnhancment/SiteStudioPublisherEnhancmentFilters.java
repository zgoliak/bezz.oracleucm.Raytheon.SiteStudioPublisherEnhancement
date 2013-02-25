package com.bezzotech.oracleucm.raytheon.SiteStudioPublisherEnhancment;

import intradoc.common.ExecutionContext;
import intradoc.common.ServiceException;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.Workspace;
import intradoc.shared.FilterImplementor;

public class SiteStudioPublisherEnhancmentFilters implements FilterImplementor {
	public int doFilter( Workspace ws, DataBinder db, ExecutionContext ec )
			throws DataException, ServiceException {
		Report.trace( "bezzotechcosign", "Entering SiteStudioPublisherEnhancmentFilters.doFilter", null );
		m_workspace = ws;
		if( ec == null ) {
			System.out.println( "Plugin filter called without a context." );
			return FilterImplementor.CONTINUE;
		}
		Object obj = ec.getCachedObject( "filterParameter" );
		m_shared = SharedObjects.getSharedObjects( ec );
		if( obj == null || !( obj instanceof String ) ) {
			System.out.println( "Plugin filter called without filter parameter." );
			return FilterImplementor.CONTINUE;
		}
		String s = ( String )obj;
		String Type = db.getLocal( "dDocType" );

		if( s.equals( "SSPE_validateStandard" ) ) {
			method2( db );
		} else if( s.equals( "SSPE_SiteStudioAddNode" ) ) {
			method1( db );
		} else
		// SS_SET_NODE_PROPERTY && SS_SWITCH_REGION_ASSOCIATION
		if( s.equals( "SSPE_SiteStudioPreSetNodeProperty" ) ) {
			method3( db, ws, ec );
		}
		return FilterImplementor.CONTINUE;
	}

	protected void method1( DataBinder db ) {
		int i = 0;
		while((s = super.m_binder.getLocal("property" + i)) != null) {
			if( s.equals( "PublicationStatus" ) ) break;
		}
		String siteID = db.getLocal( "siteId" ); 
		String nodeID = db.getLocal( "nodeId" ); 
		String nodeProperty = getValidParentPublicationStatus( db, ec, siteID, nodeID );
		db.putLocal( "property" + i, "PublicationStatus" );
		db.putLocal( "value" + i, nodeProperty );
	}

	protected void method2( DataBinder db, ExecutionContext ec ) {
		String dependsOn = db.getLocal( "xDependsOnWebsiteNode" ); // Depends on Metadata

		// If content is marked as a Global asset set to release and be done
		if( dependsOn.equalsIgnoreCase( "Global" ) ) {
			db.putLocal( "xPublicationStatus", "release" );
			db.putLocal( "dSecurityGroup", "public" );
			return FilterImplementor.CONTINUE;
		}

		String siteID = db.getLocal( "xWebsites" ); // SiteStudio Website
		String nodeProperty = null;
		String targetNode = null;

		// Check the content for metadata dependancies and determine if a valid status is available
		if( dependsOn != null ) {
			nodeProperty = getNodeProperty( db, ec, siteID, dependsOn, "PublicationStatus" );
			if( isValidPublicationStatus( nodeProperty ) ) {
				db.putLocal( "xPublicationStatus", nodeProperty );
				db.putLocal( "dSecurityGroup", calculateSecurityGroup( nodeProperty ) );
				return FilterImplementor.CONTINUE;
			}
			targetNode = dependsOn;
		}

		String nodeID = db.getLocal( "xWebsiteSection" ); // SiteStudio Website Section
		if( targetNode == null && nodeID != null ) {
			db.putLocal( "xDependsOnWebsiteNode", nodeID );
			nodeProperty = getNodeProperty( db, ec, siteID, nodeID, "PublicationStatus" );
			if( isValidPublicationStatus( nodeProperty ) ) {
				db.putLocal( "xPublicationStatus", nodeProperty );
				db.putLocal( "dSecurityGroup", calculateSecurityGroup( nodeProperty ) );
				return FilterImplementor.CONTINUE;
			}
			targetNode = nodeID;
		}
		if( targetNode != null ) {
			nodeProperty = getValidParentPublicationStatus( db, ec, siteID, targetNode );
			db.putLocal( "xPublicationStatus", nodeProperty );
			db.putLocal( "dSecurityGroup", calculateSecurityGroup( nodeProperty ) );
			return FilterImplementor.CONTINUE;
		}
	}

	protected void method3( DataBinder db, Workspace ws, ExecutionContext ec ) {
		if( db.getLocal( "SiteStudio_property" ).equals( "PublicationStatus" ) ) {
			String pubStat = db.getLocal( "SiteStudio_value" );
			String siteID = db.getLocal( "SiteStudio_siteId" );
			String nodeID = db.getLocal( "SiteStudio_nodeId" );
			if( pubStat.equals( "inherit" ) ) {
				pubStat = getValidParentPublicationStatus( db, ec, siteID, nodeID );
			}
			DataBinder params = new DataBinder();
			params.putLocal( siteID );
			params.putLocal( nodeID );
			ResultSet rset = ws.createResultSet( "QdependsOnWebsiteNode", params );
			if( !rset.isEmpty() ) {
				do{
					DataBinder docinfoParams = new DataBinder();
					docinfoParams.putLocal( rset.getStringValueByName( "dID" ) );
					ResultSet docinfoRSet = ws.createResultSet( "QdocInfo", docinfoParams )
					DataResultSet drset = new DataResultSet();
					drset.copy( rset );
					if( !drset.isEmpty() ) {
						do{
							if( !drset.getStringValueByName( "xDependsOnWebsiteNode" ).equals( "Global" ) ) {
								drset.setCurrentValue( drset.getFieldInfoIndex( "xPublicationStatus" ), pubStat );
								drset.setCurrentValue(
										drset.getFieldInfoIndex( "dSecurityGroup" ), calculateSecurityGroup( pubStat ) );
								SSHierarchyServiceHandler ssHandler = new SSHierarchyServiceHandler();
								DataBinder updateBinder = new DataBinder();
								updateBinder.mergeResultSetRowIntoLocalData( drset );
								updateBinder.putLocal( "IdcService", "UPDATE_DOCINFO" );
								ssHandler.executeServiceAsSysadmin( updateBinder, ws );
							}
						} while( drset.next() );
					}
				} while( rset.next() );
			}
		}
	}

	protected boolean isValidPublicationStatus( String np ) {
		if( np == null ) return false;
		if( np.equals( "" ) return false;
		if( np.equals( "inherit" ) return false;
		return true;
	}

	protected string calculateSecurityGroup( String np ) {
		String secGrp = SharedObjects.get( "HeldSecurityGroup" );
		if( np.equalsIgnoreCase( "release" ) )
			secGrp = SharedObjects.get( "PublishedSecurityGroup" );
		return secGrp;
	}

//	public String getNodeProperty( String SiteID, String SectionID, String property )
//			throws DataException, ServiceException {
	public void getNodeProperty( DataBinder db, ExecutionContext ec, String siteID, String nodeID,
			String prop ) throws DataException, ServiceException {
		Service s = ( Service )ec;
//		DataBinder db = new DataBinder();
//		db.setEnvironment( SharedObjects.getEnvironment() );
//		db.putLocal( "IdcService", "SS_GET_NODE_PROPERTY" );
		db.putLocal( "siteId", siteID );
		db.putLocal( "nodeId", nodeID );
		db.putLocal( "property", prop );
		s.executeService( "SS_GET_NODE_PROPERTY" );
//		DataBinder B = executeService( db, "weblogic", false );
		//get the service execution result and return it as a string value;
//		String result = B.getLocalData().toString();
//		String value = result.substring( result.indexOf( "value" ), result.indexOf( ",", result.indexOf( "value" ) ) );
		String value = db.getLocal( "value" );
		Report.trace( value );
		return value;
	}

	public static String getParent( DataBinder db, ExecutionContext ec, String siteID, String nodeID,
			String rel ) throws DataException, ServiceException {
		Service s = ( Service )ec;
//		db.putLocal("IdcService", "SS_GET_RELATIVE_NODE_ID");
		db.putLocal( "siteId", siteID );
		db.putLocal( "nodeId", nodeID );
		db.putLocal( "relative", rel );
		s.executeService( "SS_GET_RELATIVE_NODE_ID" );
		// execute the request
//		ServiceResponse response = idcClient.sendRequest(userContext, binder);
//		String result = response.getResponseAsBinder().getLocal("relativeId");
		String result = db.getLocal( "relativeId" );
		Report.trace( result );
		return result;
	}

	public static String getValidParentPublicationStatus( DataBinder db, ExecutionContext ec,
			String siteID, String nodeID ) throws DataException, ServiceException {
		String nodeProperty = "inherit";
		String parent = null;
		while( !nodeProperty.equals( "inherit" ) ) {
			parent = getParent( db, ec, siteID, nodeID, "parent" );
			nodeProperty = getNodeProperty( db, ec, siteID, parent, "PublicationStatus" );
		}
		return nodeProperty;
	}
}