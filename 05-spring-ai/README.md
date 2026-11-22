# API de orçamento com Spring AI

Projeto do bootcamp DIO que evolui a API de orçamento apresentada pelo expert Poiani.
A aplicação recebe comandos de voz, transcreve o áudio, usa ferramentas de IA para registrar ou consultar transações e gera uma resposta em áudio.

## Melhoria implementada

As validações ficam em `PersistTransactionUseCase`, compartilhado pelo endpoint REST e pela ferramenta de IA:

- Dados da transação devem estar presentes.
- A descrição deve estar preenchida.
- O valor deve ser maior que zero.
- A categoria deve estar informada.

Dados inválidos são rejeitados antes de chamar o repositório. No REST, `InvalidTransactionException` é convertida em HTTP 400 com `ProblemDetail`.

Também foi corrigida a conversão monetária: entrada e persistência usam `long` em centavos; a saída usa `BigDecimal` em reais. Por exemplo, `3590` centavos resultam em `35.90` reais.

## Tecnologias

- Java 25, Spring Boot 4.0.5 e Spring AI 2.0.0-M4.
- Spring Web, Spring Data JPA e MySQL 9.6 com Docker Compose.
- Gradle Wrapper 9.4.1 e Lombok.
- JUnit, Mockito e MockMvc.

## Testar a melhoria sem banco ou API de IA

É necessário ter o JDK 25. Abra o PowerShell na pasta `05-spring-ai` e execute:

```powershell
.\gradlew.bat test --tests '*PersistTransactionUseCaseTest' --tests '*TransactionControllerTest'
```

O Gradle baixa as dependências na primeira execução. Esses testes usam mocks e não precisam de MySQL, chave da OpenAI ou modelo local.

São verificados dados válidos e inválidos, ausência de chamadas ao repositório nos casos rejeitados, respostas HTTP e precisão monetária. O MockMvc usa o controller real com dependências simuladas e registro manual do handler, sem carregar a aplicação completa.

Na última revisão da versão publicada, a suíte completa, executada sem `OPENAI_API_KEY`, registrou:

- **25 testes aprovados:** 13 do caso de uso e 12 do controller.
- **Seis registros de integração ignorados**, por dependerem da configuração externa.
- **Nenhuma falha.**

O relatório da sua execução fica em `build/reports/tests/test/index.html`.

## Executar a aplicação completa

Este modo requer JDK 25, Docker em execução com Docker Compose e uma chave própria da API da OpenAI.

Na pasta `05-spring-ai`, execute no PowerShell:

```powershell
docker compose up -d database
$env:OPENAI_API_KEY = 'SUA_CHAVE_DA_OPENAI'
.\gradlew.bat bootRun
```

O MySQL usa a porta `3307` do computador. Os exemplos abaixo consideram a API em `http://localhost:8080`.

## Exemplos de uso da API

Com a aplicação em execução, abra outro PowerShell na pasta `05-spring-ai`.

### Criar uma transação

Envie o valor em centavos. O endpoint retorna HTTP 201 com os dados da transação e o valor em reais.

```powershell
$transaction = @{
    category = 'GROCERIES'
    description = 'Compra no mercado'
    amount = 3590
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/transactions' -ContentType 'application/json' -Body $transaction
```

Neste exemplo, o campo `amount` da resposta representa `35.90` reais.

### Consultar por categoria

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/transactions/GROCERIES'
```

As categorias disponíveis são `GROCERIES`, `PHARMA` e `AUTO`.

### Enviar um comando de voz

O campo multipart deve se chamar `file`. Este exemplo usa um áudio incluído no projeto e salva a resposta como `resposta.mp3`.

```powershell
curl.exe --fail --show-error --form 'file=@src/test/resources/audio/recording-1.m4a' --output 'resposta.mp3' 'http://localhost:8080/transactions/ai'
```

**Limite da validação:** os exemplos de API acima são instruções de uso. O fluxo completo com MySQL e OpenAI reais não foi validado nesta evolução; os resultados comprovados são os testes com dependências simuladas.

## Aprendizado

O desenvolvimento e a revisão exercitaram a centralização de regras no caso de uso, o tratamento de erros HTTP, a precisão de valores monetários e o uso de mocks para testar o comportamento da aplicação. Também reforçaram a diferença entre testes isolados e testes de integração.

## Créditos

Projeto-base desenvolvido pelo expert Poiani e disponibilizado pela DIO:

[Repositório original — módulo Spring AI](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai).
