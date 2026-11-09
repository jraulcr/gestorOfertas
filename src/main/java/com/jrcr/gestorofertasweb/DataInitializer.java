package com.jrcr.gestorofertasweb;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.jrcr.gestorofertasweb.model.Oferta;
import com.jrcr.gestorofertasweb.model.Usuario;
import com.jrcr.gestorofertasweb.repository.OfertaRepository;
import com.jrcr.gestorofertasweb.repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    private final OfertaRepository ofertaRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-user.username}")
    private String defaultUsername;

    @Value("${app.default-user.password}")
    private String defaultPassword;

    DataInitializer(UsuarioRepository usuarioRepository, OfertaRepository ofertaRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.ofertaRepository = ofertaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Crear usuarios si no existen
        if (usuarioRepository.findByUsername(defaultUsername).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            usuarioRepository.save(admin);
        }

        if (usuarioRepository.findByUsername(defaultUsername).isEmpty()) {
            Usuario user = new Usuario();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode(defaultPassword));
            user.setRole("ROLE_USER");
            user.setEnabled(true);
            usuarioRepository.save(user);
        }

        // Crear ofertas de prueba si no existen
		usuarioRepository.findByUsername("admin").filter(admin -> ofertaRepository.count() == 0).ifPresent(admin -> {
			Oferta o1 = new Oferta();
			o1.setEmpresa("Google");
			o1.setPuesto("Desarrollador Java Senior");
			o1.setSector("Tecnología");
			o1.setModalidad("Teletrabajo");
			o1.setMetodoEnvio("LinkedIn");
			o1.setEstado("Inscrito");
			o1.setFechaPublicacion(LocalDate.now().minusDays(10));
			o1.setFechaEnvio(LocalDate.now().plusDays(5));
			o1.setUsuario(admin);
			ofertaRepository.save(o1);

			Oferta o2 = new Oferta();
			o2.setEmpresa("Amazon");
			o2.setPuesto("Backend Developer");
			o2.setSector("Tecnología");
			o2.setModalidad("Híbrido");
			o2.setMetodoEnvio("Web propia");
			o2.setEstado("En espera");
			o2.setFechaPublicacion(LocalDate.now().minusDays(5));
			o2.setFechaEnvio(LocalDate.now().plusDays(10));
			o2.setUsuario(admin);
			ofertaRepository.save(o2);

			Oferta o3 = new Oferta();
			o3.setEmpresa("Inditex");
			o3.setPuesto("Analista de Datos");
			o3.setSector("Retail");
			o3.setModalidad("Presencial");
			o3.setMetodoEnvio("Infojobs");
			o3.setEstado("Rechazado");
			o3.setFechaPublicacion(LocalDate.now().minusDays(20));
			o3.setFechaEnvio(LocalDate.now().minusDays(5));
			o3.setUsuario(admin);
			ofertaRepository.save(o3);

			System.out.println("Ofertas de prueba creadas");
		});
    }
}