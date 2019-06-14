package enrichmentapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import enrichmentapi.dto.out.ErrorDto;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthorizationFilter extends OncePerRequestFilter {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authorization = request.getHeader("Authorization");
        final String token = System.getenv("token");

        if (token != null && (authorization == null || !authorization.replaceFirst("Token ", "").equals(token))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(mapper.writeValueAsString(new ErrorDto("invalid credentials")));
        } else {
            filterChain.doFilter(request, response);
        }

    }
}