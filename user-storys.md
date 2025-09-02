## *Histórias de usuário*

---

#### História de Usuário 1:
 *Como usuário, gostaria de visualizar os postos de alertas da minha região por meio de um mapa interativo, para ter conhecimento dos lugares com possível risco.*

##### Critério de Aceitação:

1. O aplicativo deve exibir um mapa interativo com a posição dos postos de alerta mais próximos da localização do usuário.

2.  Cada posto deve ser representado por um marcador visual com cores indicando o nível de risco (por exemplo: verde, amarelo, vermelho).

3.  O usuário deve poder tocar em cada posto no mapa para visualizar informações como: nome, status, última atualização e nível de alerta.

4.  O mapa deve permitir zoom e navegação para explorar outras áreas além da localização atual.

5.  O aplicativo deve atualizar automaticamente os dados dos postos de alerta a cada 1 minuto.

---

#### História de Usuário 2:

*Como usuário, gostaria de acessar os detalhes de um posto de alerta da minha região, para visualizar os dados atuais de temperatura, umidade, pressão e possibilidade de alagamento daquela região.*

##### Critério de Aceitação: 
1. O aplicativo deve exibir os dados detalhados ao selecionar um posto de alerta no mapa.

2. Os dados exibidos devem incluir pelo menos: nome/ID do posto, localização (bairro, rua), temperatura do ambiente (em °C), umidade do ar (em porcentagem), pressão (em hPa), e status de risco (sem risco, baixo, médio, alto).

3.O aplicativo deve exibir um aviso visual (ex: cor dos ícones e letras) de acordo com o risco identificado.

---

#### História de Usuário 3:

*Como usuário, gostaria de visualizar a previsão do tempo para o dia e os próximos dias, para planejar meus deslocamentos com mais segurança. & Essencial*

##### Critério de Aceitação: 
1. O aplicativo deve exibir o nome da localidade atual (ex: Itacoatiara) deve ser exibido no topo da tela.

2. O usuário deve visualizar a temperatura atual da cidade, exibida com destaque (ex: 24,7°C).

3. O usuário deve visualizar também informações como umidade relativa (\%), pressão atmosférica (hPa) e precipitação (mm).

4. O usuário deve ver a indicação de risco de alagamento (ex: "Sem Risco") com um ícone e intensidade atual de chuva (ex: 0,0 mm/s).

---


#### História de Usuário 4:

*Como usuário, gostaria de receber notificações de alerta do posto de alerta mais próximo de minha residência, para que eu possa agir rapidamente em casos de risco.*

##### Critério de Aceitação:
1. O aplicativo deve identificar o posto mais próximo com base na localização ou local salvo como "residência".

2. O usuário deve receber uma notificação automática sempre que o posto entrar em status “Moderado”.

3. As notificações devem indicar o nível de risco, data/hora da detecção e uma orientação básica.

4. O usuário deve ter a opção de ativar/desativar esse tipo específico de notificação nas configurações.

---


#### História de Usuário 5:

*Como usuário, gostaria de ativar/desativar o recebimento de notificações baseadas na minha localização atual, para evitar alertas desnecessários.*

##### Critério de Aceitação:
1. O aplicativo deve permitir ao usuário ativar ou desativar o rastreamento de localização para envio de alertas.

2. Quando desativado, o app deve deixar de enviar alertas baseados em localização atual, mas manter os baseados em localizações salvas.

3. O usuário deve ser informado sobre o impacto de desativar essa funcionalidade (ex: “você pode deixar de receber alertas importantes”).
