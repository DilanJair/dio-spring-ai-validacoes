# DIO Spring Boot - Final Project 05: Spring AI (budgeting)

## Evolução implementada

Esta versão acrescenta validação antes do registro de uma transação. O caso de uso
`PersistTransactionUseCase` é utilizado pelo endpoint REST e está registrado como
ferramenta do assistente de IA. Ele exige uma descrição preenchida, um valor
positivo em centavos e uma categoria informada. Dados inválidos geram uma exceção
antes de chamar o repositório.

A entrada e a persistência usam centavos inteiros (long). A saída do caso de uso
e a resposta REST usam reais (BigDecimal). Por exemplo, um POST com
`"amount": 3590` grava 3590 centavos e retorna `"amount": 35.90` no JSON.

No REST, `ApiExceptionHandler` converte apenas InvalidTransactionException em HTTP 400 com uma resposta
`ProblemDetail`. Por exemplo, um valor igual a zero retorna o detalhe
`O valor deve ser maior que zero.`

## Testar a melhoria gratuitamente

Use o JDK 25. No PowerShell, abra esta pasta e execute os testes que chamam o caso
de uso e o controller com dependências simuladas.

```powershell
.\gradlew.bat test --tests '*PersistTransactionUseCaseTest' --tests '*TransactionControllerTest'
```

Os testes verificam registros válidos, descrições vazias, valores zero ou
negativos, categorias ausentes, entrada ausente e respostas HTTP 201 e 400.
Nos casos inválidos, verificam também que o repositório recebeu zero chamadas.
O repositório e os componentes de IA são mocks do Mockito. O MockMvc exercita o
mapeamento HTTP e a conversão de JSON diretamente no teste Java. Ele é configurado
em modo standalone, com registro manual de ApiExceptionHandler, sem carregar
o contexto completo do Spring.

O arquivo `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
configura mocks de interfaces por proxies Java para executar os testes em
ambientes onde a instalação dinâmica de um agente na JVM está restrita.

Essa execução usa JUnit e Mockito com as dependências baixadas pelo Gradle. Ela
funciona sem banco iniciado, credencial da OpenAI ou modelo de IA instalado.

Para executar toda a suíte com as integrações externas desabilitadas, use um
terminal sem a variável `OPENAI_API_KEY` configurada e rode
`.\gradlew.bat test`. Os testes `OpenAi*IT`, `ToolCallingIT` e
`BudgetingApplicationTests` usam `@EnabledIfEnvironmentVariable` e ficam
ignorados nessa situação. O último carrega a aplicação completa e requer o banco
e a configuração de IA da base.

O relatório fica em `build/reports/tests/test/index.html`. No PowerShell, abra-o
com `Start-Process .\build\reports\tests\test\index.html`.

Na execução desta correção com JDK 25, Gradle 9.4.1 e --rerun-tasks, os 25 casos
selecionados passaram (13 do caso de uso e 12 do controller). Eles incluem a
conversão para reais, a precisão de valores grandes e a propagação de falhas do
repositório. Na verificação anterior, os 21 casos do recorte passaram e uma
execução da suíte completa registrou seis testes de integração ignorados por
ausência de `OPENAI_API_KEY`. O relatório gerado na sua máquina
mostra os resultados da sua execução.

## Escopo da verificação e aprendizado

A validação da melhoria pode ser reproduzida pelos testes automatizados acima.
A transcrição real de áudio, a seleção de ferramentas por um modelo real, a
geração de voz e o fluxo com o MySQL precisam de verificação de integração à
parte. Os mocks permitem controlar as dependências para conferir as regras
Java e as respostas HTTP.

O aprendizado desta evolução envolve centralizar a regra no caso de uso,
transformar erros de entrada em respostas HTTP adequadas e usar testes para
comprovar que uma operação inválida é rejeitada antes da persistência.

O projeto-base foi desenvolvido pelo expert Poiani e disponibilizado pela DIO.
Referência original em
[dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai).

## Introduction

This final module applies Spring AI in a budgeting API while preserving the same layered architecture used across the track.

The goal is to integrate AI capabilities without bypassing domain and use case boundaries.

## Code Context

The project processes voice commands to create and query financial transactions.

Primary flow:

1. Client uploads an audio file.
2. Audio is transcribed into text.
3. The model selects an application tool/use case.
4. The use case persists or queries transaction data.
5. The final response is converted to audio.

## Project Structure

- `src/main/java/dio/budgeting/domain`
  - Domain model and repository contract.
- `src/main/java/dio/budgeting/application`
  - Use cases used by both REST and AI tool calling.
- `src/main/java/dio/budgeting/infrastructure`
  - HTTP adapters, JPA adapters, and integration glue.

## Module-Specific Topics

### Speech-to-text

- Uses `TranscriptionModel` for audio transcription.
- Model settings are configured in `application.properties`.

### Tool calling

- `ChatClient` registers use-case tools.
- `@Tool` methods expose business capabilities to the model.

### Text-to-speech

- `TextToSpeechModel` produces MP3 output from final text.
- AI endpoint returns generated audio.

## Spring AI Documentation

- Spring AI Reference: https://docs.spring.io/spring-ai/reference/index.html
- ChatModel API: https://docs.spring.io/spring-ai/reference/api/chatmodel.html
- ChatClient API: https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Tools API: https://docs.spring.io/spring-ai/reference/api/tools.html
- Audio Transcriptions API: https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html
- Audio Speech API: https://docs.spring.io/spring-ai/reference/api/audio/speech.html

## Shared Architecture References

Common architecture concepts are documented in the root README:

- [DDD layers](../README.md#ddd-layered-architecture)
- [Class vs record](../README.md#java-class-vs-java-record-in-domain-modeling)
- [Strong typed identifiers](../README.md#strong-typed-identifiers)
- [Repository pattern](../README.md#repository-pattern)
- [Use cases and Clean Architecture](../README.md#use-cases-and-clean-architecture)
- [Docker Compose support](../README.md#docker-compose-support-in-development)

## How to Run

Set your OpenAI API key:

```bash
export OPENAI_API_KEY="your_api_key_here"
```

Run the application and tests:

```bash
./gradlew bootRun
./gradlew test
```

## Notes

- Educational final project focused on AI plus architectural discipline.
- External provider integration tests may require active credentials.
