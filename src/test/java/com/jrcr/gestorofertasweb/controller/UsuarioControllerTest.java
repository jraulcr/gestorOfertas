package com.jrcr.gestorofertasweb.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jrcr.gestorofertasweb.model.TipoMensaje;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.UsuarioRepository;

/**
 * Tests de integración para {@link UsuarioController} usando MockMvc.
 *
 * Las aserciones de 'tipoMensaje' usan el enum {@link TipoMensaje}
 * (no String) porque el controlador ya está refactorizado con enums.
 */
@WebMvcTest(UsuarioController.class)
@Import({UsuarioController.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    // ==========================================
    // GET /cambiar-password
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /cambiar-password debe mostrar el formulario")
    void mostrarCambiarPassword_conUsuarioAutenticado_deberiaMostrarFormulario() throws Exception {
        mockMvc.perform(get("/cambiar-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("cambiar-password"))
                .andExpect(model().attribute("username", "admin"));
    }

    @Test
    @DisplayName("GET /cambiar-password sin autenticación debe devolver 401")
    void mostrarCambiarPassword_sinAutenticacion_deberiaDevolver401() throws Exception {
        mockMvc.perform(get("/cambiar-password"))
                .andExpect(status().isUnauthorized());  // 401 en lugar de redirección
    }

    // ==========================================
    // POST /cambiar-password
    // ==========================================

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /cambiar-password con contraseñas que no coinciden debe volver al formulario")
    void procesarCambiarPassword_conContraseñasDiferentes_deberiaVolverAlFormulario() throws Exception {
        // Given
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("encoded");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        // When & Then
        mockMvc.perform(post("/cambiar-password")
                        .with(csrf())
                        .param("passwordActual", "admin123")
                        .param("passwordNueva", "nueva123")
                        .param("passwordConfirmar", "diferente"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cambiar-password"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.WARNING));  // Enum
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /cambiar-password con contraseña actual incorrecta debe volver al formulario")
    void procesarCambiarPassword_conPasswordActualIncorrecta_deberiaVolverAlFormulario() throws Exception {
        // Given
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("encoded");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/cambiar-password")
                        .with(csrf())
                        .param("passwordActual", "incorrecta")
                        .param("passwordNueva", "nueva123")
                        .param("passwordConfirmar", "nueva123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cambiar-password"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.ERROR));  // Enum
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /cambiar-password con username ya en uso debe volver al formulario")
    void procesarCambiarPassword_conUsernameEnUso_deberiaVolverAlFormulario() throws Exception {
        // Given
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("encoded");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByUsername("otroUsuario")).thenReturn(Optional.of(new Usuario()));

        // When & Then
        mockMvc.perform(post("/cambiar-password")
                        .with(csrf())
                        .param("passwordActual", "admin123")
                        .param("passwordNueva", "nueva123")
                        .param("passwordConfirmar", "nueva123")
                        .param("nuevoUsername", "otroUsuario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cambiar-password"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.WARNING));  // Enum
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /cambiar-password con datos válidos debe redirigir al home")
    void procesarCambiarPassword_conDatosValidos_deberiaRedirigirAHome() throws Exception {
        // Given
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("encoded");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("nueva123")).thenReturn("newEncoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When & Then
        mockMvc.perform(post("/cambiar-password")
                        .with(csrf())
                        .param("passwordActual", "admin123")
                        .param("passwordNueva", "nueva123")
                        .param("passwordConfirmar", "nueva123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.SUCCESS));  // Enum

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /cambiar-password con contraseña menor a 6 caracteres debe volver al formulario")
    void procesarCambiarPassword_conPasswordCorta_deberiaVolverAlFormulario() throws Exception {
        // Given
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("encoded");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        // When & Then
        mockMvc.perform(post("/cambiar-password")
                        .with(csrf())
                        .param("passwordActual", "admin123")
                        .param("passwordNueva", "123")
                        .param("passwordConfirmar", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cambiar-password"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.WARNING));  // Enum
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /cambiar-password con usuario no encontrado debe redirigir al home")
    void procesarCambiarPassword_conUsuarioNoEncontrado_deberiaRedirigirAHome() throws Exception {
        // Given
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/cambiar-password")
                        .with(csrf())
                        .param("passwordActual", "admin123")
                        .param("passwordNueva", "nueva123")
                        .param("passwordConfirmar", "nueva123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("tipoMensaje", TipoMensaje.ERROR));  // Enum
    }
}