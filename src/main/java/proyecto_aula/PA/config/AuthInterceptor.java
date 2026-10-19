package proyecto_aula.PA.config;


import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.*;

public class AuthInterceptor implements HandlerInterceptor {

    @Override 
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws Exception{
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("usuario") == null){
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
        
    }
}
