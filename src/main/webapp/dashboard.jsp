<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="ds.web.AccessLogRepository" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Job Search Service — Dashboard</title>
    <style>
        body { font-family: system-ui, sans-serif; margin: 1.5rem; }
        h1 { font-size: 1.25rem; }
        table { border-collapse: collapse; width: 100%; margin-bottom: 2rem; }
        th, td { border: 1px solid #ccc; padding: 0.4rem 0.5rem; text-align: left; }
        th { background: #f4f4f4; }
        .metrics td { font-weight: 600; }
    </style>
</head>
<body>
<h1>Operations analytics</h1>
<table class="metrics">
    <tr><th>Metric</th><th>Value</th></tr>
    <tr><td>Total mobile API requests logged</td><td>${totalRequests}</td></tr>
    <tr><td>Average Arbeitnow API latency (ms)</td><td><%
        Object avg = request.getAttribute("avgThirdPartyLatencyMs");
        double avgMs = avg instanceof Number ? ((Number) avg).doubleValue() : 0;
    %><%= String.format("%.1f", avgMs) %></td></tr>
    <tr><td>Distinct device model values seen</td><td>${distinctDeviceModels}</td></tr>
</table>

<h2>Top search keywords</h2>
<table>
    <tr><th>Keyword</th><th>Count</th></tr>
    <%
        @SuppressWarnings("unchecked")
        List<AccessLogRepository.KeywordCount> top =
                (List<AccessLogRepository.KeywordCount>) request.getAttribute("topKeywords");
        if (top != null) {
            for (AccessLogRepository.KeywordCount row : top) {
    %>
    <tr><td><%= row.getKeyword() %></td><td><%= row.getCount() %></td></tr>
    <%
            }
        }
    %>
</table>

<h2>Recent access logs (mobile only)</h2>
<table>
    <tr>
        <th>Time</th><th>Client IP</th><th>Device</th><th>Keyword</th>
        <th>3rd-party HTTP</th><th>3rd-party ms</th><th>Jobs returned</th><th>Reply HTTP</th>
    </tr>
    <%
        @SuppressWarnings("unchecked")
        List<AccessLogRepository.AccessLogRow> logs =
                (List<AccessLogRepository.AccessLogRow>) request.getAttribute("recentLogs");
        if (logs != null) {
            for (AccessLogRepository.AccessLogRow r : logs) {
    %>
    <tr>
        <td><%= r.getReceivedAt() %></td>
        <td><%= r.getClientIp() %></td>
        <td><%= r.getDeviceModel() %></td>
        <td><%= r.getKeyword() %></td>
        <td><%= r.getThirdPartyStatus() %></td>
        <td><%= r.getThirdPartyLatencyMs() %></td>
        <td><%= r.getJobsReturned() %></td>
        <td><%= r.getReplyHttpStatus() %></td>
    </tr>
    <%
            }
        }
    %>
</table>
</body>
</html>
