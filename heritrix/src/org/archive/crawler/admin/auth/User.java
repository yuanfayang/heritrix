package org.archive.crawler.admin.auth;

/**
 * @author Kristinn Sigurðsson
 *
 * A simple class for user authentication.  The class also defines the availible user roles.
 */
public class User
{
	// Defined roles
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
	 * @return The int value of the user's role (ADMINISTRATOR, USER).  
	 * A return of INVALID_USERS equals a failed authentication. 
	 */
	public int authenticate()
	{
		if(sUsername!=null && sPassword != null)
		{
			if (sUsername.equalsIgnoreCase("admin") && sPassword.equalsIgnoreCase("letmein"))
			{
				return ADMINISTRATOR;
			}	
			else if(sUsername.equalsIgnoreCase("user") && sPassword.equalsIgnoreCase("archive"))
			{
				return USER;
			}
		}
		return INVALID_USER;
	}
}