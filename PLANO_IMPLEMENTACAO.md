# Plano de implementação — Create: Copycat Replace!

## Objetivo

Permitir que o jogador substitua diretamente o material de um copycat ao clicar nele com:

- a wrench do Create em uma mão; e
- um bloco de material na outra mão.

As duas orientações devem funcionar: wrench na mão principal ou na mão secundária. O comportamento deve cobrir os copycats nativos do Create e, quando instalado, todos os formatos do Create: Copycats+.

## Dependências e limites

- Minecraft `1.21.1` e NeoForge `21.1.248`.
- Create (`create`) é obrigatório. A implementação será validada inicialmente contra `6.0.10`.
- Create: Copycats+ (`copycats`) é opcional. A integração será validada inicialmente contra `3.0.4+mc.1.21.1-neoforge`.
- Os JARs locais ficam em `libs/` apenas como dependências de desenvolvimento; eles não serão incluídos dentro do JAR deste mod.
- Flywheel `1.0.6`, Ponder `1.0.82` e Registrate já vêm embutidos no JAR do Create por Jar-in-Jar; não devem ser declarados ou copiados separadamente.

Configuração Gradle implementada:

- Create como `implementation`, disponível para compilação e execução local.
- Copycats+ como `compileOnly` e `localRuntime`, para que continue opcional para o usuário final, mas esteja presente nos testes locais.
- Declarar `create` como dependência `required` e `copycats` como `optional` em `neoforge.mods.toml`.

## Arquitetura mínima

### 1. Um único listener de interação

Escutar `PlayerInteractEvent.RightClickBlock` no game event bus, com prioridade alta.

O listener processará somente o evento da mão principal. Ele sempre examinará os dois slots do jogador, evitando executar a troca duas vezes e permitindo as duas combinações de mãos.

Fluxo:

1. Confirmar que uma das mãos contém a wrench do Create e a outra contém o material.
2. Confirmar que o bloco atingido é um copycat suportado.
3. Cancelar a interação normal da wrench, impedindo que ela remova, gire ou desmonte o copycat durante a substituição.
4. Validar o material usando a própria API do bloco copycat.
5. No servidor, trocar o material e acertar o inventário de forma atômica.
6. Retornar `InteractionResult.sidedSuccess(...)` para animação e previsão corretas no cliente.

Se o material for inválido, a interação será consumida sem alterar o copycat. Isso evita que uma tentativa de substituição remova acidentalmente o material atual.

### 2. Adaptador para copycats do Create

Reconhecer blocos derivados de `CopycatBlock` e acessar seu `CopycatBlockEntity`.

- Obter o novo estado por `CopycatBlock#getAcceptedBlockState(...)`.
- Normalizar o estado por `CopycatBlock#prepareMaterial(...)`.
- Ler o item anteriormente consumido.
- Aplicar `setMaterial(...)` e `setConsumedItem(...)`.

Essa rota cobre os copycats mantidos diretamente pelo Create sem depender de IDs de registro específicos.

### 3. Compatibilidade isolada com Copycats+

Colocar toda referência a classes do addon em um pacote de compatibilidade separado, carregado somente quando `ModList.isLoaded("copycats")` for verdadeiro. Assim, o mod inicia normalmente sem Copycats+.

Para copycats simples:

- reconhecer `ICopycatBlock` e `ICopycatBlockEntity`;
- usar `getAcceptedBlockState(...)` e `prepareMaterial(...)`;
- substituir material e item consumido pela API da block entity.

Para copycats multipartes:

- reconhecer `IMultiStateCopycatBlock` e `IMultiStateCopycatBlockEntity`;
- localizar a parte atingida com `getPropertyFromInteraction(...)` e o `BlockHitResult` original;
- substituir somente o material daquela parte por `setMaterial(property, state)` e `setConsumedItem(property, stack)`.

O uso das interfaces cobre automaticamente as variações atuais e futuras do addon que respeitem a API, sem manter uma lista de blocos.

### 4. Transação de inventário

No servidor e apenas depois de toda validação:

1. Guardar o item consumido pelo material antigo da parte atingida.
2. Consumir uma unidade do novo material em sobrevivência; não consumir em criativo.
3. Registrar uma cópia de tamanho 1 como item consumido pelo copycat.
4. Devolver o material antigo ao inventário do jogador; se não houver espaço, soltá-lo no mundo.
5. Marcar e sincronizar a block entity.

Trocar pelo mesmo estado atual será tratado como sucesso sem consumo ou devolução, evitando duplicação de itens.

## Estrutura implementada

```text
src/main/java/com/radaeli/copycatreplace/
  CopycatReplace.java
  interaction/
    CopycatReplaceHandler.java
  service/
    MaterialReplacement.java
    CreateCopycatAdapter.java
  compat/copycats/
    CopycatsPlusCompat.java
```

Não são necessários mixins, registros de blocos, pacotes de rede próprios, recursos gráficos ou configurações para a primeira versão.

## Ordem de implementação

1. Configurar as dependências locais no Gradle e os requisitos em `neoforge.mods.toml`.
2. Implementar o listener e a detecção das duas mãos.
3. Implementar e testar os copycats nativos do Create.
4. Adicionar a compatibilidade opcional com Copycats+, incluindo multipartes.
5. Implementar consumo, devolução e sincronização.
6. Executar o cliente e o servidor dedicado e corrigir qualquer diferença de lado lógico.

## Matriz de validação

- Wrench na mão principal e material na secundária.
- Material na mão principal e wrench na secundária.
- Copycat vazio e copycat já preenchido.
- Sobrevivência: consome o novo material e devolve o antigo exatamente uma vez.
- Criativo: não consome o novo material e não duplica o antigo.
- Inventário cheio: o material antigo é solto no mundo.
- Material recusado pelo copycat: nenhuma mudança e nenhuma ação normal da wrench.
- Copycats do Create: panel, step e bars.
- Copycats+ simples e multipartes, incluindo seleção de partes diferentes do mesmo bloco.
- Cliente sem Copycats+ instalado.
- Servidor dedicado com Create e com/sem Copycats+.

## Critério de conclusão

A funcionalidade estará pronta quando todas as combinações acima substituírem somente o material/parte atingida, com inventário consistente, sem mixins e sem tornar Copycats+ obrigatório.

## Status

Implementação concluída em 21 de agosto de 2026:

- listener único para as duas combinações de mãos;
- adaptador para os copycats nativos do Create;
- integração opcional com copycats simples e multipartes do Copycats+;
- consumo, transferência e devolução dos itens de material;
- task `NeoForge: Run Client` para VS Code;
- build Gradle aprovado;
- cliente e servidor dedicado inicializados com sucesso com e sem Copycats+.

A validação de carregamento automatizada está concluída. A matriz de interações
dentro do jogo permanece como roteiro de teste manual de aceitação.
