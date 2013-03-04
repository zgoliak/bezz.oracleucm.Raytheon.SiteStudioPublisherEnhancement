/*
This file can be used to write custom install/uninstall steps.
*/

package com.bezzotech.oracleucm.raytheon.SiteStudioPublisherEnhancment;

import intradoc.common.*;
import intradoc.data.*;
import intradoc.shared.*;
import intradoc.server.*;

import java.io.*;

public class SSPE_InstallFilter implements FilterImplementor {
	public int doFilter( Workspace ws, DataBinder binder, ExecutionContext cxt )
			throws DataException, ServiceException {
		// CS version must be greater than 7.0 to run this install filter.
		if( getSignificantVersion() <= 6 ) {
			return CONTINUE;
		}
		String param = null;
		Object paramObj = cxt.getCachedObject( "filterParameter" );
		if( paramObj == null || ( paramObj instanceof String ) == false ) {
			return CONTINUE;
		}
		param = ( String )paramObj;

		Service s = null;
		IdcExtendedLoader loader = null;

		if( cxt instanceof IdcExtendedLoader ) {
			loader = ( IdcExtendedLoader ) cxt;
			if( ws == null ) {
				ws = loader.getLoaderWorkspace();
			}
		}
		else if( cxt instanceof Service ) {
			s = ( Service )cxt;
			loader = ( IdcExtendedLoader )ComponentClassFactory.createClassInstance( "IdcExtendedLoader",
								"intradoc.server.IdcExtendedLoader", "!csCustomInitializerConstructionError" );
		}

		// Called after environment data has been loaded and directory locations
		// have been determined but before database has been accessed.
		if( param.equals( "extraAfterConfigInit" ) ) {
		}
		// Called after initial connection to database and queries have been
		// loaded but before database is used to load data into application.
		// This is a good service for performing database table manipulation.
		else if( param.equals( "extraBeforeCacheLoadInit" ) ) {
		}
		// Called after the last standard activity of a
		// server side application initialization.  This is a good place
		// to manipulate cached data or override standard configuration
		// data.
		else if( param.equals( "extraAfterServicesLoadInit" ) ) {
		}
		// Called after loading cached tables.
		else if( param.equals( "initSubjects" ) ) {
		}
		// Called for custom uninstallation steps.
		// NOTE: Change the uninstall filter name to have your component name prefix.
		// For example:  MyTestCompnentUninstallFilter
		else if( param.equals( "<MyComponentName>ComponentUninstallFilter" ) ) {
		}
		return CONTINUE;
	}

	protected int getSignificantVersion() {
		String strVersion = SystemUtils.getProductVersionInfo();
		int nIndex = strVersion.indexOf( "." );
		if( nIndex != -1 ) {
			strVersion = strVersion.substring( 0, nIndex );
		}
		return( Integer.valueOf( strVersion ).intValue() );
	}
}