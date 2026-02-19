<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<!DOCTYPE html>
<html>
<head>
    <title>User List - Struts Demo</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #007bff; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .back { display: inline-block; padding: 10px 20px; background: #6c757d; color: white; 
                text-decoration: none; border-radius: 5px; margin-top: 20px; }
    </style>
</head>
<body>
    <h1>User List</h1>
    
    <table>
        <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
        </tr>
        <logic:iterate name="users" id="user">
        <tr>
            <td><bean:write name="user" property="id"/></td>
            <td><bean:write name="user" property="username"/></td>
            <td><bean:write name="user" property="email"/></td>
        </tr>
        </logic:iterate>
    </table>
    
    <a href="index.do" class="back">Back to Home</a>
</body>
</html>
