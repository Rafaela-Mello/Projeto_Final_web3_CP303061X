package com.ifsp.users.controllers;

import com.ifsp.users.dtos.EmailDto;
import com.ifsp.users.dtos.EmailRequest;
import com.ifsp.users.dtos.LoginUserDto;
import com.ifsp.users.dtos.RecoveryJwtTokenDto;
import com.ifsp.users.dtos.VerifyCodeRequest;
import com.ifsp.users.producers.UserProducer;
import com.ifsp.users.services.CodigoCacheService;
import com.ifsp.users.services.JwtTokenService;
import com.ifsp.users.services.UserDetailsImpl;
import com.ifsp.users.services.UserDetailsServiceImpl;
import com.ifsp.users.services.UserService;
import org.springframework.amqp.AmqpException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CodigoCacheService codigoCacheService;
    private final UserService userService;
    private final UserProducer userProducer;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtTokenService jwtTokenService;

    public AuthController(
            CodigoCacheService codigoCacheService,
            UserService userService,
            UserProducer userProducer,
            UserDetailsServiceImpl userDetailsService,
            JwtTokenService jwtTokenService
    ) {
        this.codigoCacheService = codigoCacheService;
        this.userService = userService;
        this.userProducer = userProducer;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<RecoveryJwtTokenDto> login(@RequestBody LoginUserDto dto) {
        RecoveryJwtTokenDto token = userService.authenticateUser(dto);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/request-code")
    public ResponseEntity<?> requestCode(@RequestBody EmailRequest request) {
        var email = request.email();
        var codigo = String.format("%06d", new Random().nextInt(999999));

        var user = userService.getOrCreateUserForCode(email);

        codigoCacheService.salvarCodigo(email, codigo);

        var emailDto = new EmailDto(
                user.getId(),
                email,
                "Seu código de acesso",
                "Seu código de verificação é: " + codigo + ". Ele expira em 5 minutos."
        );

        try {
            userProducer.sendEmail(emailDto);
        } catch (AmqpException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Falha ao publicar e-mail na fila RabbitMQ. Verifique as credenciais."));
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest request) {
        var codigoSalvo = codigoCacheService.obterCodigoValido(request.email());

        if (codigoSalvo == null || !codigoSalvo.equals(request.code())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Código inválido ou expirado"));
        }

        codigoCacheService.removerCodigo(request.email());

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(request.email());
        String token = jwtTokenService.generateToken(userDetails);

        return ResponseEntity.ok(new RecoveryJwtTokenDto(token));
    }
}
