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

import java.util.Vector;

/**
 * A simple class for user authentication.  The class also defines the availible user roles.
 * On startup, all valid logins should be added using the {@link #addLogin(String, String, int) addLogin()}
 * method. Logins can be added and removed at any time.
 * <p>
 * The login add/remove methods are static. To handle user authentication, create a new instance of the class
 * giving the login info (username/password) to the constructor. The method {@link #authenticate()} will then
 * return the user role or if login fails, the INVALID_USER.
 * <p>
 * 
 * @see #INVALID_USER
 * @see #ADMINISTRATOR
 * @see #USER
 * 
 * @author Kristinn Sigurdsson
 */
public class User
{
	// Defined roles
  	/** A failed login */
  	public static final int INVALID_USER = -1;
	/** Super user, full permissions */
	public static final int ADMINISTRATOR = 0;
	/** General user, minimum permissions */
	public static final int USER = 1;

	//Known valid logins
	private static Vector logins = new Vector();
	
	String sUsername;
	String sPassword;
	int iRole = INVALID_USER;
	
	/**
	 * Constructor to authenticate a new login. Username and password are <b>not</b> case sensitive.
	 * 
	 * @param name Username
	 * @param password Password
	 */
	public User(String name, String password)
	{
		sUsername = name;
		sPassword = password;
		
		if(sUsername!=null && sPassword != null)
		{
			for(int i=0; i<logins.size(); i++)
			{
				Login login = (Login)logins.get(i);
				if(login.username.equalsIgnoreCase(sUsername))
				{
					if(login.password.equalsIgnoreCase(sPassword))
					{
						iRole = login.role;
					}
					return;
				}
			}
		}
	}

	/**
	 * @return The int value of the user's role (@link{User#ADMINISTRATOR}, @link{User#USER}).  
	 * A return of @link{User#INVALID_USER} equals a failed authentication. 
	 */
	public int authenticate()
	{
		return iRole;
	}

	/**
	 * Add a new user to list of allowed logins.
     *
	 * If given username already exist this method will overwrite it.
	 * 
	 * @param username Username
	 * @param password Password
	 * @param role Role
	 * 
	 * @see #ADMINISTRATOR
	 * @see #USER
	 */
	public static void addLogin(String username, String password, int role)
	{	
		removeLogin(username); // In case it already exist.
		logins.add(new Login(username, password, role));
	}
	
	/**
	 * Remove a login by it's username.
	 * 
	 * @param username Username
	 */
	public static void removeLogin(String username)
	{
		for(int i=0; i<logins.size(); i++)
		{
			Login login = (Login)logins.get(i);
			if(login.username.equalsIgnoreCase(username))
			{
				logins.remove(i);
				return;
			}
		}
	}
}

/**
 * Internal class for the User class. This class is essentially a struct to contain one valid login.
 * Username + password + role.
 * 
 * @see org.archive.crawler.admin.auth.User
 * 
 * @author Kristinn Sigurdsson
 */
class Login
{
	protected String username;
	protected String password;
	protected int role; 

	/**
	 * Constructor.  A quick way of setting all the attributes
	 * 
	 * @param username Username
	 * @param password Password
	 * @param role Role
	 */
	public Login(String username, String password, int role)
	{
		this.username =username;
		this.password = password;
		this.role = role;
	}
}
