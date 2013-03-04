package com.bezzotech.oracleucm.raytheon.SiteStudioPublisherEnhancment;

import intradoc.common.ServiceException;
import intradoc.common.SystemUtils;
import intradoc.data.DataBinder;
import intradoc.data.Workspace;
import intradoc.server.Service;
import intradoc.server.ServiceData;
import intradoc.server.ServiceManager;
import intradoc.shared.SharedObjects;

import java.util.Properties;


public class SSPEUtils {
	public static void executeServiceAsSysadmin( DataBinder db, Workspace ws )
			throws ServiceException {
		if( SharedObjects.getTable( "Users" ) == null ) {
			ServiceException serviceexception = new ServiceException( "Users table is not available.  " +
					"Cannot execute an authenticated service." );
			SystemUtils.dumpException( null, serviceexception );
		}
		String s = db.getLocal( "IdcService" );
		Properties properties = db.getEnvironment();
		Properties properties1 = ( Properties )SharedObjects.getEnvironment().clone();
		db.setEnvironment( properties1 );
		try {
			db.setEnvironmentValue( "HTTP_INTERNETUSER", "sysadmin" );
			ServiceData sd = ServiceManager.getFullService( s );
			Service service = ServiceManager.createService( sd.m_classID, ws, null, db, sd );
			service.setSendFlags( true, true );
			service.initDelegatedObjects();
			service.globalSecurityCheck();
			service.preActions();
			service.doActions();
			service.postActions();
		} catch( Exception exception ) {
			exception.printStackTrace();
			throw new ServiceException( exception.getMessage() );
		} finally {
			db.setEnvironment( properties );
		}
		db.setEnvironment( properties );
	}
}