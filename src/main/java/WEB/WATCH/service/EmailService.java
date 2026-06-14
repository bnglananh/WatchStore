package WEB.WATCH.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã xác nhận đặt lại mật khẩu - Watch Store");
        message.setText("Chào bạn,\n\n" +
                "Mã xác nhận để đặt lại mật khẩu của bạn là: " + code + "\n" +
                "Mã này sẽ hết hạn sau 1 giờ.\n\n" +
                "Nếu bạn không yêu cầu đổi mật khẩu, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ Watch Store");
        mailSender.send(message);
    }
}
