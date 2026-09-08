# Checkpoint 4 — Bug Hunt StreamFIAP

## Identificação

**Grupo:** ___

| Integrante                | RM     | Turma |
|---------------------------|--------|-------|
| Enzo Cardilli Cerneviva   | 563480 | 2CCPX |
| Matheus Lara Carneiro     | 564049 | 2CCPX |
| Victor Hugo Almeida Bahia | 564633 | 2CCPX |

| Campo | |
|---|---|
| **Total de bugs corrigidos** | 12 / 12 |
| **Total de ajustes de Clean Code** | 6 / 6 (+1 adicional) |

---

## Parte 1 — Bugs encontrados

| # | Sintoma observado (o que fiz/vi) | Causa raiz (arquivo e linha aproximada) | Correção aplicada | Conceito da disciplina |
|---|---|---|---|---|
| bug01 | `GET /api/conteudos/999` (id inexistente) respondia **200 com corpo vazio** (`null`), como se fosse sucesso. | `ConteudoController.buscarPorId` (~l. 30): a `ConteudoNaoEncontradoException` caía num `try/catch (Exception e)` vazio e o método retornava `null`. | Removido o `try/catch`. A exceção agora propaga até o `GlobalExceptionHandler`, que responde **404** com a mensagem. | Tratamento de exceções: não silenciar exceção (catch vazio); `@RestControllerAdvice` (Aula 11) |
| bug02 | `GET /api/conteudos/categoria/FICCAO` retornava **lista vazia** mesmo havendo conteúdos dessa categoria. | `ConteudoController.listarPorCategoria` (~l. 45): comparava `c.getCategoria() == categoria` — identidade de referência entre `String`, quase sempre `false`. | Substituído o laço manual por `conteudoRepository.findByCategoria(categoria)` (query method que já existia e não era usado). | `==` vs `.equals()` em objetos; Spring Data query methods (Aulas 4 e 13) |
| bug03 | Série cadastrada vinha com `titulo`, `categoria`, `duracaoMinutos`, `classificacaoEtaria` nulos/zero; preço e validações erradas. | `Serie` (~l. 14): o construtor só atribuía `numeroTemporadas` e **não chamava `super(...)`**; o Java inseria `super()` sem argumentos. | Adicionado `super(titulo, categoria, duracaoMinutos, classificacaoEtaria, true)` como primeira linha do construtor. | Herança: cadeia de construtores e `super(...)` (Aula 7) |
| bug04 | Preço de qualquer série saía **R$ 9,90** (ou 7,92 com promoção), ignorando o número de temporadas. | `Serie` (~l. 19): o método era `calcularPrecoAluguel(double desconto)` — assinatura diferente da superclasse, logo **sobrecarga**, nunca chamada por `calcularPrecoPromocional()`. | Removido o parâmetro e adicionado `@Override`. Agora sobrescreve de fato: `4.90 * numeroTemporadas`. | Sobrescrita (override) × sobrecarga (overload); anotação `@Override` (Aula 7) |
| bug05 | Documentário era cobrado **R$ 9,90** em vez de ser gratuito. | `Documentario`: não sobrescrevia `calcularPrecoAluguel()`, herdava o `return 9.90` de `Conteudo`. | Adicionado `@Override public double calcularPrecoAluguel() { return 0.0; }`. | Polimorfismo: subclasse que não sobrescreve herda a implementação da superclasse (Aula 7/8) |
| bug06 | Consultar o preço promocional de um filme **aumentava** o valor (R$ 11,88) em vez de dar desconto. | `Filme.aplicarPromocao` (~l. 24): `return preco * 1.2`. | `return preco * 0.8` (20% de desconto), conforme o contrato da interface `Promocionavel`. | Interface como contrato; implementação de método de interface (Aula 8/9) |
| bug07 | `POST /api/usuarios` salvava o usuário com **`nome` nulo**. | `Usuario` (~l. 22): construtor com `nome = nome;` — atribuição do parâmetro a si mesmo (shadowing), o campo nunca era setado. | `this.nome = nome;`. | Escopo de variáveis / shadowing; palavra-chave `this` (Aula 3) |
| bug08 | `POST /api/usuarios` falhava ao persistir (id não era gerado). | `Usuario` (~l. 11): campo `id` com `@Id` mas **sem `@GeneratedValue`** — JPA esperava id atribuído manualmente. | Adicionado `@GeneratedValue(strategy = GenerationType.IDENTITY)`, igual a `Conteudo`. | JPA: mapeamento de identidade / geração de chave primária (Aula 13) |
| bug09 | Aluguel recusado mesmo com créditos suficientes; quando aceito, os créditos ficavam **negativos**. | `Usuario.temCreditosSuficientes` (~l. 28): `return preco >= this.creditos` — comparação invertida. | `return this.creditos >= preco;`. Como a checagem ocorre antes do débito, os créditos nunca ficam negativos. | Expressões booleanas / operadores relacionais; invariantes de domínio (Aulas 2 e 3) |
| bug10 | Era possível alugar um conteúdo **já alugado** (indisponível). | `Usuario.alugar` (~l. 36): não verificava `conteudo.isDisponivel()` antes de debitar e marcar como indisponível. | Adicionado `if (!conteudo.isDisponivel()) throw new ConteudoIndisponivelException(...)` — exceção que já existia e não era usada; o handler responde **409**. | Regras de negócio no model; uso de exceções customizadas (Aula 11) |
| bug11 | Cadastro com `duracaoMinutos = 0` ou negativa era salvo normalmente. | Nenhum ponto validava a duração antes do `save` (construtor de `Conteudo`, ~l. 25). | Validação no construtor de `Conteudo` (`if (duracaoMinutos <= 0) throw new IllegalArgumentException(...)`) + `@ExceptionHandler(IllegalArgumentException.class)` no `GlobalExceptionHandler` respondendo **400**. Nada é persistido. | Blindagem do objeto no construtor; tradução de exceção → resposta HTTP (Aulas 3, 4 e 11) |
| bug12 | Usuário abaixo da classificação indicativa recebia **500** (erro genérico) sem a mensagem da regra. | `ClassificacaoIndicativaException` era `checked` (`extends Exception`) e **não tinha `@ExceptionHandler`**. | Passou a `extends RuntimeException` (removidos os `throws` de `Usuario.alugar` e `AluguelController`) e ganhou handler no `GlobalExceptionHandler` respondendo **403** com a mensagem. | Exceções checked × unchecked; `@RestControllerAdvice` / `@ExceptionHandler` (Aula 11) |

## Parte 2 — Ajustes de Clean Code

| # | Onde estava | Qual princípio/boas práticas era violado | O que eu mudei |
|---|---|---|---|
| clean01 | `Usuario.alugar` — variáveis `c` (conteúdo) e `p` (preço) | Nomes reveladores de intenção — o código precisava de comentário pra ser entendido | Renomeei `c` → `conteudo` e `p` → `preco` |
| clean02 | `Conteudo` — campo `public int duracaoMinutos`, acessado direto (`filme.duracaoMinutos`) nos controllers | Encapsulamento — estado interno exposto | Tornei o campo `private`; `ConteudoController` passou a usar `getDuracaoMinutos()` nos três cadastros |
| clean03 | `ConteudoController` — método `private calcularDescontoAntigo(...)` nunca chamado | Código morto | Removi o método (e o comentário que o "justificava") |
| clean04 | `ConteudoController` — bloco de código comentado da "regra de cupons" com `// não apagar` | Código comentado / ruído — o histórico do git já preserva | Removi o bloco |
| clean05 | `Usuario.alugar` — 8 linhas de `System.out.println` imprimindo um "recibo" | Separação de responsabilidades — o model não deve fazer I/O de apresentação | Removi os `println`; a resposta da API já devolve o usuário com os créditos atualizados |
| clean06 | `Usuario.debitarCreditos` — comentário `// adiciona o valor aos créditos` enquanto o código subtrai | Comentário enganoso (pior que nenhum) | Corrigi para `// debita o valor dos créditos do usuário` |
| clean07 *(adicional)* | `Filme`, `Serie`, `Promocionavel` — literais de preço (`9.90`, `5.00`, `4.90`, `0.8`) espalhados | Números mágicos | Extraí para constantes nomeadas: `Filme.PRECO_BASE`, `Filme.ADICIONAL_ESTREIA`, `Serie.PRECO_POR_TEMPORADA`, `Promocionavel.FATOR_DESCONTO_PROMOCIONAL` |

---

## Parte 3 — Perguntas de reflexão

> **Observação para o grupo:** revisem e reescrevam estas respostas com as próprias
> palavras antes de entregar. O enunciado avisa que "respostas genéricas de tutorial
> não pontuam" e vocês podem ser questionados sobre elas.

### 1. Injeção de dependência (Aula 13)

Os controllers (`ConteudoController`, `UsuarioController`, `AluguelController`) declaram os repositories com `@Autowired` e nunca fazem `new`. O motivo é que `ConteudoRepository` é apenas uma **interface** — quem cria a implementação concreta em tempo de execução é o Spring Data JPA (via proxy dinâmico), então não existe uma classe para instanciar com `new`. Além disso, esse objeto precisa de colaboradores internos (o `EntityManager`, a fonte de dados, o gerenciador de transações), todos também gerenciados pelo container. Ao "injetar um bean", o Spring: (1) cria a instância uma única vez, (2) resolve e injeta as dependências dela, (3) guarda no contêiner e entrega a mesma referência a quem precisar. Um `new` comum criaria um objeto solto, sem `EntityManager`, sem transação e sem proxy — os métodos de CRUD simplesmente não funcionariam.

### 2. JDBC vs Spring Data JPA (Aulas 12 e 13)

No `ProdutoDAO` da Aula 12 escrevíamos tudo à mão: abrir `Connection`, montar `PreparedStatement`, iterar `ResultSet`, mapear coluna a coluna para o objeto e fechar os recursos. Aqui, `ConteudoRepository extends JpaRepository<Conteudo, Long>` e ganha `save`, `findById`, `findAll`, `deleteById` etc. sem nenhuma implementação. O Spring Data automatiza: geração do SQL, mapeamento objeto-relacional (via anotações `@Entity`, `@Id`, herança), gerenciamento de conexão e transação. O `findByCategoria` funciona porque o Spring **deriva a query do nome do método** ("findBy" + campo `categoria`) e gera o `WHERE categoria = ?`. O JDBC/DAO ainda é preferível quando se precisa de SQL muito específico, otimização fina de performance, procedures ou controle total sobre a conexão — casos em que o ORM atrapalha mais do que ajuda.

### 3. Exceções checked vs unchecked (Aula 11)

`extends Exception` cria uma exceção **checked**: o compilador obriga a declarar `throws` ou tratar em `try/catch` em toda a cadeia de chamadas. Era o caso da `ClassificacaoIndicativaException`, que subia por `Usuario.alugar` → `AluguelController` como um `throws` "burocrático" e, sem handler, o Spring devolvia um **500 genérico** sem a mensagem. `extends RuntimeException` cria uma exceção **unchecked**: ela sobe sozinha até onde alguém queira tratá-la. Mudamos para `RuntimeException`, removemos os `throws`, e adicionamos no `GlobalExceptionHandler` um `@ExceptionHandler(ClassificacaoIndicativaException.class)` que responde **403** com `e.getMessage()`. Assim a regra de negócio ("usuário de X anos não pode assistir...") chega clara ao cliente da API, com o status HTTP correto.

### 4. Sobrescrita vs sobrecarga (Aula 7)

`Serie` tinha `public double calcularPrecoAluguel(double desconto)`. Isso **não sobrescreve** `Conteudo.calcularPrecoAluguel()` — assinatura diferente (um parâmetro a mais) significa um método **novo** (sobrecarga). `calcularPrecoPromocional()` chama a versão **sem argumento**, que na `Serie` não existia, então caía na implementação da superclasse (`return 9.90`). É *override* quando o método filho tem exatamente a mesma assinatura do pai e substitui o comportamento; é *overload* quando só o nome coincide e a lista de parâmetros muda. A anotação `@Override` teria impedido o bug: colocada sobre `calcularPrecoAluguel(double)`, o compilador acusaria "método não sobrescreve nada da superclasse" e o erro apareceria já na compilação, não em produção.

### 5. Onde blindar o objeto? (Aulas 3, 4 e 13)

- **Construtor** — invariantes que nunca podem ser violadas nem no momento da criação: `duracaoMinutos > 0` (bug11) ficou no construtor de `Conteudo`, garantindo que não existe objeto inválido em memória.
- **Setter** — se um campo pode ser alterado depois, a mesma regra precisa valer ali também; senão dá pra criar válido e depois "estragar" com o setter.
- **Método de negócio do model** — regras que dependem de mais de um campo ou do estado de outro objeto: `alugar` valida disponibilidade (bug10), classificação e créditos (bug09), porque essas condições só fazem sentido no contexto da operação.

Validar só num lugar não bastou porque cada camada tem uma brecha diferente: o `nome = nome` (bug07) passou pelo construtor sem erro de compilação; a duração negativa entrava porque nem construtor nem setter checavam; e a disponibilidade não é um valor de entrada, é uma condição verificada na hora do aluguel. Blindar é defender **cada porta de entrada** do estado do objeto.

### 6. Abstração e interface (Aulas 8 e 9)

`Conteudo` é **classe abstrata**: define o que todo conteúdo *é* (título, categoria, duração, `calcularPrecoAluguel()`) e concentra o código comum; `Filme`, `Serie` e `Documentario` herdam e especializam. `Promocionavel` é **interface**: define o que uma classe *sabe fazer* (`aplicarPromocao`), sem herança e sem estado — `Filme` e `Serie` a implementam, `Documentario` não, e é isso que exclui o documentário das promoções (`calcularPrecoPromocional` testa `this instanceof Promocionavel`).

Se o Documentário passasse a ter promoção, bastaria: `Documentario implements Promocionavel` e implementar `aplicarPromocao`. **Nenhuma outra classe seria tocada** — nem `Conteudo`, nem `Filme`, nem `Serie`, nem o `ConteudoController`. Isso mostra que o sistema está bem desenhado: comportamento opcional entra por interface, e adicionar uma capacidade a uma classe é uma mudança local, não uma alteração em cascata.

---

