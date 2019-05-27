package org.apache.catalina.valves;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.StringManager;


/**
 * <p>Valve实现类输出HTML错误页面.</p>
 *
 * <p>这个Valve 应该附加在Host等级, 如果附加到Context等级，它也可以工作.</p>
 */
public class ErrorReportValve extends ValveBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 调试等级
     */
    private int debug = 0;


    /**
     * 实现类描述信息
     */
    private static final String info =
        "org.apache.catalina.valves.ErrorReportValve/1.0";


    /**
     * The StringManager for this package.
     */
    protected static StringManager sm = StringManager.getManager(Constants.Package);


    // ------------------------------------------------------------- Properties


    /**
     * 实现类描述信息
     */
    public String getInfo() {
        return (info);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * 执行序列中的下一个Valve. 当执行返回时, 检查响应状态, 输出一个错误报告是必要的.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void invoke(Request request, Response response,
                       ValveContext context)
        throws IOException, ServletException {

        // Perform the request
        context.invokeNext(request, response);

        ServletRequest sreq = (ServletRequest) request;
        Throwable throwable = 
            (Throwable) sreq.getAttribute(Globals.EXCEPTION_ATTR);

        ServletResponse sresp = (ServletResponse) response;
        if (sresp.isCommitted()) {
            return;
        }

        if (throwable != null) {

            // The response is an error
            response.setError();

            // Reset the response (if possible)
            try {
                sresp.reset();
            } catch (IllegalStateException e) {
            }

            ServletResponse sresponse = (ServletResponse) response;
            if (sresponse instanceof HttpServletResponse)
                ((HttpServletResponse) sresponse).sendError
                    (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.setSuspended(false);

        try {
            report(request, response, throwable);
        } catch (Throwable tt) {
            tt.printStackTrace();
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ErrorReportValve[");
        sb.append(container.getName());
        sb.append("]");
        return (sb.toString());
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 打印出错误报告
     * 
     * @param request The request being processed
     * @param response The response being generated
     * @param exception The exception that occurred (which possibly wraps
     *  a root cause exception
     */
    protected void report(Request request, Response response, Throwable throwable)
        throws IOException {

        // Do nothing on non-HTTP responses
        if (!(response instanceof HttpResponse))
            return;
        HttpResponse hresponse = (HttpResponse) response;
        if (!(response instanceof HttpServletResponse))
            return;
        HttpServletResponse hres = (HttpServletResponse) response;
        int statusCode = hresponse.getStatus();
        String message = RequestUtil.filter(hresponse.getMessage());
        if (message == null)
            message = "";

        // Do nothing on a 1xx, 2xx and 3xx status
        if (statusCode < 400)
            return;

        // FIXME: Reset part of the request
/*
        try {
            if (hresponse.isError())
                hresponse.reset(statusCode, message);
        } catch (IllegalStateException e) {
            ;
        }
*/

        Throwable rootCause = null;

        if (throwable != null) {
            if (throwable instanceof ServletException)
                rootCause = ((ServletException) throwable).getRootCause();
        }

        // 如果没有指定状态码的报告，请不要做任何事
        String report = null;
        try {
            report = sm.getString("http." + statusCode, message);
        } catch (Throwable t) {
        }
        if (report == null)
            return;

        StringBuffer sb = new StringBuffer();

        sb.append("<html><head><title>");
        sb.append(ServerInfo.getServerInfo()).append(" - ");
        sb.append(sm.getString("errorReportValve.errorReport"));
        sb.append("</title>");
        sb.append("<STYLE><!--");
        sb.append("H1{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append("H3{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append("BODY{font-family : sans-serif,Arial,Tahoma;color : black;background-color : white;} ");
        sb.append("B{color : white;background-color : #0086b2;} ");
        sb.append("HR{color : #0086b2;} ");
        sb.append("--></STYLE> ");
        sb.append("</head><body>");
        sb.append("<h1>");
        sb.append(sm.getString("errorReportValve.statusHeader",
                               "" + statusCode, message)).append("</h1>");
        sb.append("<HR size=\"1\" noshade>");
        sb.append("<p><b>type</b> ");
        if (throwable != null) {
            sb.append(sm.getString("errorReportValve.exceptionReport"));
        } else {
            sb.append(sm.getString("errorReportValve.statusReport"));
        }
        sb.append("</p>");
        sb.append("<p><b>");
        sb.append(sm.getString("errorReportValve.message"));
        sb.append("</b> <u>");
        sb.append(message).append("</u></p>");
        sb.append("<p><b>");
        sb.append(sm.getString("errorReportValve.description"));
        sb.append("</b> <u>");
        sb.append(report);
        sb.append("</u></p>");

        if (throwable != null) {
            StringWriter stackTrace = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stackTrace));
            sb.append("<p><b>");
            sb.append(sm.getString("errorReportValve.exception"));
            sb.append("</b> <pre>");
            sb.append(stackTrace.toString());
            sb.append("</pre></p>");
            if (rootCause != null) {
                stackTrace = new StringWriter();
                rootCause.printStackTrace(new PrintWriter(stackTrace));
                sb.append("<p><b>");
                sb.append(sm.getString("errorReportValve.rootCause"));
                sb.append("</b> <pre>");
                sb.append(stackTrace.toString());
                sb.append("</pre></p>");
            }
        }

        sb.append("<HR size=\"1\" noshade>");
        sb.append("<h3>").append(ServerInfo.getServerInfo()).append("</h3>");
        sb.append("</body></html>");

        try {

            Writer writer = response.getReporter();

            if (writer != null) {

                Locale locale = Locale.getDefault();

                try {
                    hres.setContentType("text/html");
                    hres.setLocale(locale);
                } catch (Throwable t) {
                    if (debug >= 1)
                        log("status.setContentType", t);
                }

                // If writer is null, 这表明响应已经提交, 不该发生
                writer.write(sb.toString());
                writer.flush();
            }

        } catch (IOException e) {
        } catch (IllegalStateException e) {
        }
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message);
        else
            System.out.println(this.toString() + ": " + message);
    }


    /**
     * 记录日志
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message, throwable);
        else {
            System.out.println(this.toString() + ": " + message);
            throwable.printStackTrace(System.out);
        }
    }
}
