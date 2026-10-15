package br.com.gestaofrota;

import br.com.gestaofrota.infra.security.services.CustomUserDetailsService;
import br.com.gestaofrota.infra.startup.AdminInitializer;
import br.com.gestaofrota.infra.startup.AdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GestaoFrotaApplicationTests {

	@MockitoBean
	private AdminInitializer adminInitializer;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Autowired
	private AdminProperties adminProperties;

	@Test
	void contextLoadsAndBindsAdminProperties() {
		assertThat(adminProperties.name()).isEqualTo("Administrador de Teste");
		assertThat(adminProperties.email()).isEqualTo("admin.test@exemplo.com");
		assertThat(adminProperties.password()).isEqualTo("senha-de-teste");
		assertThat(adminProperties.sectorName()).isEqualTo("Setor de Teste");
	}

}
