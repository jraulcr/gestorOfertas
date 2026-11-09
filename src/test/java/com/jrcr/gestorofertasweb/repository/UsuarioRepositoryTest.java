package com.jrcr.gestorofertasweb.repository;

import com.jrcr.gestorofertasweb.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para {@link UsuarioRepository} usando BBDD en memoria (H2).
 *
 * Verifica que los métodos del repositorio funcionen correctamente
 * con datos reales en base de datos.
 */
@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        // Limpiar la BBDD antes de cada test
        entityManager.clear();
    }

    // ==========================================
    // TESTS DE findByUsername
    // ==========================================

    @Test
    @DisplayName("Debe encontrar usuario por username existente")
    void findByUsername_conUsernameExistente_deberiaDevolverUsuario() {
        // Given
        Usuario usuario = crearUsuario("admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Usuario> resultado = usuarioRepository.findByUsername("admin");

        // Then
        assertTrue(resultado.isPresent());
        assertEquals("admin", resultado.get().getUsername());
        assertEquals("encoded123", resultado.get().getPassword());
    }

    @Test
    @DisplayName("Debe devolver Optional.empty cuando el username no existe")
    void findByUsername_conUsernameInexistente_deberiaDevolverEmpty() {
        // Given
        Usuario usuario = crearUsuario("admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Usuario> resultado = usuarioRepository.findByUsername("usuario_inexistente");

        // Then
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Debe ser case-sensitive al buscar por username")
    void findByUsername_deberiaSerCaseSensitive() {
        // Given
        Usuario usuario = crearUsuario("Admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Usuario> resultadoMinusculas = usuarioRepository.findByUsername("admin");
        Optional<Usuario> resultadoMayusculas = usuarioRepository.findByUsername("Admin");

        // Then
        assertTrue(resultadoMinusculas.isEmpty());
        assertTrue(resultadoMayusculas.isPresent());
    }

    // ==========================================
    // TESTS DE save (crear y actualizar)
    // ==========================================

    @Test
    @DisplayName("Debe guardar un nuevo usuario correctamente")
    void save_usuarioNuevo_deberiaPersistirlo() {
        // Given
        Usuario usuario = crearUsuario("nuevo_user", "password123", "USER");

        // When
        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertNotNull(guardado.getId());
        Optional<Usuario> encontrado = usuarioRepository.findById(guardado.getId());
        assertTrue(encontrado.isPresent());
        assertEquals("nuevo_user", encontrado.get().getUsername());
        assertEquals("password123", encontrado.get().getPassword());
        assertEquals("USER", encontrado.get().getRole());
    }

    @Test
    @DisplayName("Debe actualizar un usuario existente")
    void save_usuarioExistente_deberiaActualizarse() {
        // Given
        Usuario usuario = crearUsuario("admin", "password_antigua", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // Modificar el usuario
        usuario.setPassword("password_nueva");
        usuario.setRole("ADMIN");

        // When
        usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Usuario> actualizado = usuarioRepository.findByUsername("admin");
        assertTrue(actualizado.isPresent());
        assertEquals("password_nueva", actualizado.get().getPassword());
        assertEquals("ADMIN", actualizado.get().getRole());
    }

    // ==========================================
    // TESTS DE findById
    // ==========================================

    @Test
    @DisplayName("Debe encontrar usuario por ID existente")
    void findById_conIdExistente_deberiaDevolverUsuario() {
        // Given
        Usuario usuario = crearUsuario("admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Usuario> resultado = usuarioRepository.findById(usuario.getId());

        // Then
        assertTrue(resultado.isPresent());
        assertEquals("admin", resultado.get().getUsername());
    }

    @Test
    @DisplayName("Debe devolver Optional.empty cuando el ID no existe")
    void findById_conIdInexistente_deberiaDevolverEmpty() {
        // When
        Optional<Usuario> resultado = usuarioRepository.findById(999L);

        // Then
        assertTrue(resultado.isEmpty());
    }

    // ==========================================
    // TESTS DE existsByUsername (si existe en tu repositorio)
    // ==========================================

    @Test
    @DisplayName("Debe devolver true cuando el username existe")
    void existsByUsername_conUsernameExistente_deberiaDevolverTrue() {
        // Given
        Usuario usuario = crearUsuario("admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertTrue(usuarioRepository.existsByUsername("admin"));
    }

    @Test
    @DisplayName("Debe devolver false cuando el username no existe")
    void existsByUsername_conUsernameInexistente_deberiaDevolverFalse() {
        // Given
        Usuario usuario = crearUsuario("admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertFalse(usuarioRepository.existsByUsername("usuario_inexistente"));
    }

    // ==========================================
    // TESTS DE delete
    // ==========================================

    @Test
    @DisplayName("Debe eliminar un usuario existente")
    void delete_usuarioExistente_deberiaEliminarlo() {
        // Given
        Usuario usuario = crearUsuario("admin", "encoded123", "USER");
        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        // When
        usuarioRepository.delete(usuario);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Usuario> resultado = usuarioRepository.findByUsername("admin");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Debe contar correctamente los usuarios")
    void count_deberiaDevolverNumeroCorrecto() {
        // Given
        entityManager.persist(crearUsuario("user1", "pass1", "USER"));
        entityManager.persist(crearUsuario("user2", "pass2", "USER"));
        entityManager.persist(crearUsuario("admin", "pass3", "ADMIN"));
        entityManager.flush();
        entityManager.clear();

        // When
        long total = usuarioRepository.count();

        // Then
        assertEquals(3, total);
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    /**
     * Crea un usuario con datos de prueba (NO lo persiste).
     */
    private Usuario crearUsuario(String username, String password, String role) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(password);
        usuario.setRole(role);
        return usuario;
    }
}