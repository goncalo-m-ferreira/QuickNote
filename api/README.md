# QuickNote API

API REST para a aplicação móvel QuickNote, responsável pela gestão de utilizadores, autenticação, notas e fotografias associadas.

## Tecnologias

- Node.js
- Express
- PostgreSQL
- pg
- bcryptjs
- jsonwebtoken
- cors
- dotenv
- Multer

## Configuração

Criar o ficheiro `api/.env` com as seguintes variáveis de ambiente:

```env
PORT=3000
NODE_ENV=development
DATABASE_URL=postgres://postgres:postgres@localhost:5432/quicknote
JWT_SECRET=chave_secreta_jwt
JWT_EXPIRES_IN=7d
```

## Executar localmente

1. Entrar na pasta da API:
   ```bash
   cd api
   ```

2. Instalar as dependências:
   ```bash
   npm install
   ```

3. Iniciar o servidor em modo de desenvolvimento:
   ```bash
   npm run dev
   ```

4. Iniciar em modo de produção:
   ```bash
   npm start
   ```

## Endpoints

| Método | Endpoint | Autenticação | Descrição |
| --- | --- | --- | --- |
| GET | /health | Não | Estado da API |
| POST | /auth/register | Não | Registar novo utilizador |
| POST | /auth/login | Não | Autenticar utilizador e obter token JWT |
| POST | /auth/logout | Sim | Terminar sessão do utilizador |
| GET | /users/me | Sim | Obter dados do utilizador autenticado |
| PATCH | /users/me | Sim | Atualizar o nome de apresentação do utilizador |
| GET | /notes | Sim | Listar as notas do utilizador autenticado |
| GET | /notes/:id | Sim | Obter uma nota específica do utilizador |
| POST | /notes | Sim | Criar uma nova nota |
| PUT | /notes/:id | Sim | Atualizar uma nota existente |
| DELETE | /notes/:id | Sim | Eliminar uma nota |
| PUT | /notes/:id/photo | Sim | Enviar ou atualizar a fotografia de uma nota |
| GET | /notes/:id/photo | Sim | Obter a fotografia de uma nota |
| DELETE | /notes/:id/photo | Sim | Eliminar a fotografia de uma nota |

## Autenticação

A autenticação é feita com JSON Web Tokens (JWT) enviados no cabeçalho `Authorization: Bearer <token>`.

- O acesso às notas é estritamente limitado ao respetivo proprietário.
- As palavras-passe são protegidas com hash utilizando bcryptjs antes de serem guardadas na base de dados.
- Todas as consultas ao PostgreSQL utilizam queries parametrizadas para prevenir injeção de SQL.

## Fotografias

A gestão de fotografias utiliza o `Multer` para processar pedidos `multipart/form-data`.

- O envio da fotografia deve ser feito através do campo `photo`.
- Formatos suportados: JPEG, PNG e WebP.
- Tamanho máximo permitido: 5 MB.
