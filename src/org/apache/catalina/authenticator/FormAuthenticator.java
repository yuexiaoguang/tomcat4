package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.deploy.LoginConfig;

/**
 * <b>Authenticator</b>和<b>Valve</b>的FORM BASED验证实现类, 正如servlet API规范中描述的, Version 2.2.
 */
public class FormAuthenticator extends AuthenticatorBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * 实现类的描述信息
     */
    protected static final String info =
        "org.apache.catalina.authenticator.FormAuthenticator/1.0";

    // ------------------------------------------------------------- Properties

    /**
     * 返回Valve实现类的描述信息
     */
    public String getInfo() {
        return (this.info);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * 根据指定的登录配置，对作出此请求的用户进行身份验证. 
     * 如果满足指定的约束，返回<code>true</code>;
     * 如果已经创建了一个响应，返回 or <code>false</code>
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param login 描述如何进行身份验证的登录配置
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config)
        throws IOException {

        // 稍后需要的对象的引用
        HttpServletRequest hreq =
          (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
          (HttpServletResponse) response.getResponse();
        Session session = null;

        // 是否转备好验证其中一个?
        Principal principal = hreq.getUserPrincipal();
        if (principal != null) {
            if (debug >= 1)
                log("Already authenticated '" +
                    principal.getName() + "'");
            String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
            if (ssoId != null)
                associate(ssoId, getSession(request, true));
            return (true);
        }

        // 是否已经验证过这个用户，但是已经禁用了缓存？
        if (!cache) {
            session = getSession(request, true);
            if (debug >= 1)
                log("Checking for reauthenticate in session " + session);
            String username =
                (String) session.getNote(Constants.SESS_USERNAME_NOTE);
            String password =
                (String) session.getNote(Constants.SESS_PASSWORD_NOTE);
            if ((username != null) && (password != null)) {
                if (debug >= 1)
                    log("Reauthenticating username '" + username + "'");
                principal =
                    context.getRealm().authenticate(username, password);
                if (principal != null) {
                    session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);
                    register(request, response, principal,
                             Constants.FORM_METHOD,
                             username, password);
                    return (true);
                }
                if (debug >= 1)
                    log("Reauthentication failed, proceed normally");
            }
        }

        // 这是在成功身份验证之后的原始请求URI的重新提交吗？如果是，则转发原始请求
        if (matchRequest(request)) {
            session = getSession(request, true);
            if (debug >= 1)
                log("Restore request from session '" + session.getId() + "'");
            principal = (Principal)
                session.getNote(Constants.FORM_PRINCIPAL_NOTE);
            register(request, response, principal, Constants.FORM_METHOD,
                     (String) session.getNote(Constants.SESS_USERNAME_NOTE),
                     (String) session.getNote(Constants.SESS_PASSWORD_NOTE));
            String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
            if (ssoId != null)
                associate(ssoId, session);
            if (restoreRequest(request, session)) {
                if (debug >= 1)
                    log("Proceed to restored request");
                return (true);
            } else {
                if (debug >= 1)
                    log("Restore of original request failed");
                hres.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return (false);
            }
        }

        // 获取需要评估的对象的引用
        String contextPath = hreq.getContextPath();
        String requestURI = request.getDecodedRequestURI();
        response.setContext(request.getContext());

        // 这是登录页面本身的请求吗?  测试此处以避免显示两次（从用户的角度来看） 
        //-- 一次是因为“保存和重定向”，一次是因为“还原和重定向”在下面执行的
        String loginURI = contextPath + config.getLoginPage();
        if (requestURI.equals(loginURI)) {
            if (debug >= 1)
                log("Requesting login page normally");
            return (true);      // 以通常的方式显示登录页面
        }

        // 这是错误页面本身的请求吗？  
//        如果错误页面位于安全约束的保护区域内，请在此测试以避免循环（返回登录页）
        String errorURI = contextPath + config.getErrorPage();
        if (requestURI.equals(errorURI)) {
            if (debug >= 1)
                log("Requesting error page normally");
            return (true);      //以通常的方式显示错误页面
        }

        //这是登录页面的操作请求吗?
        boolean loginAction =
            requestURI.startsWith(contextPath) &&
            requestURI.endsWith(Constants.FORM_ACTION);

        // No -- 保存此请求并重定向到表单登录页面
        if (!loginAction) {
            session = getSession(request, true);
            if (debug >= 1)
                log("Save request in session '" + session.getId() + "'");
            saveRequest(request, session);
            if (debug >= 1)
                log("Redirect to login page '" + loginURI + "'");
            hres.sendRedirect(hres.encodeRedirectURL(loginURI));
            return (false);
        }

        // Yes -- 验证指定的凭据并重定向到错误页，如果它们不正确
        Realm realm = context.getRealm();
        String username = hreq.getParameter(Constants.FORM_USERNAME);
        String password = hreq.getParameter(Constants.FORM_PASSWORD);
        if (debug >= 1)
            log("Authenticating username '" + username + "'");
        principal = realm.authenticate(username, password);
        if (principal == null) {
            if (debug >= 1)
                log("Redirect to error page '" + errorURI + "'");
            hres.sendRedirect(hres.encodeRedirectURL(errorURI));
            return (false);
        }

        // 保存认证的Principal到session
        if (debug >= 1)
            log("Authentication of '" + username + "' was successful");
        if (session == null)
            session = getSession(request, true);
        session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);

        //如果我们不缓存，也要保存用户名和密码
        if (!cache) {
            session.setNote(Constants.SESS_USERNAME_NOTE, username);
            session.setNote(Constants.SESS_PASSWORD_NOTE, password);
        }

        // 将用户重定向到原始请求URI（这将导致原始请求恢复）
        requestURI = savedRequestURL(session);
        if (debug >= 1)
            log("Redirecting to original '" + requestURI + "'");
        if (requestURI == null)
            hres.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           sm.getString("authenticator.formlogin"));
        else
            hres.sendRedirect(hres.encodeRedirectURL(requestURI));
        return (false);

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * 这个请求是否与保存的请求相匹配?(因此它必须是在成功身份验证之后发出的重定向)
     *
     * @param request The request to be verified
     */
    protected boolean matchRequest(HttpRequest request) {

      // Has a session been created?
      Session session = getSession(request, false);
      if (session == null)
          return (false);

      // Is there a saved request?
      SavedRequest sreq = (SavedRequest)
          session.getNote(Constants.FORM_REQUEST_NOTE);
      if (sreq == null)
          return (false);

      // Is there a saved principal?
      if (session.getNote(Constants.FORM_PRINCIPAL_NOTE) == null)
          return (false);

      // Does the request URI match?
      HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
      String requestURI = hreq.getRequestURI();
      if (requestURI == null)
          return (false);
      return (requestURI.equals(sreq.getRequestURI()));

    }


    /**
     * 从会话中存储的信息恢复原始请求.
     * 如果原始请求不再存在 (由于会话超时), 返回<code>false</code>; 否则返回<code>true</code>.
     *
     * @param request 要恢复的请求
     * @param session 包含保存的信息的会话
     */
    protected boolean restoreRequest(HttpRequest request, Session session) {

        //从会话中检索和删除 SavedRequest对象
        SavedRequest saved = (SavedRequest)
            session.getNote(Constants.FORM_REQUEST_NOTE);
        session.removeNote(Constants.FORM_REQUEST_NOTE);
        session.removeNote(Constants.FORM_PRINCIPAL_NOTE);
        if (saved == null)
            return (false);

        // 修改当前请求以反映原始请求
        request.clearCookies();
        Iterator cookies = saved.getCookies();
        while (cookies.hasNext()) {
            request.addCookie((Cookie) cookies.next());
        }
        request.clearHeaders();
        Iterator names = saved.getHeaderNames();
        while (names.hasNext()) {
            String name = (String) names.next();
            Iterator values = saved.getHeaderValues(name);
            while (values.hasNext()) {
                request.addHeader(name, (String) values.next());
            }
        }
        request.clearLocales();
        Iterator locales = saved.getLocales();
        while (locales.hasNext()) {
            request.addLocale((Locale) locales.next());
        }
        request.clearParameters();
        if ("POST".equalsIgnoreCase(saved.getMethod())) {
            Iterator paramNames = saved.getParameterNames();
            while (paramNames.hasNext()) {
                String paramName = (String) paramNames.next();
                String paramValues[] =
                    (String[]) saved.getParameterValues(paramName);
                request.addParameter(paramName, paramValues);
            }
        }
        request.setMethod(saved.getMethod());
        request.setQueryString(saved.getQueryString());
        request.setRequestURI(saved.getRequestURI());
        return (true);

    }


    /**
     * 将原始请求信息保存到会话中
     *
     * @param request The request to be saved
     * @param session The session to contain the saved information
     */
    private void saveRequest(HttpRequest request, Session session) {

        // Create and populate a SavedRequest object for this request
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        SavedRequest saved = new SavedRequest();
        Cookie cookies[] = hreq.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++)
                saved.addCookie(cookies[i]);
        }
        Enumeration names = hreq.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Enumeration values = hreq.getHeaders(name);
            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                saved.addHeader(name, value);
            }
        }
        Enumeration locales = hreq.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = (Locale) locales.nextElement();
            saved.addLocale(locale);
        }
        Map parameters = hreq.getParameterMap();
        Iterator paramNames = parameters.keySet().iterator();
        while (paramNames.hasNext()) {
            String paramName = (String) paramNames.next();
            String paramValues[] = (String[]) parameters.get(paramName);
            saved.addParameter(paramName, paramValues);
        }
        saved.setMethod(hreq.getMethod());
        saved.setQueryString(hreq.getQueryString());
        saved.setRequestURI(hreq.getRequestURI());

        // 隐藏SavedRequest到session中，以后使用
        session.setNote(Constants.FORM_REQUEST_NOTE, saved);

    }


    /**
     * 返回保存的请求的URI (使用相应的查询字符串)，这样就可以重定向到它
     *
     * @param session Our current session
     */
    private String savedRequestURL(Session session) {

        SavedRequest saved =
            (SavedRequest) session.getNote(Constants.FORM_REQUEST_NOTE);
        if (saved == null)
            return (null);
        StringBuffer sb = new StringBuffer(saved.getRequestURI());
        if (saved.getQueryString() != null) {
            sb.append('?');
            sb.append(saved.getQueryString());
        }
        return (sb.toString());
    }
}
