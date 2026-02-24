<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-nested" prefix="nested" %>

<%
    java.util.List<?> users = (java.util.List<?>) request.getAttribute("users");
    if (users == null) users = new java.util.ArrayList<>();
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户列表 - Struts Zero-Code Demo</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .header {
            margin-bottom: 30px;
            padding-bottom: 20px;
            border-bottom: 2px solid #667eea;
        }
        .header h1 {
            color: #667eea;
            font-size: 2em;
            margin-bottom: 10px;
        }
        .header p {
            color: #666;
            font-size: 1.1em;
        }
        .success-banner {
            background: linear-gradient(135deg, #00c853 0%, #00e676 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 30px;
            box-shadow: 0 4px 15px rgba(0,200,83,0.3);
        }
        .success-banner h2 {
            margin-bottom: 10px;
            font-size: 1.3em;
        }
        .info-box {
            background: #e3f2fd;
            border-left: 4px solid #2196f3;
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        thead {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        th {
            padding: 15px;
            text-align: left;
            font-weight: 600;
            border-bottom: 2px solid #555;
        }
        td {
            padding: 15px;
            border-bottom: 1px solid #eee;
        }
        tbody tr:hover {
            background: #f8f9fa;
            transition: background 0.3s;
        }
        .status {
            display: inline-block;
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 0.9em;
            font-weight: 600;
        }
        .status-active {
            background: #c8e6c9;
            color: #2e7d32;
        }
        .status-inactive {
            background: #ffcdd2;
            color: #c62828;
        }
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #999;
        }
        .empty-state i {
            font-size: 60px;
            margin-bottom: 20px;
            opacity: 0.5;
        }
        .back-btn {
            display: inline-block;
            margin-top: 20px;
            padding: 10px 20px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-weight: bold;
            transition: all 0.3s;
        }
        .back-btn:hover {
            background: #764ba2;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102,126,234,0.4);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>👥 用户列表</h1>
            <p>零代码集成演示 - 数据自动从装饰后的DataSource获取</p>
        </div>

        <div class="success-banner">
            <h2>✅ 零代码集成成功!</h2>
            <p>DataSource 自动解密密码，Action 代码完全无需修改。用户数据成功从 H2 数据库读取。</p>
        </div>

        <div class="info-box">
            <strong>💡 集成验证点：</strong>
            <ul style="margin-left: 20px; margin-top: 5px;">
                <li>DataSource 在 Listener 中创建并装饰</li>
                <li>Action 只需 <code>getServletContext().getAttribute("dataSource")</code></li>
                <li>密码解密由 <code>DataSourceDecorator</code> 自动完成</li>
                <li>无需任何 Action 代码修改</li>
            </ul>
        </div>

        <div class="info-box" style="background: #fff3e0; border-left-color: #ff9800;">
            <strong>🔐 安全特性：</strong>
            <ul style="margin-left: 20px; margin-top: 5px;">
                <li>密码已使用 SM4-GCM 加密存储在 <code>~/.dbpwd/db.pwd</code></li>
                <li>密钥文件位于 <code>~/.dbpwd/sm4.key</code></li>
                <li>数据库连接时自动解密，内存中无明文密码存储</li>
                <li>占位符格式：<code>@struts_demo_zero_code_db@</code></li>
            </ul>
        </div>

        <% if (users.isEmpty()) { %>
            <div class="empty-state">
                <i>📭</i>
                <h3>暂无用户数据</h3>
                <p>初始化时已创建示例数据，请刷新页面查看</p>
            </div>
        <% } else { %>
            <div style="overflow-x: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>用户名</th>
                            <th>邮箱</th>
                            <th>状态</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Object userObj : users) {
                            java.util.Map<?, ?> user = (java.util.Map<?, ?>) userObj;
                        %>
                        <tr>
                            <td><strong><%= user.get("id") %></strong></td>
                            <td><%= user.get("username") %></td>
                            <td><%= user.get("email") %></td>
                            <td>
                                <span class="status <%= "1".equals(String.valueOf(user.get("status"))) ? "status-active" : "status-inactive" %>">
                                    <%= "1".equals(String.valueOf(user.get("status"))) ? "Active" : "Inactive" %>
                                </span>
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
            <p style="margin-top: 15px; color: #666; font-size: 0.9em;">
                共显示 <strong><%= users.size() %></strong> 条用户记录
            </p>
        <% } %>

        <a href="index.do" class="back-btn">← 返回首页</a>
    </div>
</body>
</html>
