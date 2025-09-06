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

3. O aplicativo deve exibir um aviso visual (ex: cor dos ícones e letras) de acordo com o risco identificado.

---

#### História de Usuário 3:

*Como usuário, gostaria de visualizar as condições climáticas em tempo real, para planejar meus deslocamentos com mais segurança e me previnir de eventuais alagamentos ou enchentes que podem me afetar.*

##### Critério de Aceitação: 
1. O aplicativo deve exibir o nome da localidade atual (ex: Itacoatiara) deve ser exibido no topo da tela.

2. O usuário deve visualizar a temperatura atual da cidade, exibida com destaque (ex: 24,7°C).

3. O usuário deve visualizar também informações como umidade relativa (\%), pressão atmosférica (hPa) e precipitação (mm).

4. O usuário deve ver a indicação de risco de alagamento (ex: "Sem Risco") com um ícone e intensidade atual de chuva (ex: 0,0 mm/s).

---

#### História de Usuário 4:

*Como usuário, gostaria de escolher o posto de alerta clincado em algum deles todas as vezes que entrar no aplicativo, para que eu tenha a possibilidade de alterar a vizualização de diferentes postos ao entar no aplicativo.*

##### Critérios de Aceitação:

1. Ao iniciar o aplicativo, uma tela de seleção deve ser exibida ao usuário antes de qualquer outra interface principal, exibindo o mapa com os postos.

2. Ao clicar no posto exibido no mapa, o sistema deve abrir uma caixa de dialogo que exibirá as informações do posto, e dando ao usuário a opção de escolher aquele posto.

3. A tela do mapa deve apresentar uma opção que exibirá uma lista de todos os postos de alerta disponíveis e cadastrados no sistema. Cada item na lista deve exibir, no mínimo, o nome do posto e sua localização (bairro/rua) para fácil identificação.

4. Ao selecionar um posto da lista, o usuário deve ser redirecionado diretamente para a tela de detalhes daquele posto específico (conforme descrito na História de Usuário 2).

5. A tela de seleção deve conter uma opção para "Cancelar" ou "Ver no Mapa". Ao ser acionada, essa opção fecha a tela de seleção e leva o usuário para a visualização padrão do mapa (conforme a História de Usuário 1).


---

#### História de Usuário 5:

*Como usuário, gostaria de ter a possibilidade definir um posto de alerta como 'principal', para que o aplicativo abra diretamente nele, agilizando minha consulta diária.*

##### Critério de Aceitação:

1. Na tela de detalhes de qualquer posto de alerta (da História de Usuário 2), deve haver uma opção clara para o usuário marcar aquele posto como 'Principal'.

2. O usuário só pode ter um posto marcado como 'Principal'. Ao selecionar um novo posto como principal, o anterior deve ser desmarcado automaticamente.

3. O estado da seleção deve ser visualmente óbvio. O ícone do posto 'Principal' deve ser diferente dos demais (ex: estrela preenchida vs. estrela vazia). No mapa, o posto principal também pode ter um destaque diferente.

4. Ao iniciar o aplicativo, o sistema deve verificar se um posto 'Principal' foi definido. Se sim, o aplicativo deve carregar diretamente a tela de detalhes deste posto. Se não, o aplicativo deve iniciar em sua tela padrão.

5. O usuário deve poder desmarcar um posto como 'Principal', fazendo com que o aplicativo volte ao seu comportamento padrão de inicialização.

---

#### História de Usuário 6:

*Como usuário, gostaria de receber notificações de alerta dos postos de alerta que eu eu escolher, para que eu possa agir rapidamente em casos de risco.*

##### Critério de Aceitação:
1. Na tela de detalhes de cada posto de alerta, deve haver um botão para ativar as notificações daquele posto passar a receber notificações daquele posto específico.

2. O usuário pode receber notificações de múltiplos postos de alerta simultaneamente (ex: um perto de casa e outro perto do trabalho).

3. O usuário deve receber uma notificação push automática sempre que qualquer um dos postos em que ele está inscrito atingir um status de risco "Moderado" ou superior.

4. A notificação deve ser específica, indicando claramente qual posto está em risco, o nível do risco, a data/hora da detecção e uma orientação básica.

5. O usuário deve ter a opção de ativar/desativar esse tipo específico de notificação nas configurações.

6. Deve haver uma seção de "Configurações de Notificações" no aplicativo, onde o usuário possa ver uma lista de todos os postos em que está habilitado as notificações e ter a opção de cancelar a desabilitar as notificações de cada um individualmente.

7. A seção de configurações também deve ter uma opção separada para "Ativar/Desativar notificações por proximidade", que alerta o usuário sobre riscos com base em sua localização GPS atual.

---

#### História de Usuário 7:

*Como usuário, gostaria de visualizar o histórico de dados das condições climáticas para analisar se houver uma melhora ou uma piora da situação local.* 

##### Critério de Aceitação:

1. A aplicação deve abrir uma caixa de dialogo para cada fator climático apresentado no Dashboard.

2. O histórico de dados deve mostrar no máximo os últimos 15 valores de cada fator climático (Temperatura, Umidade, Pressão e Volume).

3. Cada valor registrado no histórico, deve ser apresentado com data, hora e seu valor com sua unidade de medida.