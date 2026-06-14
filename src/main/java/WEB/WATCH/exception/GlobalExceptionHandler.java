package WEB.WATCH.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isAjaxRequest(jakarta.servlet.http.HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        // Kiểm tra X-Requested-With cho AJAX truyền thống và Accept header cho fetch API
        return "XMLHttpRequest".equals(requestedWith) || 
               (accept != null && accept.contains("application/json") && !accept.contains("text/html"));
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, Model model, jakarta.servlet.http.HttpServletRequest request) {
        String message = "Đã xảy ra lỗi không mong muốn: " + ex.getMessage();
        ex.printStackTrace(); // Log lỗi ra console để debug
        
        if (isAjaxRequest(request)) {
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("status", 500);
            body.put("error", "Internal Server Error");
            body.put("message", message);
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body);
        }

        model.addAttribute("errorMessage", message);
        return "error";
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFoundException(UsernameNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex, Model model, jakarta.servlet.http.HttpServletRequest request) {
        if (isAjaxRequest(request)) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}
