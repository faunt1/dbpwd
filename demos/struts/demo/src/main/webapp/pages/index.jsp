<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<!DOCTYPE html>
<html>
<head>
    <title>Database Password Manager - Struts Demo</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #333; }
        .info { background: #f0f0f0; padding: 15px; border-radius: 5px; }
        .btn { display: inline-block; padding: 10px 20px; background: #007bff; color: white; 
               text-decoration: none; border-radius: 5px; margin: 5px; }
        .btn:hover { background: #0056b3; }
    </style>
</head>
<body>
    <h1>Database Password Manager - Struts Demo</h1>
    
    <div class="info">
        <p><strong>Integration Mode:</strong> Struts 1.x + db-pwd-web</p>
        <p><strong>Encryption:</strong> SM4-GCM</p>
        <p><strong>Database:</strong> H2 File Mode</p>
    </div>
    
    <h2>Available Actions:</h2>
    <ul>
        <li><a href="user/list.do">List Users</a></li>
        <li><a href="users.do">User Management</a></li>
    </ul>
    
    <h2>Test API:</h2>
    <p><a class="btn" href="user/list.do">View Users</a></p>
</body>
</html>
