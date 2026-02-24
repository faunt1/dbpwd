<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Struts Zero-Code Integration Demo</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
        }
        .container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }
        h1 {
            color: #667eea;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }
        .feature {
            background: #f0f4ff;
            padding: 20px;
            margin: 15px 0;
            border-left: 4px solid #667eea;
            border-radius: 5px;
        }
        .feature h3 {
            color: #764ba2;
            margin-top: 0;
        }
        .code {
            background: #2d2d2d;
            color: #f8f8f2;
            padding: 15px;
            border-radius: 5px;
            font-family: 'Courier New', monospace;
            white-space: pre-wrap;
            font-size: 14px;
        }
        .btn {
            display: inline-block;
            padding: 12px 24px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            font-weight: bold;
            margin-top: 20px;
            transition: all 0.3s;
        }
        .btn:hover {
            background: #764ba2;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }
        .highlight {
            background: #fffacd;
            padding: 2px 4px;
            border-radius: 3px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Struts Zero-Code Integration Demo</h1>
        <p>
            演示如何使用 <span class="highlight">db-pwd</span> 实现零代码数据库密码加密集成
        </p>

        <div class="feature">
            <h3>✨ 零代码集成的关键特性</h3>
            <ul>
                <li><strong>无需修改 Action 代码</strong> - 只需从 ServletContext 获取 DataSource</li>
                <li><strong>自动密码解密</strong> - DataSourceDecorator 在 getConnection() 时自动解密</li>
                <li><strong>最小配置改动</strong> - 只需添加一个 Listener 到 web.xml</li>
                <li><strong>典型 Struts 模式</strong> - 符合 Struts 最佳实践</li>
            </ul>
        </div>

        <div class="feature">
            <h3>📝 与当前实现对比</h3>

            <h4>当前方式（需要代码修改）</h4>
            <div class="code">// UserListAction.execute()
DruidDataSource dataSource = new DruidDataSource();
dataSource.setPassword(plainPassword);  // ← 需要手动解密
conn = dataSource.getConnection();</div>

            <h4>零代码方式（无需修改）</h4>
            <div class="code">// UserListAction.execute()
DataSource ds = (DataSource) getServlet()
    .getServletContext().getAttribute("dataSource");
conn = ds.getConnection();  // ← 自动解密，零代码！</div>
        </div>

        <div class="feature">
            <h3>🔧 集成步骤</h3>
            <ol>
                <li>在 web.xml 中添加 <code>DbPwdStrutsInitializer</code> Listener（仅需1行配置）</li>
                <li>在 pom.xml 中添加 db-pwd 依赖</li>
                <li>Action 保持原有逻辑，无需任何修改</li>
                <li>完成！密码自动加密/解密</li>
            </ol>
        </div>

        <a href="users.do" class="btn">查看用户列表 &rarr;</a>
    </div>
</body>
</html>
