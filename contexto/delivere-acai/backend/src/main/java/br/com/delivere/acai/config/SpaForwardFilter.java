package br.com.delivere.acai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Encaminha requisições GET que não são API nem arquivos estáticos para /index.html (SPA).
 * Permite que o frontend (Angular) seja servido pelo mesmo servidor e as rotas funcionem ao recarregar.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaForwardFilter extends OncePerRequestFilter {

    private static final Set<String> PREFIXES_IGNORADOS = Set.of("/api/", "/h2-console", "/error");
    private static final Set<String> EXTENSOES_ESTATICAS = Set.of("js", "css", "ico", "png", "jpg", "jpeg", "gif", "svg", "woff", "woff2", "ttf", "eot", "map");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (path == null) path = "";
        for (String prefix : PREFIXES_IGNORADOS) {
            if (path.startsWith(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        if (temExtensaoEstatica(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    private static boolean temExtensaoEstatica(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1 || lastDot < lastSlash) return false;
        String ext = path.substring(lastDot + 1).toLowerCase();
        return EXTENSOES_ESTATICAS.contains(ext);
    }
}
