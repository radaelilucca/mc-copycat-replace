# Build e validação

## Comandos

Executar na raiz, com JDK 21 em `JAVA_HOME`. O toolchain Forge usa Java 17; o resolver pode baixá-lo quando necessário. No Windows usar `gradlew.bat`; em Linux usar `./gradlew`.

```powershell
.\gradlew.bat buildAll
.\gradlew.bat verifyAll
.\gradlew.bat verifyAll -PwithoutCopycats
.\gradlew.bat runForgeClient
.\gradlew.bat runNeoForgeClient
.\gradlew.bat runForgeServer
.\gradlew.bat runNeoForgeServer
.\gradlew.bat :forge1201:runClientSmoke
.\gradlew.bat :neoforge:runClientSmoke
```

`buildAll` constrói os loaders em sequência. `verifyAll` verifica os artefatos e executa GameTests dos dois loaders em sequência. A opção `-PwithoutCopycats` mantém a integração compilada, mas remove Copycats+ do runtime. O VS Code oferece Build All como build padrão e tarefas individuais.

Para somente compilar e verificar os JARs, usar `buildAll`. `verifyAll` inicia servidores de GameTests; `runClientSmoke` abre um cliente. O `build` da raiz também inclui `check`/`verifyAll`. Executar as tarefas de jogo individualmente, conforme a memória disponível. Os scripts opcionais de multiplayer abrem um servidor e dois clientes e não fazem parte de `buildAll`.

Os artefatos ficam em `forge1201/build/libs/copycat_replace-forge-1.20.1-0.2.0.jar` e `neoforge/build/libs/copycat_replace-neoforge-1.21.1-0.2.0.jar`.

## Camadas de evidência

| Camada | Critério |
|---|---|
| Build | Compilação, empacotamento e reobfuscação Forge |
| Artefato | Metadados expandidos, loader/Java corretos, traduções/licença e ausência de classes de teste/dependências embarcadas |
| GameTests | Blocos e inventários reais no servidor, nos dois loaders, com e sem Copycats+ |
| Cliente | Entrada remapeada, pacotes reais, sincronização do bloco, preview, HUD, telas e eventos de som |
| Visual | Inspeção das capturas e comportamento durante transições |
| Multiplayer | Servidor dedicado com dois clientes, limite e estado por jogador, mudanças observadas remotamente e reconexão |
| Produção | Repetição usando o JAR Forge final reobfuscado em instalação normal |

GameTests cobrem ações individuais e conectadas, ordens de mãos, Creative/Survival, material inválido/igual, bypass, estoque parcial, inventário cheio, barreiras por tipo/material/diagonal, limites 1/64/256 e multipartes. Testes com jogadores simulados não substituem multiplayer com conexões reais.

O teste de cliente cria um mundo descartável em `build/client-smoke-copycats` ou `build/client-smoke-create`. `result.txt` registra PASS/FAIL e cada etapa; capturas ficam em `screenshots`. Ele usa as ações e canais reais do addon. Capturas precisam ser inspecionadas antes de declarar aprovação visual. O evento de som comprova despacho; não equivale a uma avaliação auditiva humana.

## Resultado desta implementação

Versão preparada: **0.2.0**. Os resultados de jogo abaixo foram obtidos antes do bump, com o código de produção ainda identificado como 0.1.0; a preparação da versão altera somente a identificação nos artefatos. O build e a verificação dos JARs foram repetidos para 0.2.0, sem iniciar Minecraft. Os testes práticos adicionais foram interrompidos a pedido do usuário por consumo de memória do host; sua continuação fica a cargo do usuário. Nenhuma instância de teste iniciada pelo agente permaneceu em execução.

| Camada | Forge 1.20.1 / Create 6.0.8 | NeoForge 1.21.1 / Create 6.0.10 |
|---|---|---|
| Build e artefatos | Aprovados, incluindo reobfuscação | Aprovados |
| GameTests com Copycats+ | 21/21 aprovados | 21/21 aprovados |
| GameTests sem Copycats+ | 17/17 aprovados | 17/17 aprovados |
| Cliente com e sem Copycats+ | 22 etapas aprovadas em cada execução | 22 etapas aprovadas em cada execução |
| Visual | Capturas de HUD/contornos inspecionadas; revisão prática completa pendente | Capturas de HUD/contornos inspecionadas; revisão prática completa pendente |
| Multiplayer com Copycats+ | Parcial; pendência de recarga abaixo | Aprovado no cenário automatizado com dois clientes |
| Multiplayer sem Copycats+ | Parcial; pendência de recarga abaixo | Pendente |
| Instalação normal com JAR final | Inicialização, aplicação, substituição, remoção e atualização remota verificadas; cenário completo pendente | Não repetida nesta etapa |
| Sons | Despacho de evento aprovado; avaliação auditiva pendente | Despacho de evento aprovado; avaliação auditiva pendente |

São **76 execuções de GameTests** nas quatro combinações, não 76 cenários diferentes. Os testes de cliente aprovados verificam clique real, tecla remapeada, abertura/fechamento de tela, limite recebido, preview e soltura da tecla. Eles não cobrem toda a matriz manual abaixo.

Evidências locais, mantidas nas pastas ignoradas de build:

- `build/version-0.2.0-build.log`: build e verificação dos artefatos 0.2.0, sem execução de jogo.
- `build/validation-all.log`: builds, artefatos e GameTests com Copycats+.
- `build/validation-without.log`: GameTests sem Copycats+.
- `<módulo>/build/client-smoke-copycats/result.txt` e `client-smoke-create/result.txt`: etapas e resultado do cliente.
- `neoforge/build/multiplayer-copycats/result.txt`: multiplayer aprovado, incluindo multipartes, recarga do limite, estado por jogador e desconexão/reconexão com tecla ativa.
- `forge1201/build/multiplayer-production-copycats/`: logs da instalação Forge normal com o JAR reobfuscado.

As capturas representativas estão em [Forge com preview](validation/forge-preview.png), [NeoForge com preview](validation/neoforge-preview.png) e [Forge após soltar a tecla](validation/forge-released.png). A execução remota do workflow de publicação não foi realizada.

### Pendência conhecida: recarga no Forge

No cenário Forge, o servidor carregou inicialmente o limite 3 e o enviou ao cliente. Após editar `config/copycat_replace-common.toml` para limite 1 durante a execução, a configuração em memória continuou em 3 e não foi observado o evento de recarga. O listener de `ModConfigEvent.Reloading` está registrado e agenda o envio na thread do servidor; falta esclarecer por que a alteração do arquivo não foi recebida nessa instalação.

O comportamento foi observado em desenvolvimento e na instalação normal Forge. O cenário parou na etapa de recarga, portanto não comprova as etapas posteriores de multipartes e reconexão no Forge. O encerramento posterior por desconexão dos clientes não identifica a causa da falta de recarga. Essa pendência impede declarar a validação completa.

Para reproduzir manualmente, iniciar o servidor com `max_connected_blocks = 3`, conectar um cliente e conferir o preview sobre quatro Copycats conectados. Editar o arquivo COMMON do servidor para `max_connected_blocks = 1` sem reiniciar e conferir o HUD e quantos blocos a ação altera. Registrar também o resultado após reconectar e após reiniciar o servidor. Repetir com e sem Copycats+.

### Matriz prática restante

Executar nos dois loaders, com e sem Copycats+, usando os JARs finais:

- Aplicar e substituir individualmente nas duas ordens de mãos; Sneak, materiais inválidos e bloqueio dos efeitos da wrench.
- Aplicar, substituir e remover grupos; seis faces, diagonais excluídas, tipos/materiais distintos e limites 1, 64 e 256.
- Survival e Creative; estoque insuficiente, execução parcial, inventário cheio e devoluções sem perda/duplicação.
- Multipartes com materiais iguais e diferentes; seleção da parte, transferência do item pago e remoção.
- Tecla remapeada, telas, troca de alvo/material, movimento e soltura; revisão visual contínua de HUD/contornos e escuta dos sons.
- Servidor dedicado com dois jogadores; alterações remotas, isolamento da tecla, recarga do limite, desconexão/reconexão e inicialização sem Copycats+.

Os resultados automatizados acima são evidência parcial dessa matriz. O aceite prático completo permanece pendente.
