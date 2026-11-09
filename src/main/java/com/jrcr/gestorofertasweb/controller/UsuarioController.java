package com.jrcr.gestorofertasweb.controller;

import java.util.Objects;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jrcr.gestorofertasweb.model.TipoMensaje;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.UsuarioRepository;

/**
 * Controlador para operaciones de gestión de usuario:
 * - Cambio de contraseña
 * - Cambio de nombre de usuario
 *
 * Utiliza el enum {@link TipoMensaje} para los tipos de notificación
 * y {@link MessageSource} para externalizar todos los mensajes.
 */
@Controller
public class UsuarioController {

    private static final String FLASH_MENSAJE = "mensaje";
    private static final String FLASH_TIPO = "tipoMensaje";

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final MessageSource messageSource;

    UsuarioController(PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository, MessageSource messageSource) {
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.messageSource = messageSource;
    }

    private String msg(String clave, Object... args) {
        return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
    }

    @GetMapping("/cambiar-password")
    public String mostrarCambiarPassword(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String procesarCambiarPassword(
            @RequestParam String passwordActual,
            @RequestParam String passwordNueva,
            @RequestParam String passwordConfirmar,
            @RequestParam(required = false) String nuevoUsername,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String usernameActual = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(usernameActual).orElse(null);

        if (Objects.isNull(usuario)) {
            redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.notFound"));
            redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
            return "redirect:/home";
        }

        // --- LÓGICA PARA CAMBIAR NOMBRE DE USUARIO ---
        boolean usernameCambiado = false;
        if (Objects.nonNull(nuevoUsername) && !nuevoUsername.trim().isEmpty() && !nuevoUsername.equals(usernameActual)) {
            if (usuarioRepository.findByUsername(nuevoUsername.trim()).isPresent()) {
                redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.usernameTaken", nuevoUsername));
                redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.WARNING);
                return "redirect:/cambiar-password";
            }
            usuario.setUsername(nuevoUsername.trim());
            usernameCambiado = true;
        }

        // --- LÓGICA PARA CAMBIAR CONTRASEÑA ---
        if (Objects.nonNull(passwordNueva) && !passwordNueva.trim().isEmpty()) {

            if (passwordEncoder.matches(passwordNueva, usuario.getPassword())) {
                redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.passwordSameAsCurrent"));
                redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.WARNING);
                return "redirect:/cambiar-password";
            }

            if (!passwordNueva.equals(passwordConfirmar)) {
                redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.passwordMismatch"));
                redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.WARNING);
                return "redirect:/cambiar-password";
            }
            if (passwordNueva.length() < 6) {
                redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.passwordTooShort"));
                redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.WARNING);
                return "redirect:/cambiar-password";
            }
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                redirectAttributes.addFlashAttribute(FLASH_MENSAJE, msg("user.passwordIncorrect"));
                redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.ERROR);
                return "redirect:/cambiar-password";
            }
            usuario.setPassword(passwordEncoder.encode(passwordNueva));
        }

        usuarioRepository.save(usuario);

        String mensaje;
        if (usernameCambiado && Objects.nonNull(passwordNueva) && !passwordNueva.trim().isEmpty()) {
            mensaje = msg("user.updatedWithLogout");
        } else if (usernameCambiado) {
            mensaje = msg("user.usernameUpdated");
        } else {
            mensaje = msg("user.passwordUpdated");
        }

        redirectAttributes.addFlashAttribute(FLASH_MENSAJE, mensaje);
        redirectAttributes.addFlashAttribute(FLASH_TIPO, TipoMensaje.SUCCESS);

        return "redirect:/home";
    }
}