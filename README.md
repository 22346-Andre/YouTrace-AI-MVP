# YouTrace AI

MVP acadêmico para o Desafio de Inovação YouShop 2027 — Engenharia da Computação / André Lucas Reis da Silva.

Sistema de triagem de incidentes que combina relatos e logs simulados por serviço e janela temporal. Não é um produto oficial da YouShop, não usa dados reais e não está integrado à plataforma.

## Rodar agora (Windows, sem Maven ou Node)

1. Instale o **JDK 21** e confirme `java -version` e `javac -version` no terminal.
2. Extraia o ZIP. Abra o terminal na pasta `YouTraceAI` e execute `scripts\demo.bat` (no PowerShell: `.\scripts\demo.bat`).
3. Acesse http://localhost:8080 no navegador. Deixe o terminal aberto. Ctrl+C encerra.

No Linux/macOS: `./scripts/demo.sh`. Se a porta estiver em uso, `./scripts/demo.sh 8765` (Windows: altere o número 8080 na última linha do .bat).

A demo usa o mesmo motor Java e componente React que a aplicação principal, mas um servidor HTTP do JDK e React 16 UMD incluído apenas para funcionar offline; **não executa Spring Boot**. Use a versão padrão abaixo para continuar o desenvolvimento. Os dados são temporários e reiniciam ao encerrar.

## Versão padrão: Spring Boot + React

Requisitos: JDK 21, Maven 3.9+, Node.js 22 e acesso à internet para as dependências.

Terminal 1:
```sh
cd backend
mvn spring-boot:run
```
Terminal 2:
```sh
cd frontend
npm install
npm run dev
```
Acesse http://localhost:5173. O proxy Vite direciona `/api` ao backend na porta 8080. Para build frontend: `npm run build`.

Alternativa com Docker Compose (requer internet): na raiz, `docker compose up --build`, depois http://localhost:8080. Não execute a demo e a versão Docker simultaneamente na mesma porta.

## Demonstração para a banca (90 segundos)

- Dashboard inicial: 7 relatos, 3 grupos, 5 logs simulados, 1 relato sem classificação. Checkout agrupa 3 relatos, com 2 evidências recentes; o log antigo é excluído.
- Clique em Checkout e mostre a hipótese e o roteiro QA.
- Em Novo relato, clique no exemplo Checkout e em Analisar relato. O grupo passa a 4 relatos.
- Envie o exemplo Fora do escopo: o relato vai à revisão manual, sem inventar diagnóstico.
- Restaure o cenário.

## Arquitetura

React -> API REST (Spring Boot) -> IncidentEngine (Java 21) -> registros em memória.

| Método | Rota | Finalidade |
|---|---|---|
| GET | `/api/dashboard` | Dados, incidentes e fila de revisão |
| POST | `/api/reports` | Cadastrar relato (`application/x-www-form-urlencoded`: `actor`, `text`) |
| POST | `/api/reset` | Restaurar dados fictícios |

## Como funciona a IA

- Normalização de texto, remoção de acentos e palavras frequentes, redução simples de plurais.
- TF-IDF (frequência de termos ponderada pelo inverso da frequência nos documentos) calculado sobre três perfis de incidentes e o relato analisado.
- Similaridade de cosseno: compara os vetores e escolhe o perfil mais próximo se o escore >= 0,20 e a margem em relação ao segundo >= 0,035. Caso contrário, encaminha à revisão manual.
- Agrupa relatos pelo perfil predefinido e cruza logs do serviço em uma janela de ±15 minutos. Classifica a severidade por regras de volume e evidência.
- Sugere hipótese e roteiro QA previamente cadastrados para o perfil identificado. Não é IA generativa nem diagnóstico comprovado. Similaridade não é probabilidade de acerto.

O diferencial proposto é conectar triagem textual, observabilidade e QA em um único fluxo. Não alegamos exclusividade no mercado. A versão futura pode usar embeddings, pgvector, coleta de logs, rastreamento distribuído, RAG e avaliação humana.

## Testes executados e limites

O motor Java compilou e passou oito verificações em `tests/EngineTest.java`: dados iniciais, severidade, janela temporal, novos relatos de checkout/tracking, fora do escopo, validação de entrada, reset. A API do adaptador offline passou testes HTTP GET/POST, erro 400 e reset. As versões Spring Boot/Vite/Docker foram fornecidas em código, mas não puderam ser compiladas/executadas neste ambiente por falta de acesso às dependências. Testes com dados fictícios não demonstram precisão em produção.

Para repetir os testes Java, a partir da raiz:
```sh
mkdir -p build
javac -encoding UTF-8 -d build backend/src/main/java/com/youtrace/core/IncidentEngine.java tests/EngineTest.java
java -cp build EngineTest
```
No Windows, crie a pasta `build` se não existir; os mesmos comandos javac e java funcionam.

Limites: três categorias conhecidas, vocabulário limitado, sem persistência, autenticação, isolamento de empresas ou integração real. Não inserir CPF, cartões, tokens, e-mails ou logs reais. Não publicar a demo como serviço de produção. Antes de um piloto real: anonimização, RBAC, auditoria, retenção, validação humana, e avaliação em amostra rotulada por QA. Medir precisão/recall da triagem, tempo de diagnóstico e aceitação dos roteiros. Metas da apresentação são hipóteses, não resultados obtidos.

## Estrutura

- `backend/`: aplicação Spring Boot, API e núcleo de análise.
- `frontend/`: aplicação React e Vite.
- `demo/`: servidor JDK para execução offline e bibliotecas React legadas.
- `scripts/`: inicialização da demo.
- `tests/`: testes do núcleo.
- `compose.yaml`: execução com Docker.

## GitHub

Crie um repositório vazio `youtrace-ai`, depois, na raiz do projeto:
```sh
git init
git add .
git commit -m "feat: implement YouTrace AI MVP"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/youtrace-ai.git
git push -u origin main
```
O repositório não foi criado ou publicado automaticamente. Revise o código, execute o projeto e faça seus próprios commits antes de enviar o link.
