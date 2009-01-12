<%@ page pageEncoding="UTF-8" %> 
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
        <title>Heritrix Admin Login</title>
    </head>

    <body onLoad="document.passwordForm.enteredPassword.focus()">
        <h3> Heritrix Admin Login </h3>

        <form method="post" name="passwordForm" accept-charset='UTF-8'>
            Admin password: <input type="password" name="enteredPassword"/>
            <input type="submit" value="go"/>
        </form>
    </body>
</html> 

