/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.crawler.admin.auth;

/**
 * @author Kristinn Sigurdsson
 *
 * A simple class for user authentication.  The class also defines the availible user roles.
 */
public class User
{
	// Defined roles
	private static final String USER_PASSWORD = "archive";
  private static final String USER_USERNAME = "user";
  private static final String OPERATOR_PASSWORD = "letmein";
  private static final String OPERATOR_USERNAME = "admin";
  public static final int INVALID_USER = -1; 	//Failed login
	public static final int ADMINISTRATOR = 0;	//Super user, full permissions
	public static final int USER = 1;			//General user, minimum permissions
	
	String sUsername;
	String sPassword;
	
	public User(String name, String password)
	{
		sUsername = name;
		sPassword = password;
	}

	/**
	 * @return The int value of the user's role (@link{User#ADMINISTRATOR}, @link{User#USER}).  
	 * A return of @link{User#INVALID_USER} equals a failed authentication. 
	 */
	public int authenticate()
	{
		if(sUsername!=null && sPassword != null)
		{
			if (sUsername.equalsIgnoreCase(getOperatorUsername()) && sPassword.equalsIgnoreCase(getOperatorPassword()))
			{
				return ADMINISTRATOR;
			}	
			else if(sUsername.equalsIgnoreCase(USER_USERNAME) && sPassword.equalsIgnoreCase(USER_PASSWORD))
			{
				return USER;
			}
		}
		return INVALID_USER;
	}

  public static String getOperatorPassword() {
    return OPERATOR_PASSWORD;
  }
 
  public static String getOperatorUsername() {
    return OPERATOR_USERNAME;
  }
}
